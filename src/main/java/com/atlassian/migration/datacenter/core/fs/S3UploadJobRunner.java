package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.spi.fs.FilesystemMigrationService;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationReport;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * Scheduled job starting directory upload to S3 bucket. If there is already job running, it will abort execution.
 * The job expects 2 parameters being passed when the job is scheduled
 * <ul>
 * <li>s3bucket: target S3 bucket we are uploading the files into</li>
 * <li>directory: directory to upload</li>
 * </ul>
 */
public class S3UploadJobRunner implements JobRunner {
    public static String KEY = "com.atlassian.migration.datacenter.fs.S3UploadJobRunner";
    private static Logger log = LoggerFactory.getLogger(S3UploadJobRunner.class);
    private final FilesystemMigrationService fsMigrationService;

    public S3UploadJobRunner(FilesystemMigrationService fsMigrationService) {
        this.fsMigrationService = fsMigrationService;
    }

    @Nullable
    @Override
    public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
        if (fsMigrationService.isRunning()) {
            return JobRunnerResponse.aborted("S3 upload job is still running");
        }

        log.info("Starting S3 migration job");
        try {
            fsMigrationService.startMigration();
        } catch (InvalidMigrationStageError e) {
            log.error("Invalid migration error - {}", e.getMessage());
            return JobRunnerResponse.failed("Encountered an invalid migration stage");
        }

        final FileSystemMigrationReport report = fsMigrationService.getReport();
        log.info("Finished S3 migration job: {}", report);

        return JobRunnerResponse.success("S3 upload completed.");
    }

}
