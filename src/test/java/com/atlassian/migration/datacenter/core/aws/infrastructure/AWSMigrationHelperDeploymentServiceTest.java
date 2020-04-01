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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AWSMigrationHelperDeploymentServiceTest {

    @Mock
    CfnApi mockCfn;

    @InjectMocks
    AWSMigrationHelperDeploymentService sut;

    @Test
    void shouldProvisionCloudformationStack() throws InvalidMigrationStageError {
        final String deploymentId = "test-deployment";
        sut.deployMigrationInfrastructure(deploymentId, Collections.emptyMap());

        verify(mockCfn).provisionStack("https://trebuchet-aws-resources.s3.amazonaws.com/migration-helper.yml", deploymentId, Collections.emptyMap());
    }

    @Test
    void shouldReturnInProgressWhileCloudformationDeploymentIsOngoing() {

    }

    @Test
    void shouldReturnCompleteWhenCloudformationDeploymentSucceeds() {

    }

    @Test
    void shouldReturnErrorWhenCloudformationDeploymentFails() {

    }
}