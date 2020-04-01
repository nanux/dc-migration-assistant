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
import com.atlassian.migration.datacenter.spi.MigrationStage;
import com.atlassian.migration.datacenter.spi.infrastructure.InfrastructureDeploymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class CloudformationDeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(CloudformationDeploymentService.class);

    private final CfnApi cfnApi;

    private String currentStack;

    protected CloudformationDeploymentService(CfnApi cfnApi) {
        this.cfnApi = cfnApi;
    }

    protected void deployCloudformationStack(String templateUrl, String stackName, Map<String, String> params) {
        currentStack = stackName;
        cfnApi.provisionStack(templateUrl, stackName, params);
        beginWatchingDeployment(stackName);
    }

    protected InfrastructureDeploymentStatus getDeploymentStatus() {
        StackStatus status = cfnApi.getStatus(currentStack);

        switch (status) {
            case CREATE_COMPLETE:
                return InfrastructureDeploymentStatus.CREATE_COMPLETE;
            case CREATE_FAILED:
                return InfrastructureDeploymentStatus.CREATE_FAILED;
            case CREATE_IN_PROGRESS:
                return InfrastructureDeploymentStatus.CREATE_IN_PROGRESS;
            default:
                throw new RuntimeException("Unexpected stack status");
        }
    }

    private void beginWatchingDeployment(String stackName) {
        CompletableFuture<String> stackCompleteFuture = new CompletableFuture<>();

        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> ticker = scheduledExecutorService.scheduleAtFixedRate(() -> {
            final StackStatus status = cfnApi.getStatus(stackName);
            if (status.equals(StackStatus.CREATE_COMPLETE)) {
                logger.info("stack {} creation succeeded", stackName);
                handleSuccessfulDeployment();
                stackCompleteFuture.complete("");
            }
            if (status.equals(StackStatus.CREATE_FAILED)) {
                logger.error("stack {} creation failed", stackName);
                handleFailedDeployment();
                stackCompleteFuture.complete("");
            }
        }, 0, 30, TimeUnit.SECONDS);

        ScheduledFuture<?> canceller = scheduledExecutorService.scheduleAtFixedRate(() -> {
            logger.error("timed out while waiting for stack {} to deploy", stackName);
            handleFailedDeployment();
            ticker.cancel(true);
            // Need to have non-zero period otherwise we get illegal argument exception
        }, 1, 100, TimeUnit.HOURS);

        stackCompleteFuture.whenComplete((result, thrown) -> {
            ticker.cancel(true);
            canceller.cancel(true);
        });
    }

    protected abstract void handleFailedDeployment();

    protected abstract void handleSuccessfulDeployment();
}
