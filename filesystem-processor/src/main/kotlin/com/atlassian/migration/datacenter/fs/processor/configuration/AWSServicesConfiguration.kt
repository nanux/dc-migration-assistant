package com.atlassian.migration.datacenter.fs.processor.configuration

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.cloudformation.AmazonCloudFormation
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2AsyncClient
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.atlassian.migration.datacenter.fs.processor.configuration.AWSServicesConfiguration.Companion.STACK_NAME
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient
import org.springframework.cloud.aws.context.config.annotation.EnableStackConfiguration
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean
import org.springframework.cloud.aws.core.region.Ec2MetadataRegionProvider
import org.springframework.cloud.aws.core.region.RegionProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("production", "test")
@EnableStackConfiguration(stackName = STACK_NAME)
open class AWSServicesConfiguration {

    @Bean
    open fun regionProvider(): RegionProvider? {
        return Ec2MetadataRegionProvider()
    }

    @Bean
    open fun credentialsProvider(): DefaultAWSCredentialsProviderChain? {
        return DefaultAWSCredentialsProviderChain()
    }

    @Bean
    @ConditionalOnMissingAmazonClient(AmazonEC2::class)
    open fun amazonEc2ClientProd(regionProvider: RegionProvider?, credentialsProvider: AWSCredentialsProvider?): AmazonWebserviceClientFactoryBean<AmazonEC2AsyncClient?>? {
        return AmazonWebserviceClientFactoryBean(AmazonEC2AsyncClient::class.java, credentialsProvider, regionProvider)
    }

    @Bean
    @ConditionalOnMissingAmazonClient(AmazonSQS::class)
    open fun awsSqsClientProd(regionProvider: RegionProvider?, credentialsProvider: AWSCredentialsProvider?): AmazonWebserviceClientFactoryBean<AmazonSQSAsyncClient?>? {
        return AmazonWebserviceClientFactoryBean(AmazonSQSAsyncClient::class.java, credentialsProvider, regionProvider)
    }

    @Bean
    @ConditionalOnMissingAmazonClient(AmazonCloudFormation::class)
    open fun awsCloudFormationClientProd(regionProvider: RegionProvider?, credentialsProvider: AWSCredentialsProvider?): AmazonWebserviceClientFactoryBean<AmazonCloudFormationAsyncClient?>? {
        return AmazonWebserviceClientFactoryBean(AmazonCloudFormationAsyncClient::class.java, credentialsProvider, regionProvider)
    }

    companion object {
        const val STACK_NAME = "migration-helper"
    }

}