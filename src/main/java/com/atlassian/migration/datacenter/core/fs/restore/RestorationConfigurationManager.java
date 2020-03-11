package com.atlassian.migration.datacenter.core.fs.restore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreatePolicyRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@Service
public class RestorationConfigurationManager implements RestorationConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(RestorationConfigurationManager.class);

    private final S3Client s3Client;
    private final SqsClient sqsClient;
    private final IamClient iamClient;

    public RestorationConfigurationManager(S3Client s3Client, SqsClient sqsClient, IamClient iamClient) {
        this.s3Client = s3Client;
        this.sqsClient = sqsClient;
        this.iamClient = iamClient;
    }

    public void configureS3Bucket(String bucketName) {
        GetBucketNotificationConfigurationRequest getBucketNotificationConfigurationRequest = GetBucketNotificationConfigurationRequest.builder()
                .bucket(bucketName)
                .build();
        GetBucketNotificationConfigurationResponse response = this.s3Client.getBucketNotificationConfiguration(getBucketNotificationConfigurationRequest);
        if (response != null && !response.hasQueueConfigurations()) {
            String queueARN = this.configureNotificationQueue();
            this.buildPolicy(queueARN, bucketName);
            QueueConfiguration queueConfiguration = QueueConfiguration.builder()
                    .queueArn(queueARN)
                    .build();
            NotificationConfiguration notificationConfiguration = NotificationConfiguration.builder()
                    .queueConfigurations(queueConfiguration)
                    .build();
            PutBucketNotificationConfigurationRequest putBucketNotificationConfigurationRequest = PutBucketNotificationConfigurationRequest.builder()
                    .bucket(bucketName)
                    .notificationConfiguration(notificationConfiguration)
                    .build();
            this.s3Client.putBucketNotificationConfiguration(putBucketNotificationConfigurationRequest);
        }
    }


    private String configureNotificationQueue() {
        final String queueName = "s3-uploads-to-efs";

        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder().queueName(queueName).build();

        CreateQueueResponse createQueueResponse = this.sqsClient.createQueue(createQueueRequest);

        GetQueueAttributesRequest queueAttributesRequest = GetQueueAttributesRequest.builder()
                .queueUrl(createQueueResponse.queueUrl())
                .attributeNamesWithStrings("All")
                .build();

        GetQueueAttributesResponse queueAttributesResponse = sqsClient.getQueueAttributes(queueAttributesRequest);
        Map<String, String> sqsAttributeMap = queueAttributesResponse.attributesAsStrings();

        return sqsAttributeMap.get("QueueArn");
    }

    private void buildPolicy(final String queueARN, final String bucketName) {
        try {
            final String policyDocument = processPolicyDocument(queueARN, bucketName);
            if (policyDocument != null) {
                CreatePolicyRequest request = CreatePolicyRequest.builder()
                        .policyDocument(policyDocument)
                        .policyName("s3-restore-notification-policy")
                        .description("a policy for the sending of bucket notifications to SQS")
                        .build();
                this.iamClient.createPolicy(request);
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
        }
    }

    private String processPolicyDocument(final String resourceARN, final String bucketName) throws IOException {
        String path = this.getClass().getClassLoader().getResource("s3NotificationPolicy.json").getFile();
        File file = ResourceUtils.getFile(path);
        if (file.exists()) {
            String policyContent = new String(Files.readAllBytes(file.toPath()));
            policyContent = policyContent.replace("{RESOURCE_ARN}", resourceARN);
            policyContent = policyContent.replace("{BUCKET_NAME}", bucketName);
            return policyContent;
        } else {
            return null;
        }
    }

}
