package com.atlassian.migration.datacenter.fs.processor.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class SQSMessageProcessor {

    private final AmazonS3 s3Client;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final String jiraHome;

    public SQSMessageProcessor(AmazonS3 s3Client, ThreadPoolTaskExecutor threadPoolTaskExecutor, @Value("${app.jira.file.path}") String jiraHome) {
        this.s3Client = s3Client;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.jiraHome = jiraHome;
    }

    @SqsListener("${app.aws.sqs.queue}")
    public void receiveMessage(S3EventNotification s3EventNotificationRecord) {
        List<S3EventNotificationRecord> s3EventNotificationRecords = s3EventNotificationRecord.getRecords();
        log.info("Received ".concat(Integer.toString(s3EventNotificationRecords.size())).concat(" records from S3."));
        try (Stream<S3EventNotificationRecord> recordStream = s3EventNotificationRecords.stream()) {
            recordStream.map(item -> (Runnable) () -> {
                try {
                    S3Object s3object = this.s3Client.getObject(item.getS3().getBucket().getName(), item.getS3().getObject().getKey());
                    S3ObjectInputStream inputStream = s3object.getObjectContent();
                    String localPath = item.getS3().getObject().getKey().replace("$JIRA_HOME", this.jiraHome);
                    FileUtils.copyInputStreamToFile(inputStream, new File(localPath));
                } catch (Exception ex) {
                    log.error(ex.getLocalizedMessage());
                }
            }).forEach(this.threadPoolTaskExecutor::submit);
        }

    }

}
