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
package com.atlassian.migration.datacenter.core.aws

import com.atlassian.activeobjects.external.ActiveObjects
import com.atlassian.activeobjects.test.TestActiveObjects
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError
import com.atlassian.migration.datacenter.core.exceptions.MigrationAlreadyExistsException
import com.atlassian.migration.datacenter.dto.Migration
import com.atlassian.migration.datacenter.dto.MigrationContext
import com.atlassian.migration.datacenter.spi.MigrationStage
import com.atlassian.migration.datacenter.spi.fs.FilesystemMigrationService
import com.atlassian.scheduler.SchedulerService
import net.java.ao.EntityManager
import net.java.ao.test.junit.ActiveObjectsJUnitRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit

// We have to use the JUnit 4 API because there is no JUnit 5 active objects extension :(
@RunWith(ActiveObjectsJUnitRunner::class)
class AWSMigrationServiceTest {
    private var ao: ActiveObjects? = null
    private val entityManager: EntityManager? = null
    private var sut: AWSMigrationService? = null

    @Rule
    var mockitoRule = MockitoJUnit.rule()

    @Mock
    private val cfnApi: CfnApi? = null

    @Mock
    private val filesystemMigrationService: FilesystemMigrationService? = null

    @Mock
    private val schedulerService: SchedulerService? = null

    @Before
    fun setup() {
        Assertions.assertNotNull(entityManager)
        ao = TestActiveObjects(entityManager)
        sut = AWSMigrationService(ao as TestActiveObjects)
    }

    @Test
    fun shouldBeInNotStartedStageWhenNoMigrationsExist() {
        setupEntities()
        val initialStage = sut!!.currentStage
        Assertions.assertEquals(MigrationStage.NOT_STARTED, initialStage)
    }

    @Test
    fun shouldBeAbleToGetCurrentStage() {
        initializeAndCreateSingleMigrationWithStage(MigrationStage.AUTHENTICATION)
        Assertions.assertEquals(MigrationStage.AUTHENTICATION, sut!!.currentStage)
    }

    @Test
    @Throws(InvalidMigrationStageError::class)
    fun shouldTransitionWhenSourceStageIsCurrentStage() {
        initializeAndCreateSingleMigrationWithStage(MigrationStage.AUTHENTICATION)
        Assertions.assertEquals(MigrationStage.AUTHENTICATION, sut!!.currentStage)
        sut!!.transition(MigrationStage.AUTHENTICATION, MigrationStage.PROVISION_APPLICATION)
        Assertions.assertEquals(MigrationStage.PROVISION_APPLICATION, sut!!.currentStage)
    }

    @Test
    fun shouldNotTransitionWhenSourceStageIsNotCurrentStage() {
        initializeAndCreateSingleMigrationWithStage(MigrationStage.AUTHENTICATION)
        Assertions.assertEquals(MigrationStage.AUTHENTICATION, sut!!.currentStage)
        Assertions.assertThrows(InvalidMigrationStageError::class.java) {
            sut!!.transition(
                MigrationStage.FS_MIGRATION_COPY,
                MigrationStage.PROVISION_APPLICATION
            )
        }
        Assertions.assertEquals(sut!!.currentStage, MigrationStage.AUTHENTICATION)
    }

    @Test
    @Throws(MigrationAlreadyExistsException::class)
    fun shouldCreateMigrationInNotStarted() {
        ao!!.migrate(Migration::class.java)
        val migration = sut!!.createMigration()
        Assertions.assertEquals(MigrationStage.NOT_STARTED, migration.stage)
    }

    @Test
    fun shouldThrowExceptionWhenMigrationExistsAlready() {
        initializeAndCreateSingleMigrationWithStage(MigrationStage.AUTHENTICATION)
        Assertions.assertThrows(MigrationAlreadyExistsException::class.java) { sut!!.createMigration() }
    }

    @Test
    fun shouldSetCurrentStageToErrorOnError() {
        initializeAndCreateSingleMigrationWithStage(MigrationStage.PROVISION_APPLICATION)
        sut!!.error()
        Assertions.assertEquals(MigrationStage.ERROR, sut!!.currentStage)
    }

    @Test
    fun shouldRaiseErrorOnGetCurrentMigrationWhenMoreThanOneExists() {
        initializeAndCreateSingleMigrationWithStage(MigrationStage.WAIT_FS_MIGRATION_COPY)
        initializeAndCreateSingleMigrationWithStage(MigrationStage.ERROR)
        assertNumberOfMigrations(2)
        Assertions.assertThrows(
            Exception::class.java,
            { sut!!.currentMigration },
            "Invalid State - should only be 1 migration"
        )
    }

    @Test
    fun shouldGetCurrentMigrationWhenOneExists() {
        val existingMigration = initializeAndCreateSingleMigrationWithStage(MigrationStage.WAIT_FS_MIGRATION_COPY)
        val currentMigration = sut!!.currentMigration
        Assertions.assertEquals(currentMigration.id, existingMigration.id)
        Assertions.assertEquals(currentMigration.stage, existingMigration.stage)
    }

    @Test
    fun shouldCreateMigrationWhenNoneExists() {
        setupEntities()
        val migration = sut!!.currentMigration
        assertNumberOfMigrations(1)
        Assertions.assertEquals(MigrationStage.NOT_STARTED, migration.stage)
    }

    private fun assertNumberOfMigrations(i: Int) {
        Assertions.assertEquals(i, ao!!.find(Migration::class.java).size)
    }

    private fun initializeAndCreateSingleMigrationWithStage(stage: MigrationStage): Migration {
        setupEntities()
        val migration = ao!!.create(Migration::class.java)
        migration.stage = stage
        migration.save()
        return migration
    }

    private fun setupEntities() {
        ao!!.migrate(Migration::class.java)
        ao!!.migrate(MigrationContext::class.java)
    }
}