package com.atlassian.migration.datacenter.fs.processor.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
public class S3ToFileWriter implements Runnable {

    private final AmazonS3 s3Client;
    private final S3EventNotification.S3Entity entity;
    private final String jiraHome;

    public S3ToFileWriter(AmazonS3 s3Client, S3EventNotification.S3Entity entity, String jiraHome) {
        this.s3Client = s3Client;
        this.entity = entity;
        this.jiraHome = jiraHome;
    }

    @SneakyThrows
    @Override
    public void run() {
        try {
            S3Object s3object = this.s3Client.getObject(this.entity.getBucket().getName(), this.entity.getObject().getKey());
            S3ObjectInputStream inputStream = s3object.getObjectContent();
            final Path localPath = Paths.get(this.jiraHome.concat("/").concat(this.entity.getObject().getKey()));
            AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(localPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            byte[] bytes = IOUtils.toByteArray(inputStream);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            fileChannel.write(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    log.debug("Wrote the file {}", localPath.toString());
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    log.error("Failed to write the file {}", localPath.toString());
                }
            });
        } catch (Exception ex) {
            log.error(ex.getLocalizedMessage());
        }
    }
}
