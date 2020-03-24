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
package com.atlassian.migration.datacenter.api

import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError
import com.atlassian.migration.datacenter.core.exceptions.MigrationAlreadyExistsException
import com.atlassian.migration.datacenter.dto.Migration
import com.atlassian.migration.datacenter.spi.MigrationService
import com.atlassian.migration.datacenter.spi.MigrationStage
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.ws.rs.core.Response

@ExtendWith(MockKExtension::class)
class MigrationEndpointTest {
    @MockK
    lateinit var migrationService: MigrationService
    @InjectMockKs
    lateinit var sut: MigrationEndpoint

    @BeforeEach
    fun init() = MockKAnnotations.init(this)

    @Test
    fun testOKAndMigrationStatusWhenMigrationExists() {
        every { migrationService.currentStage } returns MigrationStage.AUTHENTICATION
        val response = sut.getMigrationStatus()
        MatcherAssert.assertThat<String?>(response.entity.toString(), Matchers.containsString(MigrationStage.AUTHENTICATION.toString()))
    }

    @Test
    fun testNotFoundWhenMigrationDoesNotExist() {
        every { migrationService.currentStage } returns MigrationStage.NOT_STARTED
        val response = sut.getMigrationStatus()
        Assertions.assertEquals(Response.Status.NOT_FOUND.statusCode, response.status)
    }

    @Test
    fun testNoContentWhenCreatingMigrationAndNoneExists() {
        val stubMigration = mockk<Migration>()
        every { migrationService.createMigration() } returns stubMigration
        every { migrationService.transition(MigrationStage.NOT_STARTED, MigrationStage.AUTHENTICATION) } just runs
        val response = sut.createMigration()
        Assertions.assertEquals(Response.Status.NO_CONTENT.statusCode, response.status)
    }

    @Test
    @Throws(Exception::class)
    fun testBadRequestWhenCreatingMigrationAndOneExists() {
        val stubMigration = mockk<Migration>()
        every { migrationService.createMigration() } returns stubMigration
        every { migrationService.transition(MigrationStage.NOT_STARTED, MigrationStage.AUTHENTICATION) } throws InvalidMigrationStageError("")
        val response = sut.createMigration()
        Assertions.assertEquals(Response.Status.CONFLICT.statusCode, response.status)
        val entity = response.entity as MutableMap<*, *>
        Assertions.assertEquals("Unable to transition migration from initial state", entity["error"])
    }

    @Test
    fun testBadRequestWhenCreatingMigrationAndUnableToTransitionPastTheInitialStage() {
        every { (migrationService).createMigration() } throws MigrationAlreadyExistsException("")
        every { migrationService.transition(any(), any()) } just Runs
        val response = sut.createMigration()
        Assertions.assertEquals(Response.Status.CONFLICT.statusCode, response.status)
        val entity = response.entity as MutableMap<*, *>
        Assertions.assertEquals("migration already exists", entity["error"])
        verify(exactly = 0) { migrationService.transition(any(), any()) }
    }
}