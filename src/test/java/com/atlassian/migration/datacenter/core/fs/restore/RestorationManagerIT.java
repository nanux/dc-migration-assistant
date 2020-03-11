package com.atlassian.migration.datacenter.core.fs.restore;

import cloud.localstack.docker.LocalstackDockerExtension;
import cloud.localstack.docker.annotation.LocalstackDockerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

import static org.mockito.Mockito.when;

@Tag("integration")
@ExtendWith({LocalstackDockerExtension.class, MockitoExtension.class})
@LocalstackDockerProperties(services = {"s3", "iam", "sqs"}, imageTag = "0.10.8")
public class RestorationManagerIT {

    private static final String LOCALSTACK_S3_ENDPOINT = "http://localhost:4572";
    private static final String LOCALSTACK_SQS_ENDPOINT = "http://localhost:4576";
    private static final String LOCALSTACK_IAM_ENDPOINT = "http://localhost:4593";
    private static final String TREBUCHET_LOCALSTACK_BUCKET = "trebuchet-localstack-bucket";

    @Mock
    private AwsCredentialsProvider mockCredentialsProvider;

    private S3Client localStackS3Client;
    private RestorationConfigurationManager restorationConfigurationManager;

    @BeforeEach
    void setup() {
        when(mockCredentialsProvider.resolveCredentials()).thenReturn(new AwsCredentials() {
            @Override
            public String accessKeyId() {
                return "fake-access-key";
            }

            @Override
            public String secretAccessKey() {
                return "fake-secret-key";
            }
        });

        localStackS3Client = S3Client.builder()
                .credentialsProvider(mockCredentialsProvider)
                .endpointOverride(URI.create(LOCALSTACK_S3_ENDPOINT))
                .region(Region.US_EAST_1)
                .build();

        IamClient localStackIAMClient = IamClient.builder()
                .credentialsProvider(mockCredentialsProvider)
                .endpointOverride(URI.create(LOCALSTACK_IAM_ENDPOINT))
                .region(Region.US_EAST_1)
                .build();

        SqsClient localStackSQSClient = SqsClient.builder()
                .credentialsProvider(mockCredentialsProvider)
                .endpointOverride(URI.create(LOCALSTACK_SQS_ENDPOINT))
                .region(Region.US_EAST_1)
                .build();

        this.restorationConfigurationManager = new RestorationConfigurationManager(localStackS3Client, localStackSQSClient, localStackIAMClient);
    }

    @Test
    public void testRestorationConfiguration() {
        this.localStackS3Client.createBucket(CreateBucketRequest.builder().bucket(TREBUCHET_LOCALSTACK_BUCKET).build());
        this.restorationConfigurationManager.configureS3Bucket(TREBUCHET_LOCALSTACK_BUCKET);
    }

}
