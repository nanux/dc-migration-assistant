package com.atlassian.migration.datacenter.core.aws.factories;

import com.atlassian.migration.datacenter.core.aws.auth.AtlassianPluginAWSCredentialsProvider;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;

@Component
public class IAMClientFactory implements FactoryBean<IamClient> {

    private final AtlassianPluginAWSCredentialsProvider credentialsProvider;

    public IAMClientFactory(AtlassianPluginAWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public IamClient getObject() throws Exception {
        return IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .credentialsProvider(this.credentialsProvider)
                .build();
    }

    @Override
    public Class<?> getObjectType() {
        return IamClient.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
