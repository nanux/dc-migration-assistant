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
import com.atlassian.migration.datacenter.spi.infrastructure.InfrastructureDeploymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AWSMigrationHelperDeploymentServiceTest {

    static final String DEPLOYMENT_ID = "test-deployment";

    @Mock
    CfnApi mockCfn;

    @Mock
    MigrationService mockMigrationService;

    @InjectMocks
    AWSMigrationHelperDeploymentService sut;

    @Mock
    MigrationContext mockContext;

    @BeforeEach
    void setUp() {
        when(mockMigrationService.getCurrentContext()).thenReturn(mockContext);
    }

    @Test
    void shouldProvisionCloudformationStack() {
        givenMigrationStackHasStartedDeploying();

        verify(mockCfn).provisionStack("https://trebuchet-aws-resources.s3.amazonaws.com/migration-helper.yml", DEPLOYMENT_ID, Collections.emptyMap());
    }

    @Test
    void shouldReturnInProgressWhileCloudformationDeploymentIsOngoing() {
        givenMigrationStackHasStartedDeploying();
        givenMigrationStackDeploymentIsInProgress();
        givenMigrationStackNameHasBeenStoredInContext();

        assertEquals(InfrastructureDeploymentStatus.CREATE_IN_PROGRESS, sut.getDeploymentStatus());
    }

    @Test
    void shouldReturnCompleteWhenCloudformationDeploymentSucceeds() throws InterruptedException {
        givenMigrationStackHasStartedDeploying();
        givenMigrationStackDeploymentIsComplete();
        givenMigrationStackNameHasBeenStoredInContext();

        Thread.sleep(100);

        assertEquals(InfrastructureDeploymentStatus.CREATE_COMPLETE, sut.getDeploymentStatus());
    }

    @Test
    void shouldReturnErrorWhenCloudformationDeploymentFails() throws InterruptedException {
        givenMigrationStackHasStartedDeploying();
        givenMigrationStackDeploymentFailed();
        givenMigrationStackNameHasBeenStoredInContext();

        Thread.sleep(100);

        assertEquals(InfrastructureDeploymentStatus.CREATE_FAILED, sut.getDeploymentStatus());
    }

    private void givenMigrationStackHasStartedDeploying() {
        try {
            sut.deployMigrationInfrastructure(DEPLOYMENT_ID, Collections.emptyMap());
        } catch (InvalidMigrationStageError invalidMigrationStageError) {
            fail("invalid migration stage error thrown while deploying migration helper", invalidMigrationStageError);
        }
    }

    private void givenMigrationStackDeploymentIsInProgress() {
        when(mockCfn.getStatus(DEPLOYMENT_ID)).thenReturn(StackStatus.CREATE_IN_PROGRESS);
    }

    private void givenMigrationStackDeploymentIsComplete() {
        when(mockCfn.getStatus(DEPLOYMENT_ID)).thenReturn(StackStatus.CREATE_COMPLETE);
    }

    private void givenMigrationStackDeploymentFailed() {
        when(mockCfn.getStatus(DEPLOYMENT_ID)).thenReturn(StackStatus.CREATE_FAILED);
    }

    private void givenMigrationStackNameHasBeenStoredInContext() {
        when(mockContext.getHelperStackDeploymentId()).thenReturn(DEPLOYMENT_ID);
    }
}