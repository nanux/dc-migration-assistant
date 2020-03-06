package com.atlassian.migration.datacenter.spi.fs;

import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationReport;

/**
 * Service managing migration process of the application home folder to a remote location.
 */
public interface FilesystemMigrationService {
    /**
     * Start migration of the application home. This is a long running blocking operation and should be run in
     * separate thread or scheduled job. It finds all files located in the home (or shared home in case
     * of data center deployment) and upload it to the remote location.
     */
    void startMigration();

    /**
     * Provides filesystem migration report that can be used to monitor the operation
     *
     * @return migration report
     */
    FileSystemMigrationReport getReport();

    /**
     * Return true if the filesystem migration is in non-terminal state
     * @return true if the filesystem migration is in progress
     */
    boolean isRunning();
}
