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

package com.atlassian.migration.datacenter.core.aws.db;

import com.atlassian.migration.datacenter.core.application.ApplicationConfiguration;
import com.atlassian.migration.datacenter.core.db.DatabaseExtractor;
import com.atlassian.migration.datacenter.core.db.DatabaseExtractorFactory;
import com.atlassian.migration.datacenter.core.exceptions.DatabaseMigrationFailure;
import com.atlassian.migration.datacenter.core.fs.*;
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFileSystemMigrationErrorReport;
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFileSystemMigrationReport;
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFilesystemMigrationProgress;
import com.atlassian.migration.datacenter.core.util.UploadQueue;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationErrorReport;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationProgress;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationReport;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class DatabaseMigrationService
{
    private final ApplicationConfiguration applicationConfiguration;
    private final Path tempDirectory;
    private final S3AsyncClient s3AsyncClient;

    private Process extractorProcess;
    private AtomicReference<MigrationStatus> status = new AtomicReference<>();


    //TODO: Move tempdirectory away from the constructor and pass that into the method instead
    public DatabaseMigrationService(ApplicationConfiguration applicationConfiguration,
                                    Path tempDirectory,
                                    S3AsyncClient s3AsyncClient)
    {
        this.applicationConfiguration = applicationConfiguration;
        this.tempDirectory = tempDirectory;
        this.s3AsyncClient = s3AsyncClient;
        this.setStatus(MigrationStatus.NOT_STARTED);
    }

    /**
     * Start database dump and upload to S3 bucket. This is a blocking operation and should be started from ExecutorService
     * or preferably from ScheduledJob. The status of the migration can be queried via getStatus().
     */
    public FileSystemMigrationErrorReport performMigration() throws DatabaseMigrationFailure
    {
        DatabaseExtractor extractor = DatabaseExtractorFactory.getExtractor(applicationConfiguration);
        Path target = tempDirectory.resolve("db.dump");

        extractorProcess = extractor.startDatabaseDump(target);
        setStatus(MigrationStatus.DUMP_IN_PROGRESS);
        try {
            extractorProcess.waitFor();
        } catch (Exception e) {
            String msg = "Error while waiting for DB extractor to finish";
            setStatus(MigrationStatus.error(msg, e));
            throw new DatabaseMigrationFailure(msg, e);
        }
        setStatus(MigrationStatus.DUMP_COMPLETE);


        FileSystemMigrationReport report = new DefaultFileSystemMigrationReport();

        String bucket = System.getProperty("S3_TARGET_BUCKET_NAME", "trebuchet-testing");
        S3UploadConfig config = new S3UploadConfig(bucket, this.s3AsyncClient, target.getParent());


        S3Uploader uploader = new S3Uploader(config, report);
        Crawler crawler = new DirectoryStreamCrawler(report);


        FilesystemUploader filesystemUploader = new FilesystemUploader(crawler, uploader);
        setStatus(MigrationStatus.UPLOAD_IN_PROGRESS);

        filesystemUploader.uploadDirectory(target);

        setStatus(MigrationStatus.UPLOAD_COMPLETE);

        setStatus(MigrationStatus.FINISHED);

        return report;
    }

    private void setStatus(MigrationStatus status) {
        this.status.set(status);
    }

    public MigrationStatus getStatus() {
        return status.get();
    }
}
