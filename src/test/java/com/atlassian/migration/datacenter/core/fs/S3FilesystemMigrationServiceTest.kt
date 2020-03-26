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
package com.atlassian.migration.datacenter.core.fs

import com.atlassian.jira.config.util.JiraHome
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError
import com.atlassian.migration.datacenter.core.fs.download.s3sync.S3SyncFileSystemDownloadManager
import com.atlassian.migration.datacenter.dto.Migration
import com.atlassian.migration.datacenter.spi.MigrationService
import com.atlassian.migration.datacenter.spi.MigrationStage
import com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus
import com.atlassian.scheduler.SchedulerService
import com.atlassian.scheduler.SchedulerServiceException
import com.atlassian.scheduler.config.JobConfig
import com.atlassian.scheduler.config.JobId
import com.atlassian.scheduler.config.JobRunnerKey
import com.atlassian.scheduler.config.RunMode
import com.atlassian.util.concurrent.Supplier
import java.nio.file.Paths
import java.util.UUID
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.internal.util.reflection.FieldSetter
import org.mockito.junit.jupiter.MockitoExtension
import software.amazon.awssdk.services.s3.S3AsyncClient

@ExtendWith(MockitoExtension::class)
internal class S3FilesystemMigrationServiceTest {
    @Mock
    lateinit var jiraHome: JiraHome

    @Mock
    lateinit var migrationService: MigrationService

    @Mock
    lateinit var schedulerService: SchedulerService

    @Mock
    lateinit var s3AsyncClientSupplier: Supplier<S3AsyncClient>

    @Mock
    lateinit var downloadManager: S3SyncFileSystemDownloadManager

    @InjectMocks
    lateinit var fsService: S3FilesystemMigrationService

    @Test
    @Throws(InvalidMigrationStageError::class)
    fun shouldFailToStartMigrationWhenSharedHomeDirectoryIsInvalid() {
        val nonexistentDir = Paths.get(UUID.randomUUID().toString())
        Mockito.`when`(migrationService.currentStage).thenReturn(MigrationStage.FS_MIGRATION_COPY)
        Mockito.`when`(jiraHome.home).thenReturn(nonexistentDir.toFile())
        fsService.startMigration()
        Mockito.verify(migrationService)
            .transition(MigrationStage.FS_MIGRATION_COPY, MigrationStage.WAIT_FS_MIGRATION_COPY)
        Mockito.verify(migrationService).error()
    }

    @Test
    @Throws(InvalidMigrationStageError::class)
    fun shouldFailToStartMigrationWhenMigrationAlreadyInProgress() {
        Mockito.`when`(migrationService.currentStage).thenReturn(MigrationStage.WAIT_FS_MIGRATION_COPY)
        Mockito.`when`(jiraHome.home).thenReturn(Paths.get("stub").toFile())
        fsService.startMigration()
        Assertions.assertEquals(fsService.getReport().status, FilesystemMigrationStatus.NOT_STARTED)
    }

    @Test
    fun shouldNotScheduleMigrationWhenCurrentMigrationStageIsNotFilesystemMigrationCopy() {
        val mockMigration = Mockito.mock(Migration::class.java)
        Mockito.`when`(migrationService.currentMigration).thenReturn(mockMigration)
        Mockito.`when`(mockMigration.stage).thenReturn(MigrationStage.NOT_STARTED)
        Assertions.assertThrows(InvalidMigrationStageError::class.java) { fsService.scheduleMigration() }
    }

    @Test
    @Throws(Exception::class)
    fun shouldScheduleMigrationWhenCurrentMigrationStageIsFsCopy() {
        createStubMigration(MigrationStage.FS_MIGRATION_COPY)
        val isScheduled = fsService.scheduleMigration()
        Assertions.assertEquals(true, isScheduled)
        Mockito.verify(schedulerService).registerJobRunner(ArgumentMatchers.argThat { x: JobRunnerKey ->
            x.compareTo(
                JobRunnerKey.of(S3UploadJobRunner.KEY)
            ) == 0
        }, ArgumentMatchers.any(S3UploadJobRunner::class.java))
        Mockito.verify(schedulerService).scheduleJob(
            ArgumentMatchers.argThat { jobId: JobId -> jobId.compareTo(JobId.of(S3UploadJobRunner.KEY + 42)) == 0 },
            ArgumentMatchers.argThat { jobConfig: JobConfig -> jobConfig.runMode == RunMode.RUN_ONCE_PER_CLUSTER }
        )
    }

    @Test
    @Throws(Exception::class)
    fun shouldUnsetScheduledJobWhenSchedulerExceptionIsRaised() {
        createStubMigration(MigrationStage.FS_MIGRATION_COPY)
        Mockito.doThrow(SchedulerServiceException::class.java)
            .`when`(schedulerService)
            .scheduleJob(ArgumentMatchers.any(), ArgumentMatchers.any())
        val isScheduled = fsService.scheduleMigration()
        Assertions.assertEquals(false, isScheduled)
        Mockito.verify(schedulerService)
            .unscheduleJob(ArgumentMatchers.argThat { jobId: JobId -> jobId.compareTo(JobId.of(S3UploadJobRunner.KEY + 42)) == 0 })
        Mockito.verify(migrationService).error()
    }

    @Test
    @Throws(Exception::class)
    fun shouldAbortRunningMigration() {
        mockJobDetailsAndMigration(MigrationStage.WAIT_FS_MIGRATION_COPY)
        val uploader = Mockito.mock(FilesystemUploader::class.java)
        FieldSetter.setField(fsService, fsService.javaClass.getDeclaredField("fsUploader"), uploader)
        fsService.abortMigration()
        Mockito.verify(uploader).abort()
        Mockito.verify(migrationService).error()
        Assertions.assertEquals(fsService.getReport().status, FilesystemMigrationStatus.FAILED)
    }

    @Test
    fun throwExceptionWhenTryToAbortNonRunningMigration() {
        mockJobDetailsAndMigration(MigrationStage.AUTHENTICATION)
        Assertions.assertThrows(InvalidMigrationStageError::class.java) { fsService.abortMigration() }
    }

    private fun createStubMigration(migrationStage: MigrationStage): Migration {
        val mockMigration = Mockito.mock(Migration::class.java)
        Mockito.`when`(migrationService.currentMigration).thenReturn(mockMigration)
        Mockito.`when`(mockMigration.stage).thenReturn(migrationStage)
        Mockito.`when`(mockMigration.id).thenReturn(42)
        return mockMigration
    }

    private fun mockJobDetailsAndMigration(migrationStage: MigrationStage) {
        val mockMigration = Mockito.mock(Migration::class.java)
        Mockito.`when`(migrationService.currentMigration).thenReturn(mockMigration)
        Mockito.`when`(mockMigration.id).thenReturn(2)
        Mockito.`when`(schedulerService.getJobDetails(ArgumentMatchers.any())).thenReturn(null)
        Mockito.`when`(migrationService.currentStage).thenReturn(migrationStage)
    }
}