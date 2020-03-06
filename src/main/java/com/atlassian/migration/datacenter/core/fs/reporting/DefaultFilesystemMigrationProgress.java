package com.atlassian.migration.datacenter.core.fs.reporting;

import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationProgress;

import java.util.concurrent.atomic.AtomicLong;

public class DefaultFilesystemMigrationProgress implements FileSystemMigrationProgress {

    private AtomicLong numFilesMigrated = new AtomicLong(0);

    private AtomicLong filesFound = new AtomicLong(0);

    private AtomicLong filesInFlight = new AtomicLong(0);

    @Override
    public Long getNumberOfFilesFound() {
        return filesFound.get();
    }

    @Override
    public void reportFileFound() {
        filesFound.incrementAndGet();
    }

    @Override
    public Long getNumberOfFilesInFlight() {
        return filesInFlight.get();
    }

    @Override
    public void reportFileInFlight() {
        filesInFlight.incrementAndGet();
    }

    @Override
    public Long getCountOfMigratedFiles() {
        return numFilesMigrated.get();
    }

    @Override
    public void reportFileMigrated() {
        filesInFlight.decrementAndGet();
        numFilesMigrated.incrementAndGet();
    }
}
