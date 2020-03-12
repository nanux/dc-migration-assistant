package com.atlassian.migration.datacenter.fs.processor;

import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.atlassian.migration.datacenter.fs.processor.services.SQSMessageProcessor;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest()
@ActiveProfiles("test")
class FilesystemProcessorApplicationTests {

    @Autowired
    private AmazonSQSAsync sqsClient;

    @Value("${app.aws.sqs.queue}")
    private String queueName;

    @Autowired
    private QueueMessagingTemplate queueMessagingTemplate;

    @Autowired
    private SQSMessageProcessor sqsMessageProcessor;

    private static List<S3EventNotification.S3EventNotificationRecord> createNotifications() {
        int size = RandomUtils.nextInt(10, 50);
        List<S3EventNotification.S3EventNotificationRecord> notificationRecords = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            S3EventNotification.S3EventNotificationRecord record = new S3EventNotification.S3EventNotificationRecord(null, null, null, null, null, null, null, null, null);
            notificationRecords.add(record);
        }
        return notificationRecords;
    }

    @BeforeEach
    public void setup() {
        ListQueuesResult listResult = this.sqsClient.listQueues(this.queueName);
        boolean queueExists = false;
        for (String url : listResult.getQueueUrls()) {
            if (url.contains(this.queueName)) {
                queueExists = true;
                break;
            }
        }
        if (!queueExists) {
            CreateQueueRequest createQueueRequest = new CreateQueueRequest().withQueueName(this.queueName);
            this.sqsClient.createQueue(createQueueRequest).getQueueUrl();
        }
    }

    @Test
    void testSendMessage() {
        try {
            S3EventNotification eventNotification = new S3EventNotification(new ArrayList<>());
            this.queueMessagingTemplate.send(this.queueName, MessageBuilder.withPayload(eventNotification).build());
        } catch (Exception ex) {
            fail(ex.getLocalizedMessage());
        }
    }

    @Test
    void testReceiveMessage() {
        try {
            S3EventNotification eventNotification = new S3EventNotification(createNotifications());
            this.sqsMessageProcessor.receiveMessage(eventNotification);
        } catch (Exception ex) {
            fail(ex.getLocalizedMessage());
        }
    }

}
