package com.atlassian.migration.datacenter.fs.processor.configuration;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsync;
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.context.config.annotation.EnableStackConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@EnableStackConfiguration(stackName = "migration-helper")
public class AWSServicesConfiguration {

    @Value("${app.aws.sqs.test.endpoint}")
    private String awsSQSTestEndpoint;

    @Value("${app.aws.s3.test.endpoint}")
    private String awsS3TestEndpoint;

    @Value("${app.s3.vpc.endpoints}")
    private List<String> s3Endpoints;

    @Value("${app.s3.vpc.endpoints}")
    private String region;

    @Value("${app.aws.cloudformation.test.endpoint}")
    private String awsCloudFormationTestEndpoint;

    @Bean
    @Primary
    @Profile("test")
    public AmazonSQSAsync awsSqsClientMock() {
        DefaultAWSCredentialsProviderChain credentialsProviderChain = new DefaultAWSCredentialsProviderChain();
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(credentialsProviderChain)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsSQSTestEndpoint, "eu-central-1"))
                .build();
    }

    @Bean
    @Primary
    @Profile("test")
    public AmazonS3 awsS3ClientMock() {
        DefaultAWSCredentialsProviderChain credentialsProviderChain = new DefaultAWSCredentialsProviderChain();
        return AmazonS3ClientBuilder.standard()
                .withCredentials(credentialsProviderChain)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsS3TestEndpoint, "eu-central-1"))
                .build();
    }

    @Bean
    @Primary
    @Profile("test")
    public AmazonCloudFormationAsync awsCloudFormationClientMock() {
        DefaultAWSCredentialsProviderChain credentialsProviderChain = new DefaultAWSCredentialsProviderChain();
        return AmazonCloudFormationAsyncClientBuilder.standard()
                .withCredentials(credentialsProviderChain)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsCloudFormationTestEndpoint, "eu-central-1"))
                .build();
    }

    @Bean
    @Primary
    @Profile("production")
    public AmazonS3 awsS3ClientProd() {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }


    @Bean
    @Primary
    @Profile("production")
    public AmazonSQSAsync awsSqsClientProd() {
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

    @Bean
    @Primary
    @Profile("production")
    public AmazonCloudFormationAsync awsCloudFormationClientProd() {
        return AmazonCloudFormationAsyncClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

}
