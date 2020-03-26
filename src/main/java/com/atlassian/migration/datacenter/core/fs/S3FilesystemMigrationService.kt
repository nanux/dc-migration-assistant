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
import com.atlassian.migration.datacenter.core.exceptions.FileUploadException
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError
import com.atlassian.migration.datacenter.core.fs.download.s3sync.S3SyncFileSystemDownloadManager
import com.atlassian.migration.datacenter.core.fs.download.s3sync.S3SyncFileSystemDownloader.CannotLaunchCommandException
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFileSystemMigrationReport
import com.atlassian.migration.datacenter.spi.MigrationService
import com.atlassian.migration.datacenter.spi.MigrationStage
import com.atlassian.migration.datacenter.spi.fs.FilesystemMigrationService
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationReport
import com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus
import com.atlassian.scheduler.SchedulerService
import com.atlassian.scheduler.SchedulerServiceException
import com.atlassian.scheduler.config.JobConfig
import com.atlassian.scheduler.config.JobId
import com.atlassian.scheduler.config.JobRunnerKey
import com.atlassian.scheduler.config.RunMode
import com.atlassian.util.concurrent.Supplier
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.s3.S3AsyncClient
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.PostConstruct

class S3FilesystemMigrationService(
    private val s3AsyncClientSupplier: Supplier<S3AsyncClient>,
    private val jiraHome: JiraHome,
    private val fileSystemDownloadManager: S3SyncFileSystemDownloadManager,
    private val migrationService: MigrationService,
    private val schedulerService: SchedulerService
) : FilesystemMigrationService {
    private var s3AsyncClient: S3AsyncClient? = null
    private var report: FileSystemMigrationReport = DefaultFileSystemMigrationReport()
    private var fsUploader: FilesystemUploader? = null

    @PostConstruct
    fun postConstruct() {
        s3AsyncClient = s3AsyncClientSupplier.get()
    }

    override fun isRunning(): Boolean {
        return migrationService.currentStage == MigrationStage.WAIT_FS_MIGRATION_COPY
    }

    override fun getReport(): FileSystemMigrationReport {
        return report
    }

    @Throws(InvalidMigrationStageError::class)
    override fun scheduleMigration(): Boolean {
        val currentMigration = migrationService.currentMigration
        if (currentMigration.stage != MigrationStage.FS_MIGRATION_COPY) {
            throw InvalidMigrationStageError(
                "Cannot start filesystem migration as the system is not ready. " +
                    "Required state should be ${MigrationStage.FS_MIGRATION_COPY} but is ${currentMigration.stage}"
            )
        }
        val runnerKey = JobRunnerKey.of(S3UploadJobRunner.Companion.KEY)
        val jobId = getScheduledJobId()
        logger.info("Starting filesystem migration")
        if (schedulerService.getJobDetails(jobId) != null) {
            logger.warn("Tried to schedule file system migration while job already exists")
            return false
        }
        // TODO: Can the job runner be injected? It has no state
        schedulerService.registerJobRunner(runnerKey, S3UploadJobRunner(this))
        logger.info("Registered new job runner for S3")
        val jobConfig = JobConfig.forJobRunnerKey(runnerKey)
            .withSchedule(null) // run now
            .withRunMode(RunMode.RUN_ONCE_PER_CLUSTER)
        try {
            logger.info("Scheduling new job for S3 upload runner")
            schedulerService.scheduleJob(jobId, jobConfig)
        } catch (e: SchedulerServiceException) {
            logger.error("Exception when scheduling S3 upload job", e)
            schedulerService.unscheduleJob(jobId)
            migrationService.error()
            return false
        }
        return true
    }

    /**
     * Start filesystem migration to S3 bucket. This is a blocking operation and should be started from ExecutorService
     * or preferably from ScheduledJob
     */
    @Throws(InvalidMigrationStageError::class)
    override fun startMigration() {
        logger.trace(
            "Beginning migration. Uploading shared home dir {} to S3 bucket {}",
            getSharedHomeDir(),
            getS3Bucket()
        )
        if (isRunning()) {
            logger.warn("Filesystem migration is currently in progress, aborting new execution.")
            return
        }
        s3AsyncClient = s3AsyncClientSupplier.get()
        report = DefaultFileSystemMigrationReport()
        migrationService.transition(MigrationStage.FS_MIGRATION_COPY, MigrationStage.WAIT_FS_MIGRATION_COPY)
        report.status = FilesystemMigrationStatus.RUNNING
        val homeCrawler: Crawler = DirectoryStreamCrawler(report)
        val s3UploadConfig = S3UploadConfig(getS3Bucket(), s3AsyncClientSupplier.get(), getSharedHomeDir())
        val s3Uploader: Uploader = S3Uploader(s3UploadConfig, report)
        fsUploader = FilesystemUploader(homeCrawler, s3Uploader)
        logger.trace("commencing upload of shared home")
        try {
            fsUploader!!.uploadDirectory(getSharedHomeDir())
        } catch (e: FileUploadException) {
            logger.error("Caught exception during upload; check report for details.", e)
        }
        if (report.status != FilesystemMigrationStatus.FAILED) {
            logger.trace("upload of shared home complete. commencing shared home download")
            try {
                fileSystemDownloadManager.downloadFileSystem()
                report.status = FilesystemMigrationStatus.DONE
            } catch (e: CannotLaunchCommandException) {
                report.status = FilesystemMigrationStatus.FAILED
                logger.error("unable to launch s3 sync ssm command", e)
            }
        }
        if (report.status == FilesystemMigrationStatus.DONE) {
            logger.trace("Completed file system migration. Transitioning to next stage.")
            migrationService.transition(MigrationStage.WAIT_FS_MIGRATION_COPY, MigrationStage.OFFLINE_WARNING)
        } else if (report.status == FilesystemMigrationStatus.FAILED) {
            logger.error("Encountered error during file system migration. Transitioning to error state.")
            migrationService.error()
            report.status = FilesystemMigrationStatus.DONE
        }
    }

    @Throws(InvalidMigrationStageError::class)
    override fun abortMigration() { // we always try to remove scheduled job if the system is in inconsistent state
        schedulerService.getJobDetails(getScheduledJobId())?.let {
            schedulerService.unscheduleJob(getScheduledJobId())
            logger.info("Removed scheduled filesystem migration job")
        }
        if (!isRunning() || fsUploader == null) {
            throw InvalidMigrationStageError(
                String.format(
                    "Invalid migration stage when cancelling filesystem migration: %s",
                    migrationService.currentStage
                )
            )
        }
        logger.warn("Aborting running filesystem migration")
        fsUploader!!.abort()
        report.status = FilesystemMigrationStatus.FAILED
        migrationService.error()
    }

    private fun getS3Bucket(): String {
        return BUCKET_NAME
    }

    private fun getScheduledJobId(): JobId {
        return JobId.of(S3UploadJobRunner.KEY + migrationService.currentMigration.id)
    }

    private fun getSharedHomeDir(): Path {
        return if (OVERRIDE_UPLOAD_DIRECTORY != "") {
            Paths.get(OVERRIDE_UPLOAD_DIRECTORY)
        } else jiraHome.home.toPath()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(S3FilesystemMigrationService::class.java)
        private val OVERRIDE_UPLOAD_DIRECTORY =
            System.getProperty("com.atlassian.migration.datacenter.fs.overrideJiraHome", "")
        private val BUCKET_NAME = System.getProperty("S3_TARGET_BUCKET_NAME", "trebuchet-testing")
    }
}