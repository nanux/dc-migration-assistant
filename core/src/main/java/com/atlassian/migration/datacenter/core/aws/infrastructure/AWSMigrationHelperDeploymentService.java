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
import com.atlassian.migration.datacenter.spi.MigrationService;
import com.atlassian.migration.datacenter.spi.dto.MigrationContext;
import com.atlassian.migration.datacenter.spi.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.spi.infrastructure.InfrastructureDeploymentStatus;
import com.atlassian.migration.datacenter.spi.infrastructure.MigrationInfrastructureDeploymentService;

import java.util.Map;

/**
 * Manages the deployment of the migration helper stack which is used to hydrate the new
 * application deployment with data.
 */
public class AWSMigrationHelperDeploymentService extends CloudformationDeploymentService implements MigrationInfrastructureDeploymentService {

    private static final String MIGRATION_HELPER_TEMPLATE_URL = "https://trebuchet-aws-resources.s3.amazonaws.com/migration-helper.yml";

    private final MigrationService migrationService;

    public AWSMigrationHelperDeploymentService(CfnApi cfnApi, MigrationService migrationService) {
        super(cfnApi);
        this.migrationService = migrationService;
    }

    @Override
    public void deployMigrationInfrastructure(Map<String, String> params) throws InvalidMigrationStageError
    {
        String applicationDeploymentId = migrationService.getCurrentContext().getApplicationDeploymentId();
        String migrationStackDeploymentId = applicationDeploymentId + "-migration";
        super.deployCloudformationStack(MIGRATION_HELPER_TEMPLATE_URL, migrationStackDeploymentId, params);

        final MigrationContext context = migrationService.getCurrentContext();
        context.setHelperStackDeploymentId(migrationStackDeploymentId);
        context.save();
    }

    @Override
    protected void handleSuccessfulDeployment() {

    }

    @Override
    protected void handleFailedDeployment() {

    }

    @Override
    public InfrastructureDeploymentStatus getDeploymentStatus() {
        return super.getDeploymentStatus(migrationService.getCurrentContext().getHelperStackDeploymentId());
    }
}
