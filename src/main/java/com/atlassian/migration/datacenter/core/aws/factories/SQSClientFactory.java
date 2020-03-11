package com.atlassian.migration.datacenter.core.aws.factories;

import com.atlassian.migration.datacenter.core.aws.auth.AtlassianPluginAWSCredentialsProvider;
import com.atlassian.migration.datacenter.core.aws.region.RegionService;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Component
public class SQSClientFactory implements FactoryBean<SqsClient> {

    private final AtlassianPluginAWSCredentialsProvider credentialsProvider;
    private final RegionService regionService;

    public SQSClientFactory(AtlassianPluginAWSCredentialsProvider credentialsProvider, RegionService regionService) {
        this.credentialsProvider = credentialsProvider;
        this.regionService = regionService;
    }

    @Override
    public SqsClient getObject() throws Exception {
        return SqsClient.builder()
                .region(Region.of(this.regionService.getRegion()))
                .credentialsProvider(this.credentialsProvider)
                .build();
    }

    @Override
    public Class<?> getObjectType() {
        return SqsClient.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
