package com.atlassian.migration.datacenter.spi;

/**
 * Represents all possible states of an on-premise to cloud migration.
 */
public enum MigrationStage {
    ERROR("error"),
    FINISHED("finished"),
    CUTOVER("cutover"),
    VALIDATE("validate"),
    DB_MIGRATION_IMPORT("db_migration_down"), DB_MIGRATION_EXPORT("db_migration_up"),
    OFFLINE_WARNING("cutover_warning"),
    FS_MIGRATION_IMPORT("fs_migration_down"), FS_MIGRATION_EXPORT("fs_migration_up"), READY_FS_MIGRATION("ready_fs_migration"),
    WAIT_PROVISION_MIGRATION_STACK("wait_provision_migration"), PROVISION_MIGRATION_STACK("provision_migration"),
    WAIT_PROVISION_APPLICATION("wait_provision_app"), PROVISION_APPLICATION("provision_app"),
    AUTHENTICATION("authentication"),
    NOT_STARTED("");

    private String key;

    MigrationStage(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }
}
