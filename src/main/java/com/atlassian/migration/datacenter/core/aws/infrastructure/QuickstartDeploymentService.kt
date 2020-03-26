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
package com.atlassian.migration.datacenter.core.aws.infrastructure

import com.atlassian.activeobjects.external.ActiveObjects
import com.atlassian.migration.datacenter.core.aws.CfnApi
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError
import com.atlassian.migration.datacenter.dto.MigrationContext
import com.atlassian.migration.datacenter.spi.MigrationService
import com.atlassian.migration.datacenter.spi.MigrationStage
import com.atlassian.migration.datacenter.spi.infrastructure.ApplicationDeploymentService
import com.atlassian.migration.datacenter.spi.infrastructure.ApplicationDeploymentService.ApplicationDeploymentStatus
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.cloudformation.model.StackStatus

class QuickstartDeploymentService(
    private val ao: ActiveObjects,
    private val cfnApi: CfnApi,
    private val migrationService: MigrationService
) : ApplicationDeploymentService {
    private val logger = LoggerFactory.getLogger(QuickstartDeploymentService::class.java)

    /**
     * Commences the deployment of the AWS Quick Start. It will transition the state machine upon completion of the
     * deployment. If the deployment finishes successfully we transition to the next stage, otherwise we transition
     * to an error. The migration will also transition to an error if the deployment takes longer than an hour.
     *
     * @param deploymentId the stack name
     * @param params the parameters for the cloudformation template. The key should be the parameter name and the value
     * should be the parameter value.
     */
    @Throws(InvalidMigrationStageError::class)
    override fun deployApplication(deploymentId: String, params: Map<String, String>) {
        logger.info("received request to deploy application")
        migrationService.transition(MigrationStage.PROVISION_APPLICATION, MigrationStage.WAIT_PROVISION_APPLICATION)
        logger.info("deploying application stack")
        cfnApi.provisionStack(QUICKSTART_TEMPLATE_URL, deploymentId, params)
        addDeploymentIdToMigrationContext(deploymentId)
        scheduleMigrationServiceTransition(deploymentId)
    }

    override fun deploymentStatus(): ApplicationDeploymentStatus {
        val context = getMigrationContext()
        return when (cfnApi.getStatus(context.applicationDeploymentId)) {
            StackStatus.CREATE_COMPLETE -> ApplicationDeploymentStatus.CREATE_COMPLETE
            StackStatus.CREATE_FAILED -> ApplicationDeploymentStatus.CREATE_FAILED
            StackStatus.CREATE_IN_PROGRESS -> ApplicationDeploymentStatus.CREATE_IN_PROGRESS
            else -> {
                migrationService.error()
                throw RuntimeException("Unexpected stack status")
            }
        }
    }

    private fun addDeploymentIdToMigrationContext(deploymentId: String) {
        logger.info("Storing stack name in migration context")
        val context = getMigrationContext()
        context.applicationDeploymentId = deploymentId
        context.save()
    }

    private fun getMigrationContext(): MigrationContext {
        val migrationContexts = ao.find(MigrationContext::class.java)
        if (migrationContexts.isEmpty()) {
            migrationService.error()
            throw RuntimeException("No migration context exists, are you really in a migration?")
        }
        return migrationContexts[0]
    }

    private fun scheduleMigrationServiceTransition(deploymentId: String) {
        logger.info("scheduling transition of migration status when application stack deployment is completed")
        val stackCompleteFuture = CompletableFuture<String>()
        val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        val ticker = scheduledExecutorService.scheduleAtFixedRate({
            val status = cfnApi.getStatus(deploymentId)
            if (status == StackStatus.CREATE_COMPLETE) {
                try {
                    migrationService.transition(
                        MigrationStage.WAIT_PROVISION_APPLICATION,
                        MigrationStage.PROVISION_MIGRATION_STACK
                    )
                } catch (invalidMigrationStageError: InvalidMigrationStageError) {
                    logger.error(
                        "tried to transition migration from {} but got error: {}.",
                        MigrationStage.WAIT_PROVISION_APPLICATION,
                        invalidMigrationStageError.message
                    )
                }
                stackCompleteFuture.complete("")
            }
            if (status == StackStatus.CREATE_FAILED) {
                logger.error("application stack deployment failed")
                migrationService.error()
                stackCompleteFuture.complete("")
            }
        }, 0, 30, TimeUnit.SECONDS)
        val canceller = scheduledExecutorService.scheduleAtFixedRate({
            logger.error("timed out while waiting for application stack to deploy")
            migrationService.error()
            ticker.cancel(true)
        }, 1, 100, TimeUnit.HOURS)
        stackCompleteFuture.whenComplete { _: String?, _: Throwable? ->
            ticker.cancel(true)
            canceller.cancel(true)
        }
    }

    companion object {
        private const val QUICKSTART_TEMPLATE_URL =
            "https://aws-quickstart.s3.amazonaws.com/quickstart-atlassian-jira/templates/quickstart-jira-dc-with-vpc.template.yaml"
    }
}