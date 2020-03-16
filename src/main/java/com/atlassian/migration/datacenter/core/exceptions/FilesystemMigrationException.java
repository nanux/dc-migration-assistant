package com.atlassian.migration.datacenter.core.exceptions;

public class FilesystemMigrationException extends Exception {
    public FilesystemMigrationException(String message) {
        super(message);
    }

    public FilesystemMigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}