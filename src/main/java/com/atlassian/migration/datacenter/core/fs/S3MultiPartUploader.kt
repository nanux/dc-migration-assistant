/*
 * Copyright 2020 Atlassian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atlassian.migration.datacenter.core.fs

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload
import software.amazon.awssdk.services.s3.model.CompletedPart
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.UploadPartRequest

/**
 * Uploads file to S3 in multiple parts.
 *
 *
 * 1. Initialise the upload
 * 2. Split the file into same sized parts (except the last one) and upload them to S3
 * 3. Confirm the upload has finished with all the required parts
 *
 *
 * All files larger than 5GB (hard AWS limit) are required to be uploaded via this method.
 *
 *
 * https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuoverview.html
 */
class S3MultiPartUploader(private val config: S3UploadConfig, private val file: File, private val key: String) {
    private var sizeToUpload = 100 * 1024 * 1024 // 100 MB
    private val completedParts: MutableList<CompletedPart> = ArrayList()
    private lateinit var buffer: ByteBuffer
    private var uploadPartNumber = 1

    @Throws(ExecutionException::class, InterruptedException::class)
    fun upload() { // lazily loaded to save memory
        buffer = ByteBuffer.allocate(getSizeToUpload())
        val uploadId = initiateUpload()
        try {
            FileInputStream(file).use { fileInputStream ->
                BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                    var readBytes: Int
                    while (bufferedInputStream.read(buffer.array()).also { readBytes = it } > 0) {
                        logger.trace("Read {} bytes from file {}", readBytes, file)
                        val etag = uploadChunk(uploadId, uploadPartNumber, readBytes)
                        logger.debug("Uploaded part {} with etag {}", uploadPartNumber, etag)
                        uploadPartNumber = completePart(uploadPartNumber, etag)
                    }
                }
            }
        } catch (e: IOException) {
            logger.error("Cannot open file for the multi-part upload", e)
        }
        buffer.clear()
        logger.trace("Finished uploading parts, sending complete request.")
        try {
            completeUpload(key, uploadId).get()
            logger.debug("Finished multipart upload for {} with {} parts", key, completedParts.size)
        } catch (e: InterruptedException) {
            logger.error("Encountered error when uploading multipart file.", e)
        } catch (e: ExecutionException) {
            logger.error("Encountered error when uploading multipart file.", e)
        }
    }

    /**
     * Size of each chunk into which the file is split into
     *
     * @return size of the chunk
     */
    fun getSizeToUpload(): Int {
        return sizeToUpload
    }

    /**
     * Changes the default chunk size for multipart upload
     *
     * @param sizeToUpload chunk size
     */
    fun setSizeToUpload(sizeToUpload: Int) {
        this.sizeToUpload = sizeToUpload
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    private fun initiateUpload(): String {
        val createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
            .bucket(config.getBucketName())
            .key(key)
            .build()
        val response = config.getS3AsyncClient().createMultipartUpload(createMultipartUploadRequest).get()
        return response.uploadId()
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    private fun uploadChunk(uploadId: String, uploadPartNumber: Int, readBytes: Int): String {
        val uploadPartRequest = UploadPartRequest.builder()
            .bucket(config.getBucketName())
            .key(key)
            .uploadId(uploadId)
            .partNumber(uploadPartNumber)
            .build()
        val body: AsyncRequestBody =
            when {
                readBytes < buffer.limit() -> { // We need to limit the buffer if the rest of the file is smaller than the allocated size.
                    // If don't do this, the size of the sent part will be always equal to the buffer size.
                    AsyncRequestBody.fromByteBuffer(buffer.limit(readBytes) as ByteBuffer)
                }
                else -> {
                    AsyncRequestBody.fromByteBuffer(buffer)
                }
            }
        return config.getS3AsyncClient()
            .uploadPart(uploadPartRequest, body)
            .get()
            .eTag()
    }

    private fun completePart(uploadPartNumber: Int, etag: String): Int {
        val part = CompletedPart.builder()
            .partNumber(uploadPartNumber)
            .eTag(etag)
            .build()
        completedParts.add(part)
        return uploadPartNumber + 1
    }

    private fun completeUpload(key: String, uploadId: String): CompletableFuture<CompleteMultipartUploadResponse> {
        val completedMultipartUpload = CompletedMultipartUpload.builder()
            .parts(completedParts)
            .build()
        val completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
            .bucket(config.getBucketName())
            .key(key)
            .uploadId(uploadId)
            .multipartUpload(completedMultipartUpload)
            .build()
        return config.getS3AsyncClient().completeMultipartUpload(completeMultipartUploadRequest)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(S3MultiPartUploader::class.java)
    }
}