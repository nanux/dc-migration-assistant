/*
 * Copyright 2020 Atlassian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atlassian.migration.datacenter.core.aws.infrastructure;

import com.atlassian.migration.datacenter.core.aws.CfnApi;
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.dto.MigrationContext;
import com.atlassian.migration.datacenter.spi.MigrationService;
import com.atlassian.migration.datacenter.spi.infrastructure.InfrastructureDeploymentError;
import com.atlassian.migration.datacenter.spi.infrastructure.InfrastructureDeploymentStatus;
import com.atlassian.migration.datacenter.spi.infrastructure.MigrationInfrastructureDeploymentService;
import software.amazon.awssdk.services.cloudformation.model.Stack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages the deployment of the migration helper stack which is used to hydrate the new
 * application deployment with data.
 */
public class AWSMigrationHelperDeploymentService extends CloudformationDeploymentService implements MigrationInfrastructureDeploymentService {

    private static final String MIGRATION_HELPER_TEMPLATE_URL = "https://trebuchet-aws-resources.s3.amazonaws.com/migration-helper.yml";

    private final MigrationService migrationService;
    private final CfnApi cfnApi;
    private String fsRestoreDocument;
    private String fsRestoreStatusDocument;
    private String rdsRestoreDocument;
    private String migrationStackASG;
    private String migrationBucket;


    public AWSMigrationHelperDeploymentService(CfnApi cfnApi, MigrationService migrationService) {
        super(cfnApi);
        this.migrationService = migrationService;
        this.cfnApi = cfnApi;
    }

    AWSMigrationHelperDeploymentService(CfnApi cfnApi, MigrationService migrationService, int pollIntervalSeconds) {
        super(cfnApi, pollIntervalSeconds);
        this.migrationService = migrationService;
        this.cfnApi = cfnApi;
    }

    @Override
    public void deployMigrationInfrastructure(Map<String, String> params) throws InvalidMigrationStageError {
        String applicationDeploymentId = migrationService.getCurrentContext().getApplicationDeploymentId();
        String migrationStackDeploymentId = applicationDeploymentId + "-migration";
        super.deployCloudformationStack(MIGRATION_HELPER_TEMPLATE_URL, migrationStackDeploymentId, params);

        final MigrationContext context = migrationService.getCurrentContext();
        context.setHelperStackDeploymentId(migrationStackDeploymentId);
        context.save();
    }

    @Override
    protected void handleSuccessfulDeployment() {
        Optional<Stack> maybeStack = cfnApi.getStack(migrationService.getCurrentContext().getHelperStackDeploymentId());
        if (!maybeStack.isPresent()) {
            throw new InfrastructureDeploymentError("stack was not found by DescribeStack even though it succeeded");
        }

        Stack stack = maybeStack.get();

        Map<String, String> outputsMap = new HashMap<>();
        stack.outputs().forEach(output -> {
            outputsMap.put(output.outputKey(), output.outputValue());
        });

        fsRestoreDocument = outputsMap.get("DownloadSSMDocument");
        fsRestoreStatusDocument = outputsMap.get("DownloadStatusSSMDocument");
        rdsRestoreDocument = outputsMap.get("RdsRestoreSSMDocument");
        migrationStackASG = outputsMap.get("ServerGroup");
        migrationBucket = outputsMap.get("MigrationBucket");
    }

    @Override
    protected void handleFailedDeployment() {

    }

    public String getFsRestoreDocument() {
        return fsRestoreDocument;
    }

    public String getFsRestoreStatusDocument() {
        return fsRestoreStatusDocument;
    }

    public String getDbRestoreDocument() {
        return rdsRestoreDocument;
    }

    public String getMigrationS3BucketName() {
        return migrationBucket;
    }

    public String getMigrationHostInstanceId() {
        return null;
    }

    @Override
    public InfrastructureDeploymentStatus getDeploymentStatus() {
        return super.getDeploymentStatus(migrationService.getCurrentContext().getHelperStackDeploymentId());
    }
}
