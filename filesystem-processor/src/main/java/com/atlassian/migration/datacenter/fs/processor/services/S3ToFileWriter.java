package com.atlassian.migration.datacenter.fs.processor.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
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
        String key = URLDecoder.decode(this.entity.getObject().getKey(), Charset.defaultCharset().toString());
        try (S3Object s3object = this.s3Client.getObject(this.entity.getBucket().getName(), key)) {
            S3ObjectInputStream inputStream = s3object.getObjectContent();
            final File localPath = new File(this.jiraHome.concat("/").concat(key));
            if (!localPath.getParentFile().exists()) {
                if (localPath.getParentFile().mkdirs()) {
                    log.debug("Made the directory {}", localPath.getPath());
                }
            }
            AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(localPath.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
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
            inputStream.close();
        } catch (Exception ex) {
            log.error("Failed to process ".concat(ex.getLocalizedMessage()));
            log.error(ex.getCause().getLocalizedMessage());
        }
    }
}
