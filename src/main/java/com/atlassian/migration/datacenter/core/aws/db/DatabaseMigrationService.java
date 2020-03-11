package com.atlassian.migration.datacenter.core.aws.db;

import com.atlassian.migration.datacenter.core.application.ApplicationConfiguration;
import com.atlassian.migration.datacenter.core.aws.region.RegionService;
import com.atlassian.migration.datacenter.core.db.DatabaseExtractor;
import com.atlassian.migration.datacenter.core.db.DatabaseExtractorFactory;
import com.atlassian.migration.datacenter.core.exceptions.DatabaseMigrationFailure;
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
    }

    public File startMigration() throws DatabaseMigrationFailure
    {
        dbMigrationThread = Executors.newSingleThreadExecutor();
        DatabaseExtractor extractor = DatabaseExtractorFactory.getExtractor(applicationConfiguration);
        File target;
        try {
            target = Files.createTempFile(tempDirectory, "dbdump", ".sql.gz").toFile();
        } catch (IOException e) {
            throw new DatabaseMigrationFailure("Failed to create temporary database file under: "+tempDirectory);
        }

        extractorProcess = extractor.startDatabaseDump(target);

        status.set(MigrationStatus.STARTED);

        dbMigrationThread.submit(() -> {
            try {
                extractorProcess.waitFor();
            } catch (Exception e) {
                status.set(MigrationStatus.error("Error while waiting for DB extractor to finish", e));
            }
            status.set(MigrationStatus.FINISHED);
        });

        return target;
    }

    public MigrationStatus getStatus() {
        return status.get();
    }
}
