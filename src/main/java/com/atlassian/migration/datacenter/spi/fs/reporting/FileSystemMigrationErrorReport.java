package com.atlassian.migration.datacenter.spi.fs.reporting;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Set;

/**
 * Represents the error status of a file system migration
 */
@JsonSerialize(as = FileSystemMigrationErrorReport.class)
public interface FileSystemMigrationErrorReport {

    /**
     * Retrieves a set containing the files which have failed to migrate.
     */
    Set<FailedFileMigration> getFailedFiles();

    /**
     * Reports that a file has failed to migrate. Implementers should be careful that the underlying
     * collection is thread safe as this may be called from multiple file upload threads.
     */
    void reportFileNotMigrated(FailedFileMigration failedFileMigration);

}
