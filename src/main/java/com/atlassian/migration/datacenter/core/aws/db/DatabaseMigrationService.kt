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
package com.atlassian.migration.datacenter.core.aws.db

import com.atlassian.migration.datacenter.core.application.ApplicationConfiguration
import com.atlassian.migration.datacenter.core.aws.db.MigrationStatus
import com.atlassian.migration.datacenter.core.db.DatabaseExtractorFactory
import com.atlassian.migration.datacenter.core.exceptions.DatabaseMigrationFailure
import com.atlassian.migration.datacenter.core.fs.*
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFileSystemMigrationReport
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationErrorReport
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationReport
import com.atlassian.util.concurrent.Supplier
import software.amazon.awssdk.services.s3.S3AsyncClient
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PostConstruct

class DatabaseMigrationService(private val applicationConfiguration: ApplicationConfiguration,
                               private val tempDirectory: Path,
                               private val s3AsyncClientSupplier: Supplier<S3AsyncClient>) {
    private lateinit var s3AsyncClient: S3AsyncClient
    private var extractorProcess: Process? = null
    private val status = AtomicReference<MigrationStatus>()

    @PostConstruct
    fun postConstruct() {
        s3AsyncClient = s3AsyncClientSupplier.get()
    }

    /**
     * Start database dump and upload to S3 bucket. This is a blocking operation and should be started from ExecutorService
     * or preferably from ScheduledJob. The status of the migration can be queried via getStatus().
     */
    @Throws(DatabaseMigrationFailure::class)
    fun performMigration(): FileSystemMigrationErrorReport {
        s3AsyncClient = s3AsyncClientSupplier.get()
        val extractor = DatabaseExtractorFactory.getExtractor(applicationConfiguration)
        val target = tempDirectory.resolve("db.dump")
        extractorProcess = extractor.startDatabaseDump(target)
        setStatus(MigrationStatus.Companion.DUMP_IN_PROGRESS)
        try {
            extractorProcess!!.waitFor()
        } catch (e: Exception) {
            val msg = "Error while waiting for DB extractor to finish"
            setStatus(MigrationStatus.Companion.error(msg, e))
            throw DatabaseMigrationFailure(msg, e)
        }
        setStatus(MigrationStatus.Companion.DUMP_COMPLETE)
        val report: FileSystemMigrationReport = DefaultFileSystemMigrationReport()
        val bucket = System.getProperty("S3_TARGET_BUCKET_NAME", "trebuchet-testing")
        val config = S3UploadConfig(bucket, s3AsyncClient, target.parent)
        val uploader = S3Uploader(config, report)
        val crawler: Crawler = DirectoryStreamCrawler(report)
        val filesystemUploader = FilesystemUploader(crawler, uploader)
        setStatus(MigrationStatus.Companion.UPLOAD_IN_PROGRESS)
        filesystemUploader.uploadDirectory(target)
        setStatus(MigrationStatus.Companion.UPLOAD_COMPLETE)
        setStatus(MigrationStatus.Companion.FINISHED)
        return report
    }

    private fun setStatus(status: MigrationStatus) {
        this.status.set(status)
    }

    fun getStatus(): MigrationStatus {
        return status.get()
    }

    init {
        setStatus(MigrationStatus.Companion.NOT_STARTED)
    }
}