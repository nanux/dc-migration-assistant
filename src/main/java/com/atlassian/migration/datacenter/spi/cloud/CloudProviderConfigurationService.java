package com.atlassian.migration.datacenter.spi.cloud;

import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;

public interface CloudProviderConfigurationService {

    /**
     * Configures access to a cloud provider to facilitate the migration
     * @param entity an identifier for the entity accessing the cloud provider e.g. AWS access key ID
     * @param secret the secret to authenticate the entity to the cloud provider e.g. AWS secret access key
     * @param geography the geography to use for deployment of resources e.g. AWS region
     */
    void configureCloudProvider(String entity, String secret, String geography) throws InvalidMigrationStageError;

}
