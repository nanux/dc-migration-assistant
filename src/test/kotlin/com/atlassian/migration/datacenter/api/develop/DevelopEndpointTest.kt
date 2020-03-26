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
package com.atlassian.migration.datacenter.api.develop

import com.atlassian.migration.datacenter.spi.MigrationService
import com.atlassian.migration.datacenter.spi.MigrationStage
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.MockitoAnnotations

@ExtendWith(MockKExtension::class)
class DevelopEndpointTest {
    @MockK
    lateinit var migrationService: MigrationService

    @InjectMockKs
    lateinit var endpoint: DevelopEndpoint

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun shouldSetStageWithCorrectTargetStage() {
        val initialStage = MigrationStage.AUTHENTICATION
        every { migrationService.currentStage } returns initialStage
        every { migrationService.transition(any(), any()) } just Runs
        val objectMapper = ObjectMapper()
        val migrationStage: MigrationStage =
            objectMapper.readValue<MigrationStage>("\"FS_MIGRATION_COPY\"", MigrationStage::class.java)
        endpoint.setMigrationStage(migrationStage)
        verify { migrationService.transition(initialStage, MigrationStage.FS_MIGRATION_COPY) }
    }
}