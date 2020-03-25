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

import com.atlassian.migration.datacenter.spi.fs.reporting.*
import java.time.Clock
import java.time.Duration
import java.time.Instant

class DefaultFileSystemMigrationReport @JvmOverloads constructor(private val errorReport: FileSystemMigrationErrorReport = DefaultFileSystemMigrationErrorReport(), private val progress: FileSystemMigrationProgress = DefaultFilesystemMigrationProgress()) : FileSystemMigrationReport {
    private var clock: Clock = Clock.systemUTC()
    private var startTime: Instant? = null
    private var completeTime: Instant? = null

    override var status: FilesystemMigrationStatus = FilesystemMigrationStatus.NOT_STARTED
        set(value) {
            if (isStartingMigration(value)) {
                startTime = Instant.now(clock)
            } else if (isEndingMigration(value)) {
                completeTime = Instant.now(clock)
            }
            field = value
        }

    override val elapsedTime: Duration
        get() {
            var end = completeTime
            if (isRunning) {
                end = Instant.now(clock)
            }
            return Duration.between(startTime, end)
        }

    private val isRunning: Boolean = this.status == FilesystemMigrationStatus.RUNNING

    fun setClock(clock: Clock) {
        this.clock = clock
    }

    private fun isStartingMigration(toStatus: FilesystemMigrationStatus): Boolean {
        return status != FilesystemMigrationStatus.RUNNING && toStatus == FilesystemMigrationStatus.RUNNING
    }

    private fun isEndingMigration(toStatus: FilesystemMigrationStatus): Boolean {
        return status == FilesystemMigrationStatus.RUNNING && isTerminalState(toStatus)
    }

    private fun isTerminalState(toStatus: FilesystemMigrationStatus): Boolean {
        return toStatus == FilesystemMigrationStatus.DONE || toStatus == FilesystemMigrationStatus.FAILED
    }

    override fun toString(): String {
        return String.format("Filesystem migration report = { status: %s, migratedFiles: %d, erroredFiles: %d }",
                status,
                progress.getCountOfMigratedFiles(),
                errorReport.getFailedFiles().size
        )
    }

    /*
    DELEGATED METHODS FOLLOW
     */
    override fun getFailedFiles(): Set<FailedFileMigration> {
        return errorReport.getFailedFiles()
    }

    override fun reportFileNotMigrated(failedFileMigration: FailedFileMigration) {
        errorReport.reportFileNotMigrated(failedFileMigration)
    }

    override fun getNumberOfFilesFound(): Long {
        return progress.getNumberOfFilesFound()
    }

    override fun reportFileFound() {
        progress.reportFileFound()
    }

    override fun getNumberOfCommencedFileUploads(): Long {
        return progress.getNumberOfCommencedFileUploads()
    }

    override fun reportFileUploadCommenced() {
        progress.reportFileUploadCommenced()
    }

    override fun getCountOfMigratedFiles(): Long {
        return progress.getCountOfMigratedFiles()
    }

    override fun reportFileMigrated() {
        progress.reportFileMigrated()
    }
}