package com.atlassian.migration.datacenter.core.aws.cloud;

import com.atlassian.migration.datacenter.core.ModalMigrationStageWorker;
import com.atlassian.migration.datacenter.core.aws.auth.WriteCredentialsService;
import com.atlassian.migration.datacenter.core.aws.region.InvalidAWSRegionException;
import com.atlassian.migration.datacenter.core.aws.region.RegionService;
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.spi.MigrationServiceV2;
import com.atlassian.migration.datacenter.spi.MigrationStage;
import com.atlassian.migration.datacenter.spi.cloud.CloudProviderConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AWSConfigurationService implements CloudProviderConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(AWSConfigurationService.class);

    private final WriteCredentialsService writeCredentialsService;
    private final RegionService regionService;
    private final MigrationServiceV2 migrationService;
    private final ModalMigrationStageWorker worker;

    public AWSConfigurationService(WriteCredentialsService writeCredentialsService, RegionService regionService, MigrationServiceV2 migrationService, ModalMigrationStageWorker worker) {
        this.writeCredentialsService = writeCredentialsService;
        this.regionService = regionService;
        this.migrationService = migrationService;
        this.worker = worker;
    }

    /**
     * Configures the app to be able to authenticate with AWS.
     * @param entity the AWS access key ID
     * @param secret the AWS secret access key
     * @param geography the AWS region
     * @throws InvalidMigrationStageError when not in {@link MigrationStage#AUTHENTICATION}
     */
    @Override
    public void configureCloudProvider(String entity, String secret, String geography) throws InvalidMigrationStageError {
        worker.runAccordingToCurrentMode(() -> this.doConfigureAWSCreds(entity, secret, geography), MigrationStage.AUTHENTICATION, MigrationStage.PROVISION_APPLICATION);
    }

    private void doConfigureAWSCreds(String entity, String secret, String geography) throws InvalidMigrationStageError {
        try {
            regionService.storeRegion(geography);
            logger.info("stored aws region");
        } catch (InvalidAWSRegionException e) {
            logger.error("error storing AWS region", e);
            throw new RuntimeException(e);
        }

        logger.info("Storing AWS credentials");
        writeCredentialsService.storeAccessKeyId(entity);
        writeCredentialsService.storeSecretAccessKey(secret);

        migrationService.transition(MigrationStage.AUTHENTICATION, MigrationStage.PROVISION_APPLICATION);
    }
}
