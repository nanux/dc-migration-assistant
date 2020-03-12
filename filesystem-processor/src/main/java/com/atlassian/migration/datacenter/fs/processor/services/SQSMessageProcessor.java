package com.atlassian.migration.datacenter.fs.processor.services;

import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SQSMessageProcessor {

    @SqsListener("${app.aws.sqs.queue}")
    public void receiveMessage(S3EventNotification s3EventNotificationRecord) {
        List<S3EventNotificationRecord> s3EventNotificationRecords = s3EventNotificationRecord.getRecords();
        log.info("Received ".concat(Integer.toString(s3EventNotificationRecords.size())).concat(" records from S3."));
    }

}
