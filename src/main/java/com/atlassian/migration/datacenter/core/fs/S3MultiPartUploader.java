package com.atlassian.migration.datacenter.core.fs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Uploads file to S3 in multiple parts.
 * <p>
 * 1. Initialise the upload
 * 2. Split the file into same sized parts (except the last one) and upload them to S3
 * 3. Confirm the upload has finished with all the required parts
 * <p>
 * All files larger than 5GB (hard AWS limit) are required to be uploaded via this method.
 * <p>
 * https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuoverview.html
 */
public class S3MultiPartUploader {
    private final static Logger logger = LoggerFactory.getLogger(S3MultiPartUploader.class);
    private final S3UploadConfig config;

    private int sizeToUpload = 50 * 1024 * 1024; // 50MB
    private List<CompletedPart> completedParts;

    public S3MultiPartUploader(S3UploadConfig config) {
        this.config = config;
    }

    /**
     * Upload the file in multiple parts
     *
     * @param file file to upload
     * @param key  S3 key for the file
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void multiPartUpload(File file, String key) throws ExecutionException, InterruptedException {
        completedParts = new ArrayList<>();
        final S3AsyncClient s3AsyncClient = config.getS3AsyncClient();

        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(config.getBucketName())
                .key(key)
                .build();
        CreateMultipartUploadResponse response = s3AsyncClient.createMultipartUpload(createMultipartUploadRequest).get();
        String uploadId = response.uploadId();

        final ByteBuffer buffer = ByteBuffer.allocate(getSizeToUpload());
        int uploadPartNumber = 1;

        try (FileInputStream fileInputStream = new FileInputStream(file);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

            while (bufferedInputStream.read(buffer.array()) > 0) {

                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(config.getBucketName())
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(uploadPartNumber)
                        .build();
                String etag = s3AsyncClient
                        .uploadPart(uploadPartRequest, AsyncRequestBody.fromByteBuffer(buffer))
                        .get()
                        .eTag();

                logger.debug("Uploaded part {} with etag {}", uploadPartNumber, etag);

                CompletedPart part = CompletedPart.builder()
                        .partNumber(uploadPartNumber)
                        .eTag(etag)
                        .build();
                completedParts.add(part);

                uploadPartNumber++;
            }
        } catch (IOException e) {
            logger.error("Cannot open file for the multi-part upload", e);
        }
        buffer.clear();

        completeUpload(key, uploadId).get();
        logger.debug("Finished multipart upload for {} with {} parts", key, completedParts.size());
    }

    /**
     * Size of each chunk into which the file is split into
     *
     * @return size of the chunk
     */
    public int getSizeToUpload() {
        return sizeToUpload;
    }

    /**
     * Changes the default chunk size for multipart upload
     *
     * @param sizeToUpload chunk size
     */
    public void setSizeToUpload(int sizeToUpload) {
        this.sizeToUpload = sizeToUpload;
    }

    private CompletableFuture<CompleteMultipartUploadResponse> completeUpload(String key, String uploadId) {
        CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build();
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                CompleteMultipartUploadRequest.builder()
                        .bucket(config.getBucketName())
                        .key(key)
                        .uploadId(uploadId)
                        .multipartUpload(completedMultipartUpload)
                        .build();
        return config.getS3AsyncClient().completeMultipartUpload(completeMultipartUploadRequest);
    }
}
