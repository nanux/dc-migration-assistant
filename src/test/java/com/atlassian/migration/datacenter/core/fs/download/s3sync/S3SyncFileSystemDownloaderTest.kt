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
package com.atlassian.migration.datacenter.core.fs.download.s3sync

import com.atlassian.migration.datacenter.core.aws.ssm.SSMApi
import com.atlassian.migration.datacenter.core.fs.download.s3sync.S3SyncFileSystemDownloader.CannotLaunchCommandException
import com.atlassian.migration.datacenter.core.fs.download.s3sync.S3SyncFileSystemDownloader.IndeterminateS3SyncStatusException
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import software.amazon.awssdk.services.ssm.model.CommandInvocationStatus
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse

@ExtendWith(MockitoExtension::class)
internal class S3SyncFileSystemDownloaderTest {
    @Mock
    var mockSsmApi: SSMApi? = null
    var sut: S3SyncFileSystemDownloader? = null

    @BeforeEach
    fun setUp() {
        sut = S3SyncFileSystemDownloader(mockSsmApi!!, 1)
    }

    @Test
    @Throws(CannotLaunchCommandException::class)
    fun shouldIssueCommandToInstance() {
        Mockito.`when`(mockSsmApi!!.getSSMCommand(ArgumentMatchers.any(), ArgumentMatchers.anyString())).thenReturn(
                GetCommandInvocationResponse.builder()
                        .status(CommandInvocationStatus.SUCCESS)
                        .build())
        sut!!.initiateFileSystemDownload()
        Mockito.verify(mockSsmApi)?.runSSMDocument(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyMap())
    }

    @Test
    fun shouldNotThrowWhenCommandIsIssuedAndSucceeds() {
        Mockito.`when`(mockSsmApi!!.getSSMCommand(ArgumentMatchers.any(), ArgumentMatchers.anyString())).thenReturn(
                GetCommandInvocationResponse.builder()
                        .status(CommandInvocationStatus.SUCCESS)
                        .build())
        try {
            sut!!.initiateFileSystemDownload()
        } catch (e: CannotLaunchCommandException) {
            Assertions.fail<Any>()
        }
    }

    @Test
    fun shouldThrowWhenCommandDoesNotSucceedWithinTimeout() {
        Mockito.`when`(mockSsmApi!!.getSSMCommand(ArgumentMatchers.any(), ArgumentMatchers.anyString())).thenReturn(
                GetCommandInvocationResponse.builder()
                        .status(CommandInvocationStatus.DELAYED)
                        .build())
        Assertions.assertThrows(CannotLaunchCommandException::class.java) { sut!!.initiateFileSystemDownload() }
    }

    @Test
    @Throws(IndeterminateS3SyncStatusException::class)
    fun shouldGetStatusOfSsmCommand() {
        givenSyncCommandIsRunning()
        givenStatusCommandCompletesSuccessfullyWithOutput(SYNC_STATUS_SUCCESS_COMPLETE_JSON)
        val status = whenStatusCommandIsInvoked()
        Assertions.assertTrue(status!!.isComplete())
        Assertions.assertEquals(status.getExitCode(), 0)
        Assertions.assertFalse(status.hasErrors())
    }

    @Test
    @Throws(IndeterminateS3SyncStatusException::class)
    fun shouldGetStatusWhenSyncIsPartiallyCompleteButNoLongerCalculating() {
        givenSyncCommandIsRunning()
        givenStatusCommandCompletesSuccessfullyWithOutput(SYNC_STATUS_DETERMINED_PARTIAL_JSON)
        val status = whenStatusCommandIsInvoked()
        Assertions.assertFalse(status!!.isCalculating)
        Assertions.assertFalse(status.hasErrors())
        Assertions.assertEquals(49492787.2, status.getBytesDownloaded())
        Assertions.assertEquals(451411968.0, status.getTotalBytesToDownload())
        Assertions.assertEquals(528, status.getFilesRemainingToDownload())
    }

    @Test
    @Throws(IndeterminateS3SyncStatusException::class)
    fun shouldGetStatusWhenSyncIsCompleteWithErrors() {
        givenSyncCommandIsRunning()
        givenStatusCommandCompletesSuccessfullyWithOutput(SYNC_STATUS_COMPLETE_ERROR_JSON)
        val status = whenStatusCommandIsInvoked()
        Assertions.assertTrue(status!!.hasErrors())
        Assertions.assertTrue(status.isComplete())
        Assertions.assertFalse(status.isCalculating)
        Assertions.assertEquals(1, status.getExitCode())
        Assertions.assertEquals(1, status.getErrors().size)
        MatcherAssert.assertThat(status.getErrors(), Matchers.hasItem("fatal error: Unable to locate credentials\n"))
    }

    @Test
    @Throws(IndeterminateS3SyncStatusException::class)
    fun shouldGetStatusWhenSyncIsPartiallyCompleteWithErrors() {
        givenSyncCommandIsRunning()
        givenStatusCommandCompletesSuccessfullyWithOutput(SYNC_STATUS_PARTIAL_CALCULATING_WITH_ERROR_JSON)
        val status = whenStatusCommandIsInvoked()
        Assertions.assertTrue(status!!.hasErrors())
        Assertions.assertTrue(status.isCalculating)
        Assertions.assertFalse(status.isComplete())
        Assertions.assertEquals(4724464025.6, status.getBytesDownloaded())
        Assertions.assertEquals(4724464025.6, status.getTotalBytesToDownload())
        Assertions.assertEquals(1004, status.getFilesRemainingToDownload())
        MatcherAssert.assertThat(status.getErrors(), Matchers.hasItem("Oh dang it broke\n"))
    }

    private fun givenSyncCommandIsRunning() {
        Mockito.`when`(mockSsmApi!!.runSSMDocument(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyMap())).thenReturn("status-command-invocation")
    }

    private fun givenStatusCommandCompletesSuccessfullyWithOutput(syncStatusDeterminedPartial: String) {
        val mockStatusResponse = GetCommandInvocationResponse.builder()
                .status(CommandInvocationStatus.SUCCESS)
                .standardOutputContent(syncStatusDeterminedPartial)
                .build()
        Mockito.`when`(mockSsmApi!!.getSSMCommand(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(mockStatusResponse)
    }

    @Throws(IndeterminateS3SyncStatusException::class)
    private fun whenStatusCommandIsInvoked(): S3SyncCommandStatus? {
        return sut!!.getFileSystemDownloadStatus()
    }

    companion object {
        private const val SYNC_STATUS_SUCCESS_COMPLETE_JSON = "{\"finished\": true, \"code\": \"0\", \"status\": {}}\n"
        private const val SYNC_STATUS_DETERMINED_PARTIAL_JSON = "{\"status\": {\"progress\": 49492787.2, \"files_remaining\": 528, \"total\": 451411968.0, \"isCalculating\": false}}\n"
        private const val SYNC_STATUS_COMPLETE_ERROR_JSON = "{\"finished\": true, \"code\": \"1\", \"status\": {}, \"errors\": [\"fatal error: Unable to locate credentials\\n\"]}\n"
        private const val SYNC_STATUS_PARTIAL_CALCULATING_WITH_ERROR_JSON = "{\"status\": {\"progress\": 4724464025.6, \"files_remaining\": 1004, \"total\": 4724464025.6, \"isCalculating\": true}, \"errors\": [\"Oh dang it broke\\n\"]}\n"
    }
}