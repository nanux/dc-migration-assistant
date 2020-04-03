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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AWSMigrationHelperDeploymentServiceTest {

    public static final String APPLICATION_DEPLOYMENT = "application-deployment";

    @Mock
    CfnApi mockCfn;

    @Mock
    MigrationService mockMigrationService;

    @InjectMocks
    AWSMigrationHelperDeploymentService sut;

    @Mock
    MigrationContext mockContext;
    private AtomicReference<String> deploymentId;


    @BeforeEach
    void setUp() {
        when(mockMigrationService.getCurrentContext()).thenReturn(mockContext);
        when(mockContext.getApplicationDeploymentId()).thenReturn(APPLICATION_DEPLOYMENT);

        deploymentId = new AtomicReference<>();
        doAnswer(invocation -> {
            deploymentId.set(invocation.getArgument(0));
            return null;
        }).when(mockContext).setHelperStackDeploymentId(anyString());

        lenient().when(mockContext.getHelperStackDeploymentId()).thenReturn(deploymentId.get());
    }

    @Test
    void shouldNameMigrationStackAfterApplicationStackWithSuffixAndStoreInContext() {
        givenMigrationStackHasStartedDeploying();

        assertEquals(getStackName(), deploymentId.get());
    }

    @Test
    void shouldProvisionCloudformationStack() {
        givenMigrationStackHasStartedDeploying();

        verify(mockCfn).provisionStack("https://trebuchet-aws-resources.s3.amazonaws.com/migration-helper.yml", getStackName(), Collections.emptyMap());
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
            sut.deployMigrationInfrastructure(Collections.emptyMap());
        } catch (InvalidMigrationStageError invalidMigrationStageError) {
            fail("invalid migration stage error thrown while deploying migration helper", invalidMigrationStageError);
        }
    }

    private void givenMigrationStackDeploymentIsInProgress() {
        when(mockCfn.getStatus(getStackName())).thenReturn(StackStatus.CREATE_IN_PROGRESS);
    }

    private void givenMigrationStackDeploymentIsComplete() {
        when(mockCfn.getStatus(getStackName())).thenReturn(StackStatus.CREATE_COMPLETE);
    }

    private void givenMigrationStackDeploymentFailed() {
        when(mockCfn.getStatus(getStackName())).thenReturn(StackStatus.CREATE_FAILED);
    }

    private void givenMigrationStackNameHasBeenStoredInContext() {
        when(mockContext.getHelperStackDeploymentId()).thenReturn(getStackName());
    }

    @NotNull
    private String getStackName() {
        return APPLICATION_DEPLOYMENT + "-migration";
    }
}