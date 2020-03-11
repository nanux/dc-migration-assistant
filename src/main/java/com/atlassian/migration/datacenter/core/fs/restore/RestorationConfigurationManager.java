package com.atlassian.migration.datacenter.core.fs.restore;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationResponse;
import software.amazon.awssdk.services.s3.model.NotificationConfiguration;
import software.amazon.awssdk.services.s3.model.QueueConfiguration;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

@Service
public class RestorationConfigurationManager implements RestorationConfigurationService {

    private final S3Client s3Client;
    private final SqsClient sqsClient;

    public RestorationConfigurationManager(S3Client s3Client, SqsClient sqsClient) {
        this.s3Client = s3Client;
        this.sqsClient = sqsClient;
    }

    public void configureS3Bucket(String bucketName) {
        GetBucketNotificationConfigurationRequest getBucketNotificationConfigurationRequest = GetBucketNotificationConfigurationRequest.builder()
                .bucket(bucketName)
                .build();
        GetBucketNotificationConfigurationResponse response = this.s3Client.getBucketNotificationConfiguration(getBucketNotificationConfigurationRequest);
        if (!response.hasQueueConfigurations()) {
            String queueARN =
                    QueueConfiguration queueConfiguration = QueueConfiguration.builder().queueArn()
            NotificationConfiguration notificationConfiguration = NotificationConfiguration.builder().
        }
    }

    private String configureNotificationQueue() {
        final String queueName = "s3-uploads-to-efs";
        this.sqsClient.createQueue(CreateQueueRequest.builder().queueName(queueName).build());
        Arn.builder().
    }

}
