import { callAppRest } from '../utils/api';

const dbAPIBase = 'migation/db';
const dbReport = dbAPIBase + '/report';

enum DBMigrationStatus {
    NOT_STARTED,
    FAILED,
    EXPORTING,
    UPLOADING,
    IMPORTING,
    DONE,
}

// Partial mapping of backend migration status to simplified
// UI-friendly version of the DB-related states. See
// MigrationStage.java for the canonical list.
enum DBMigrationStatusMap {
    NOT_STARTED = DBMigrationStatus.NOT_STARTED,
    AUTHENTICATION = DBMigrationStatus.NOT_STARTED,
    PROVISION_APPLICATION = DBMigrationStatus.NOT_STARTED,
    PROVISION_APPLICATION_WAIT = DBMigrationStatus.NOT_STARTED,
    PROVISION_MIGRATION_STACK = DBMigrationStatus.NOT_STARTED,
    PROVISION_MIGRATION_STACK_WAIT = DBMigrationStatus.NOT_STARTED,

    FS_MIGRATION_COPY = DBMigrationStatus.NOT_STARTED,
    FS_MIGRATION_COPY_WAIT = DBMigrationStatus.NOT_STARTED,

    OFFLINE_WARNING = DBMigrationStatus.NOT_STARTED,

    DB_MIGRATION_EXPORT = DBMigrationStatus.EXPORTING,
    DB_MIGRATION_EXPORT_WAIT = DBMigrationStatus.EXPORTING,

    DB_MIGRATION_UPLOAD = DBMigrationStatus.UPLOADING,
    DB_MIGRATION_UPLOAD_WAIT = DBMigrationStatus.UPLOADING,

    DATA_MIGRATION_IMPORT = DBMigrationStatus.IMPORTING,
    DATA_MIGRATION_IMPORT_WAIT = DBMigrationStatus.IMPORTING,

    VALIDATE = DBMigrationStatus.DONE,
    CUTOVER = DBMigrationStatus.DONE,
    FINISHED = DBMigrationStatus.DONE,
    ERROR = DBMigrationStatus.FAILED,
}
