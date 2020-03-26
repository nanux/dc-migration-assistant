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
package com.atlassian.migration.datacenter.core.aws.cloud

import com.atlassian.migration.datacenter.core.aws.auth.WriteCredentialsService
import com.atlassian.migration.datacenter.core.aws.region.InvalidAWSRegionException
import com.atlassian.migration.datacenter.core.aws.region.RegionService
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError
import com.atlassian.migration.datacenter.spi.MigrationService
import com.atlassian.migration.datacenter.spi.MigrationStage
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class AWSConfigurationServiceTest {
    @Mock
    var mockCredentialsWriter: WriteCredentialsService? = null

    @Mock
    var mockRegionService: RegionService? = null

    @Mock
    var mockMigrationService: MigrationService? = null

    @InjectMocks
    var sut: AWSConfigurationService? = null

    @Test
    @Throws(InvalidMigrationStageError::class)
    fun shouldStoreCredentials() {
        mockValidMigration()
        val username = "username"
        val password = "password"
        sut!!.configureCloudProvider(username, password, "garbage")
        Mockito.verify(mockCredentialsWriter)?.storeAccessKeyId(username)
        Mockito.verify(mockCredentialsWriter)?.storeSecretAccessKey(password)
    }

    @Test
    @Throws(InvalidAWSRegionException::class, InvalidMigrationStageError::class)
    fun shouldStoreRegion() {
        mockValidMigration()
        val region = "region"
        sut!!.configureCloudProvider("username", "password", region)
        Mockito.verify(mockRegionService)?.storeRegion(region)
    }

    @Test
    fun shouldStoreCredentialsOnlyWhenStateIsAuthentication() {
        Mockito.`when`(mockMigrationService!!.currentStage).thenReturn(MigrationStage.WAIT_FS_MIGRATION_COPY)
        Assertions.assertThrows(InvalidMigrationStageError::class.java) {
            sut!!.configureCloudProvider(
                "garbage",
                "garbage",
                "garbage"
            )
        }
    }

    private fun mockValidMigration() {
        Mockito.`when`(mockMigrationService!!.currentStage).thenReturn(MigrationStage.AUTHENTICATION)
    }

    @Test
    @Throws(InvalidMigrationStageError::class)
    fun shouldTransitionToProvisionApplicationStageWhenSuccessful() {
        mockValidMigration()
        sut!!.configureCloudProvider("garbage", "garbage", "garbage")
        Mockito.verify(mockMigrationService)
            ?.transition(MigrationStage.AUTHENTICATION, MigrationStage.PROVISION_APPLICATION)
    }

    @Test
    @Throws(InvalidAWSRegionException::class, InvalidMigrationStageError::class)
    fun shouldThrowErrorAndNotTransitionWhenUnableToCompleteSuccessfully() {
        mockValidMigration()
        val testRegion = "region"
        Mockito.doThrow(InvalidAWSRegionException()).`when`(mockRegionService)?.storeRegion(testRegion)
        try {
            sut!!.configureCloudProvider("garbage", "garbage", testRegion)
            Assertions.fail<Any>()
        } catch (rte: RuntimeException) {
            Assertions.assertEquals(InvalidAWSRegionException::class.java, rte.cause!!.javaClass)
            Mockito.verify(mockMigrationService, Mockito.never())
                ?.transition(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
    }
}