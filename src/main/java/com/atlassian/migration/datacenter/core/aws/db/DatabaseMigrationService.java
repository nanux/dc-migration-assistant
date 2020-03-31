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

import com.atlassian.migration.datacenter.core.aws.db.restore.DatabaseRestoreStageTransitionCallback;
import com.atlassian.migration.datacenter.core.aws.db.restore.SsmPsqlDatabaseRestoreService;
import com.atlassian.migration.datacenter.core.db.DatabaseMigrationJobRunner;
import com.atlassian.migration.datacenter.core.exceptions.DatabaseMigrationFailure;
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.core.fs.FilesystemUploader;
import com.atlassian.migration.datacenter.core.fs.S3UploadJobRunner;
import com.atlassian.migration.datacenter.core.util.MigrationJobRunner;
import com.atlassian.migration.datacenter.core.util.MigrationRunner;
import com.atlassian.migration.datacenter.spi.MigrationService;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationErrorReport;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.scheduler.config.JobId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.file.Path;

import static com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus.FAILED;

/**
 * Copyright Atlassian: 10/03/2020
 */
public class DatabaseMigrationService
{
    private static Logger logger = LoggerFactory.getLogger(DatabaseMigrationService.class);

    // FIXME: Should really be delegated to the state machine.
    public enum MigrationStatus {
        NOT_STARTED(false),
        EXTRACTING(true),
        UPLOADING(true),
        RESTORING(true),
        FAILED(false),
        FINISHED(false);

        private boolean running;

        MigrationStatus(boolean running) {
            this.running = running;
        }

        public boolean isRunning() {
            return running;
        }
    }

    private static final String TARGET_BUCKET_NAME = System.getProperty("S3_TARGET_BUCKET_NAME", "trebuchet-testing");

    private final Path tempDirectory;
    private final DatabaseArchivalService databaseArchivalService;
    private final DatabaseArchiveStageTransitionCallback stageTransitionCallback;
    private final DatabaseArtifactS3UploadService s3UploadService;
    private final DatabaseUploadStageTransitionCallback uploadStageTransitionCallback;
    private final SsmPsqlDatabaseRestoreService restoreService;
    private final DatabaseRestoreStageTransitionCallback restoreStageTransitionCallback;
    private final MigrationService migrationService;
    private final MigrationRunner migrationRunner;

    private MigrationStatus migrationStatus;

    public DatabaseMigrationService(Path tempDirectory,
                                    MigrationService migrationService,
                                    MigrationRunner migrationRunner,
                                    DatabaseArchivalService databaseArchivalService,
                                    DatabaseArchiveStageTransitionCallback stageTransitionCallback,
                                    DatabaseArtifactS3UploadService s3UploadService,
                                    DatabaseUploadStageTransitionCallback uploadStageTransitionCallback,
                                    SsmPsqlDatabaseRestoreService restoreService,
                                    DatabaseRestoreStageTransitionCallback restoreStageTransitionCallback)
    {
        this.tempDirectory = tempDirectory;
        this.databaseArchivalService = databaseArchivalService;
        this.stageTransitionCallback = stageTransitionCallback;
        this.s3UploadService = s3UploadService;
        this.uploadStageTransitionCallback = uploadStageTransitionCallback;
        this.restoreService = restoreService;
        this.restoreStageTransitionCallback = restoreStageTransitionCallback;
        this.migrationService = migrationService;
        this.migrationRunner = migrationRunner;
        migrationStatus = MigrationStatus.NOT_STARTED;
    }

    /**
     * Start database dump and upload to S3 bucket. This is a blocking operation and should be started from ExecutorService
     * or preferably from ScheduledJob. The status of the migration can be queried via getStatus().
     */
    public FileSystemMigrationErrorReport performMigration() throws DatabaseMigrationFailure, InvalidMigrationStageError {
        migrationStatus = MigrationStatus.EXTRACTING;

        Path pathToDatabaseFile = databaseArchivalService.archiveDatabase(tempDirectory, stageTransitionCallback);

        FileSystemMigrationErrorReport report;

        migrationStatus = MigrationStatus.UPLOADING;
        try {
            report = s3UploadService.upload(pathToDatabaseFile, TARGET_BUCKET_NAME, this.uploadStageTransitionCallback);
        } catch (FilesystemUploader.FileUploadException e) {
            migrationStatus = MigrationStatus.FAILED;
            throw new DatabaseMigrationFailure("Error when uploading database dump to S3", e);
        }

        migrationStatus = MigrationStatus.RESTORING;
        try {
            restoreService.restoreDatabase(restoreStageTransitionCallback);
        } catch (Exception e) {
            migrationStatus = MigrationStatus.FAILED;
            throw new DatabaseMigrationFailure("Error when restoring database", e);
        }

        migrationStatus = MigrationStatus.FINISHED;
        return report;
    }

    public Boolean scheduleMigration() throws InvalidMigrationStageError {

        JobId jobId = getScheduledJobId();
        DatabaseMigrationJobRunner jobRunner = new DatabaseMigrationJobRunner(this);

        boolean result = migrationRunner.runMigration(jobId, jobRunner);

        if (!result) {
            migrationService.error();
        }
        return result;
    }

    public void abortMigration() throws InvalidMigrationStageError {
        // We always try to remove scheduled job if the system is in inconsistent state
        migrationRunner.abortJobIfPresesnt(getScheduledJobId());

        if (!migrationStatus.isRunning() || s3UploadService == null) {
            throw new InvalidMigrationStageError(String.format("Invalid migration stage when cancelling filesystem migration: %s", migrationService.getCurrentStage()));
        }

        logger.warn("Aborting running filesystem migration");

        migrationStatus = MigrationStatus.FAILED;
        migrationService.error();
    }

    private JobId getScheduledJobId() {
        return JobId.of(DatabaseMigrationJobRunner.KEY + migrationService.getCurrentMigration().getID());
    }

    public MigrationStatus getStatus()
    {
        return migrationStatus;
    }
}

