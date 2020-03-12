package com.atlassian.migration.datacenter.fs.processor.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.File;

public class EFSWriter implements Runnable {

    private final AmazonS3 s3Client;
    private final S3EventNotification.S3Entity entity;
    private final String jiraHome;

    public EFSWriter(AmazonS3 s3Client, S3EventNotification.S3Entity entity, String jiraHome) {
        this.s3Client = s3Client;
        this.entity = entity;
        this.jiraHome = jiraHome;
    }

    @SneakyThrows
    @Override
    public void run() {
        S3Object s3object = this.s3Client.getObject(this.entity.getBucket().getName(), this.entity.getObject().getKey());
        S3ObjectInputStream inputStream = s3object.getObjectContent();
        String localPath = this.entity.getObject().getKey().replace("$JIRA_HOME", this.jiraHome);
        FileUtils.copyInputStreamToFile(inputStream, new File(localPath));
    }
}
