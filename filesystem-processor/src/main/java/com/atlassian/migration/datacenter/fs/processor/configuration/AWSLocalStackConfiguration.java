package com.atlassian.migration.datacenter.fs.processor.configuration;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsync;
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Async;
import com.amazonaws.services.ec2.AmazonEC2AsyncClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("localStack")
public class AWSLocalStackConfiguration {

    @Value("${app.aws.sqs.localStack.endpoint}")
    private String awsSQSLocalStackEndpoint;

    @Value("${app.aws.s3.localStack.endpoint}")
    private String awsS3LocalStackEndpoint;

    @Value("${app.aws.ec2.localStack.endpoint}")
    private String awsEC2LocalStackEndpoint;

    @Value("${app.aws.cloudformation.localStack.endpoint}")
    private String awsCloudFormationLocalStackEndpoint;

    @Bean
    @Primary
    @ConditionalOnMissingAmazonClient(AmazonEC2.class)
    public AmazonEC2Async awsEc2ClientLocalStack(@Value("${app.region.id}") String region) {
        DefaultAWSCredentialsProviderChain credentialsProviderChain = new DefaultAWSCredentialsProviderChain();
        return AmazonEC2AsyncClientBuilder.standard()
                .withCredentials(credentialsProviderChain)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(this.awsEC2LocalStackEndpoint, region))
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnMissingAmazonClient(AmazonSQS.class)
    public AmazonSQSAsync awsSqsClientLocalStack(@Value("${app.region.id}") String region) {
        DefaultAWSCredentialsProviderChain credentialsProviderChain = new DefaultAWSCredentialsProviderChain();
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(credentialsProviderChain)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(this.awsSQSLocalStackEndpoint, region))
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnMissingAmazonClient(AmazonCloudFormation.class)
    public AmazonCloudFormationAsync amazonCloudFormation(@Value("${app.region.id}") String region) {
        DefaultAWSCredentialsProviderChain credentialsProviderChain = new DefaultAWSCredentialsProviderChain();
        return AmazonCloudFormationAsyncClientBuilder.standard()
                .withCredentials(credentialsProviderChain)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(this.awsCloudFormationLocalStackEndpoint, region))
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnMissingAmazonClient(AmazonS3.class)
    public AmazonS3 awsS3ClientLocalStack(@Value("${app.region.id}") String region) {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(this.awsS3LocalStackEndpoint, region))
                .withPathStyleAccessEnabled(true)
                .build();
    }

}
