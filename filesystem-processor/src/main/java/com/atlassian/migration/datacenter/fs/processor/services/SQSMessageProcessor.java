package com.atlassian.migration.datacenter.fs.processor.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Slf4j
@Component
public class SQSMessageProcessor {

    private static final String QUEUE_LOGICAL_NAME = "MigrationQueue";

    private final AmazonS3 s3Client;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final String jiraHome;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SQSMessageProcessor(AmazonS3 s3Client, ThreadPoolTaskExecutor threadPoolTaskExecutor, @Value("${app.jira.file.path}") String jiraHome) {
        this.s3Client = s3Client;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.jiraHome = jiraHome;
    }


    @SqsListener(QUEUE_LOGICAL_NAME)
    public void receiveMessage(S3EventNotification s3EventNotificationRecord) {
        log.debug("Received SQS message {}", s3EventNotificationRecord.toJson());
        List<S3EventNotificationRecord> s3EventNotificationRecords = s3EventNotificationRecord.getRecords();
        log.debug("Received ".concat(Integer.toString(s3EventNotificationRecords.size())).concat(" records from S3."));
        File jiraHomePath = new File(this.jiraHome);
        if (!jiraHomePath.exists()) {
            if (jiraHomePath.mkdir()) {
                log.debug("Created Jira Home path ".concat(jiraHomePath.getAbsolutePath()));
            }
        }
        if (s3EventNotificationRecords.size() == 1) {
            submitTask(this.s3Client, s3EventNotificationRecords.get(0).getS3(), this.jiraHome);
        } else if (s3EventNotificationRecords.size() > 1) {
            s3EventNotificationRecords.forEach(record -> submitTask(this.s3Client, record.getS3(), this.jiraHome));
        }
    }

    private void submitTask(AmazonS3 s3Client, S3EventNotification.S3Entity item, String jiraHome) {
        S3ToFileWriter fileWriter = new S3ToFileWriter(s3Client, item, jiraHome);
        this.threadPoolTaskExecutor.submit(fileWriter);
    }

}
