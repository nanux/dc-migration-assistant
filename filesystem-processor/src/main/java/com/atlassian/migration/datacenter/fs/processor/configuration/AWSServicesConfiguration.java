package com.atlassian.migration.datacenter.fs.processor.configuration;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2AsyncClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.context.config.annotation.EnableStackConfiguration;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.region.Ec2MetadataRegionProvider;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"production", "test"})
@EnableStackConfiguration(stackName = "migration-helper")
public class AWSServicesConfiguration {

    @Bean
    public RegionProvider regionProvider() {
        return new Ec2MetadataRegionProvider();
    }

    @Bean
    public DefaultAWSCredentialsProviderChain credentialsProvider() {
        return new DefaultAWSCredentialsProviderChain();
    }

    @Bean
    @ConditionalOnMissingAmazonClient(AmazonEC2.class)
    public AmazonWebserviceClientFactoryBean<AmazonEC2AsyncClient> amazonEc2ClientProd(RegionProvider regionProvider, AWSCredentialsProvider credentialsProvider) {
        return new AmazonWebserviceClientFactoryBean<>(AmazonEC2AsyncClient.class, credentialsProvider, regionProvider);
    }

    @Bean
    @ConditionalOnMissingAmazonClient(AmazonSQS.class)
    public AmazonWebserviceClientFactoryBean<AmazonSQSAsyncClient> awsSqsClientProd(RegionProvider regionProvider, AWSCredentialsProvider credentialsProvider) {
        return new AmazonWebserviceClientFactoryBean<>(AmazonSQSAsyncClient.class, credentialsProvider, regionProvider);
    }

    @Bean
    @ConditionalOnMissingAmazonClient(AmazonCloudFormation.class)
    public AmazonWebserviceClientFactoryBean<AmazonCloudFormationAsyncClient> awsCloudFormationClientProd(RegionProvider regionProvider, AWSCredentialsProvider credentialsProvider) {
        return new AmazonWebserviceClientFactoryBean<>(AmazonCloudFormationAsyncClient.class, credentialsProvider, regionProvider);
    }

}
