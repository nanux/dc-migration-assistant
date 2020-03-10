package com.atlassian.migration.datacenter.spi.fs.reporting;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.Duration;

@JsonSerialize(as = FileSystemMigrationReport.class)
public interface FileSystemMigrationReport extends FileSystemMigrationErrorReport, FileSystemMigrationProgress {

    void setStatus(FilesystemMigrationStatus status);

    FilesystemMigrationStatus getStatus();

    Duration getElapsedTime();

    /**
     * Text representation of filesystem migration report. This can be used to consume the report in the logs.
     * @return human readable representation of the migration report
     */
    String toString();
}
