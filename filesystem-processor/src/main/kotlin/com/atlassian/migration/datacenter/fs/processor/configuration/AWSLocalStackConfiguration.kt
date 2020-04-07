package com.atlassian.migration.datacenter.fs.processor.configuration

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.cloudformation.AmazonCloudFormation
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsync
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClientBuilder
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2Async
import com.amazonaws.services.ec2.AmazonEC2AsyncClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("localStack")
open class AWSLocalStackConfiguration {

    @Value("\${app.aws.sqs.localStack.endpoint}")
    lateinit var awsSQSLocalStackEndpoint: String

    @Value("\${app.aws.s3.localStack.endpoint}")
    lateinit var awsS3LocalStackEndpoint: String

    @Value("\${app.aws.ec2.localStack.endpoint}")
    lateinit var awsEC2LocalStackEndpoint: String

    @Value("\${app.aws.cloudformation.localStack.endpoint}")
    lateinit var awsCloudFormationLocalStackEndpoint: String

    @Bean
    @Primary
    @ConditionalOnMissingAmazonClient(AmazonEC2::class)
    open fun awsEc2ClientLocalStack(@Value("\${app.region.id}") region: String?): AmazonEC2Async? {
        val credentialsProviderChain = DefaultAWSCredentialsProviderChain()
        return AmazonEC2AsyncClientBuilder.standard()
                .withCredentials(credentialsProviderChain)
                .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(awsEC2LocalStackEndpoint, region))
                .build()
    }

    @Bean
    @Primary
    @ConditionalOnMissingAmazonClient(AmazonSQS::class)
    open fun awsSqsClientLocalStack(@Value("\${app.region.id}") region: String?): AmazonSQSAsync? {
        val credentialsProviderChain = DefaultAWSCredentialsProviderChain()
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(credentialsProviderChain)
                .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(awsSQSLocalStackEndpoint, region))
                .build()
    }

    @Bean
    @Primary
    @ConditionalOnMissingAmazonClient(AmazonCloudFormation::class)
    open fun amazonCloudFormation(@Value("\${app.region.id}") region: String?): AmazonCloudFormationAsync? {
        val credentialsProviderChain = DefaultAWSCredentialsProviderChain()
        return AmazonCloudFormationAsyncClientBuilder.standard()
                .withCredentials(credentialsProviderChain)
                .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(awsCloudFormationLocalStackEndpoint, region))
                .build()
    }

    @Bean
    @Primary
    @ConditionalOnMissingAmazonClient(AmazonS3::class)
    open fun awsS3ClientLocalStack(@Value("\${app.region.id}") region: String?): AmazonS3? {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain())
                .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(awsS3LocalStackEndpoint, region))
                .withPathStyleAccessEnabled(true)
                .build()
    }

}