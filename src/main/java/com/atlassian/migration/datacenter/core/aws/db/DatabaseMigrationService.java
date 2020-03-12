package com.atlassian.migration.datacenter.core.aws.db;

import com.atlassian.migration.datacenter.core.application.ApplicationConfiguration;
import com.atlassian.migration.datacenter.core.aws.region.RegionService;
import com.atlassian.migration.datacenter.core.db.DatabaseExtractor;
import com.atlassian.migration.datacenter.core.db.DatabaseExtractorFactory;
import com.atlassian.migration.datacenter.core.exceptions.DatabaseMigrationFailure;
import com.atlassian.migration.datacenter.core.fs.S3Uploader;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Copyright Atlassian: 10/03/2020
 */
@Component
public class DatabaseMigrationService
{
    private final ApplicationConfiguration applicationConfiguration;
    private final AwsCredentialsProvider awsCredentialsProvider;
    private final RegionService regionService;
    private final Path tempDirectory;

    private ExecutorService dbMigrationThread;
    private Process extractorProcess;
    private AtomicReference<MigrationStatus> status = new AtomicReference();

    public DatabaseMigrationService(ApplicationConfiguration applicationConfiguration,
                                    AwsCredentialsProvider awsCredentialsProvider,
                                    RegionService regionService,
                                    Path tempDirectory)
    {
        this.applicationConfiguration = applicationConfiguration;
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.regionService = regionService;
        this.tempDirectory = tempDirectory;
        this.setStatus(MigrationStatus.NOT_STARTED);
    }

    /**
     * Start database dump and upload to S3 bucket. This is a blocking operation and should be started from ExecutorService
     * or preferably from ScheduledJob. The status of the migration can be queried via
     */
    public void performMigration() throws DatabaseMigrationFailure
    {
        dbMigrationThread = Executors.newSingleThreadExecutor();
        DatabaseExtractor extractor = DatabaseExtractorFactory.getExtractor(applicationConfiguration);
        File target;
        try {
            target = Files.createTempFile(tempDirectory, "dbdump", ".dump").toFile();
        } catch (IOException e) {
            throw new DatabaseMigrationFailure("Failed to create temporary database file under: "+tempDirectory);
        }

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


        setStatus(MigrationStatus.FINISHED);
    }

    private void setStatus(MigrationStatus status) {
        this.status.set(status);
    }

    public MigrationStatus getStatus() {
        return status.get();
    }
}
