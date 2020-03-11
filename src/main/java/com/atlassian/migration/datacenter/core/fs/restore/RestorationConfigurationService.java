package com.atlassian.migration.datacenter.core.fs.restore;

public interface RestorationConfigurationService {

    void configureS3Bucket(String bucketName);

}
