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
package com.atlassian.migration.datacenter.core.fs.reporting

import com.atlassian.migration.datacenter.spi.fs.reporting.FailedFileMigration
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationErrorReport
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationProgress
import com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.file.Paths
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@ExtendWith(MockitoExtension::class)
class DefaultFileSystemMigrationReportTest {
    private var sut: DefaultFileSystemMigrationReport? = null

    @Spy
    var progress: FileSystemMigrationProgress? = null

    @Spy
    var errors: FileSystemMigrationErrorReport? = null

    @BeforeEach
    fun setUp() {
        sut = DefaultFileSystemMigrationReport(errors!!, progress!!)
    }

    @Test
    fun testStatusInitiallyNotStarted() {
        Assertions.assertEquals(FilesystemMigrationStatus.NOT_STARTED, sut!!.status)
    }

    @Test
    fun testSetsStatus() {
        sut!!.status = FilesystemMigrationStatus.FAILED
        Assertions.assertEquals(FilesystemMigrationStatus.FAILED, sut!!.status)
    }

    @Test
    fun shouldDelegatesToWrappedErrorReport() {
        val testFile = Paths.get("file")
        val testReason = "test"
        val failedFileMigration = FailedFileMigration(testFile, testReason)
        sut!!.reportFileNotMigrated(failedFileMigration)
        Mockito.verify(errors)?.reportFileNotMigrated(failedFileMigration)
        sut!!.getFailedFiles()
        Mockito.verify(errors)?.getFailedFiles()
    }

    @Test
    fun shouldDelegateToWrappedProgress() {
        val path = Paths.get("file")
        sut!!.reportFileMigrated()
        Mockito.verify(progress)?.reportFileMigrated()
        sut!!.getCountOfMigratedFiles()
        Mockito.verify(progress)?.getCountOfMigratedFiles()
    }

    @Test
    fun shouldGiveDurationBetweenStartedAndGetElapsedTime() {
        val testClock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault())
        sut!!.setClock(testClock)
        sut!!.status = FilesystemMigrationStatus.RUNNING
        Assertions.assertEquals(Duration.ZERO, sut!!.elapsedTime)
        sut!!.setClock(Clock.offset(testClock, Duration.ofDays(1).plusSeconds(5)))
        Assertions.assertEquals(1L, sut!!.elapsedTime.toDays())
    }

    @Test
    fun shouldNotRestartTimerWhenTransitioningFromRunningToRunning() {
        val testClock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault())
        sut!!.setClock(testClock)
        sut!!.status = FilesystemMigrationStatus.RUNNING
        sut!!.setClock(Clock.offset(testClock, Duration.ofSeconds(10)))
        Assertions.assertEquals(10L, sut!!.elapsedTime.seconds)
        sut!!.status = FilesystemMigrationStatus.RUNNING
        Assertions.assertEquals(10L, sut!!.elapsedTime.seconds)
        sut!!.setClock(Clock.offset(testClock, Duration.ofSeconds(20)))
        Assertions.assertEquals(20L, sut!!.elapsedTime.seconds)
    }

    @ParameterizedTest
    @EnumSource(value = FilesystemMigrationStatus::class, names = ["DONE", "FAILED"])
    fun shouldNotIncrementElapsedTimeAfterMigrationEnds(status: FilesystemMigrationStatus?) {
        val testClock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault())
        sut!!.setClock(testClock)
        sut!!.status = FilesystemMigrationStatus.RUNNING
        sut!!.setClock(Clock.offset(testClock, Duration.ofSeconds(10)))
        Assertions.assertEquals(10L, sut!!.elapsedTime.seconds)
        sut!!.status = status!!
        sut!!.setClock(Clock.offset(testClock, Duration.ofSeconds(20)))
        Assertions.assertEquals(10L, sut!!.elapsedTime.seconds)
    }

    @Test
    fun testToString() {
        val successfullyMigrated = 888L
        val failedFiles = 666
        Mockito.`when`(progress!!.getCountOfMigratedFiles()).thenReturn(successfullyMigrated)
        val errorList = setOf<FailedFileMigration>()
        Mockito.`when`(errorList.size).thenReturn(failedFiles)
        Mockito.`when`(errors!!.getFailedFiles()).thenReturn(errorList)
        sut!!.status = FilesystemMigrationStatus.DONE
        val text = sut.toString()
        Assert.assertThat(text, Matchers.containsString("DONE"))
        Assert.assertThat(text, Matchers.containsString(successfullyMigrated.toString()))
        Assert.assertThat(text, Matchers.containsString(failedFiles.toString()))
    }
}