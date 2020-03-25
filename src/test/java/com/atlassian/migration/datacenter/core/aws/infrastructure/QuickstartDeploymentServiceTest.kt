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
import com.atlassian.migration.datacenter.spi.infrastructure.ApplicationDeploymentService.ApplicationDeploymentStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import software.amazon.awssdk.services.cloudformation.model.StackStatus
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class QuickstartDeploymentServiceTest {
    @Mock
    lateinit var mockCfnApi: CfnApi

    @Mock
    lateinit var mockMigrationService: MigrationService

    @Mock
    lateinit var mockAo: ActiveObjects

    @InjectMocks
    lateinit var deploymentService: QuickstartDeploymentService

    @Mock
    lateinit var mockContext: MigrationContext

    @Test
    @Throws(InvalidMigrationStageError::class)
    fun shouldDeployQuickStart() {
        initialiseValidMigration()
        deploySimpleStack()
        Mockito.verify(mockCfnApi).provisionStack("https://aws-quickstart.s3.amazonaws.com/quickstart-atlassian-jira/templates/quickstart-jira-dc-with-vpc.template.yaml", STACK_NAME, STACK_PARAMS)
    }

    @Test
    @Throws(InvalidMigrationStageError::class)
    fun shouldReturnInProgressWhileDeploying() {
        initialiseValidMigration()
        Mockito.`when`(mockContext.applicationDeploymentId).thenReturn(STACK_NAME)
        Mockito.`when`(mockCfnApi.getStatus(STACK_NAME)).thenReturn(StackStatus.CREATE_IN_PROGRESS)
        deploySimpleStack()
        Assertions.assertEquals(ApplicationDeploymentStatus.CREATE_IN_PROGRESS, deploymentService.deploymentStatus())
    }

    @Test
    @Throws(InvalidMigrationStageError::class, InterruptedException::class)
    fun shouldTransitionToWaitingForDeploymentWhileDeploymentIsCompleting() {
        initialiseValidMigration()
        Mockito.`when`(mockCfnApi.getStatus(STACK_NAME)).thenReturn(StackStatus.CREATE_IN_PROGRESS)
        deploySimpleStack()
        Thread.sleep(100)
        Mockito.verify(mockMigrationService).transition(MigrationStage.PROVISION_APPLICATION, MigrationStage.WAIT_PROVISION_APPLICATION)
    }

    @Test
    @Throws(InterruptedException::class, InvalidMigrationStageError::class)
    fun shouldTransitionMigrationServiceStateWhenDeploymentFinishes() {
        initialiseValidMigration()
        Mockito.`when`(mockCfnApi.getStatus(STACK_NAME)).thenReturn(StackStatus.CREATE_COMPLETE)
        deploySimpleStack()
        Thread.sleep(100)
        Mockito.verify(mockMigrationService).transition(MigrationStage.WAIT_PROVISION_APPLICATION, MigrationStage.PROVISION_MIGRATION_STACK)
    }

    @Test
    @Throws(InterruptedException::class, InvalidMigrationStageError::class)
    fun shouldTransitionMigrationServiceToErrorWhenDeploymentFails() {
        initialiseValidMigration()
        Mockito.`when`(mockCfnApi.getStatus(STACK_NAME)).thenReturn(StackStatus.CREATE_FAILED)
        deploySimpleStack()
        Thread.sleep(100)
        Mockito.verify(mockMigrationService).error()
    }

    @Test
    @Throws(InvalidMigrationStageError::class)
    fun shouldNotInitiateDeploymentIfNotInProvisionApplicationStage() {
        Mockito.doThrow(InvalidMigrationStageError("")).`when`(mockMigrationService).transition(ArgumentMatchers.argThat { argument: MigrationStage -> argument == MigrationStage.PROVISION_APPLICATION }, ArgumentMatchers.any(MigrationStage::class.java))
        Assertions.assertThrows(InvalidMigrationStageError::class.java) { deploySimpleStack() }
    }

    @Throws(InvalidMigrationStageError::class)
    private fun deploySimpleStack() {
        deploymentService.deployApplication(STACK_NAME, STACK_PARAMS)
    }

    private fun initialiseValidMigration() {
        Mockito.`when`(mockAo.find(MigrationContext::class.java)).thenReturn(arrayOf(mockContext))
    }

    companion object {
        const val STACK_NAME = "my-stack"
        val STACK_PARAMS: HashMap<String, String> = object : HashMap<String, String>() {
            init {
                put("parameter", "value")
            }
        }
    }
}