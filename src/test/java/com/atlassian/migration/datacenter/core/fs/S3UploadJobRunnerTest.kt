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

import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFileSystemMigrationReport
import com.atlassian.migration.datacenter.spi.fs.FilesystemMigrationService
import com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus
import com.atlassian.scheduler.JobRunnerRequest
import com.atlassian.scheduler.status.RunOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class S3UploadJobRunnerTest {
    @Mock
    lateinit var filesystemMigrationService: FilesystemMigrationService

    @Mock
    lateinit var jobRunnerRequest: JobRunnerRequest

    @InjectMocks
    lateinit var s3UploadJobRunner: S3UploadJobRunner

    @Test
    fun shouldNotRunJobWhenJobIsAlreadyRunning() {
        Mockito.`when`(filesystemMigrationService.isRunning()).thenReturn(true)
        val jobRunnerResponse = s3UploadJobRunner.runJob(jobRunnerRequest)
        Assertions.assertEquals(RunOutcome.ABORTED, jobRunnerResponse!!.runOutcome)
    }

    @Test
    @Throws(Exception::class)
    fun shouldNotRunJobWhenMigrationStageIsInvalid() {
        Mockito.`when`(filesystemMigrationService.isRunning()).thenReturn(false)
        Mockito.doThrow(InvalidMigrationStageError::class.java).`when`(filesystemMigrationService).startMigration()
        val jobRunnerResponse = s3UploadJobRunner.runJob(jobRunnerRequest)
        Assertions.assertEquals(RunOutcome.FAILED, jobRunnerResponse!!.runOutcome)
    }

    @Test
    @Throws(Exception::class)
    fun shouldCompleteJobRunSuccessfully() {
        val report = DefaultFileSystemMigrationReport()
        report.status = FilesystemMigrationStatus.DONE
        Mockito.`when`(filesystemMigrationService.isRunning()).thenReturn(false)
        Mockito.`when`(filesystemMigrationService.getReport()).thenReturn(report)
        Mockito.doNothing().`when`(filesystemMigrationService).startMigration()
        val jobRunnerResponse = s3UploadJobRunner.runJob(jobRunnerRequest)
        Assertions.assertEquals(RunOutcome.SUCCESS, jobRunnerResponse!!.runOutcome)
    }
}