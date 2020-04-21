/*
 * Copyright 2020 Atlassian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const dbAPIBase = 'migation/db';
export const dbReport = `${dbAPIBase}/report`;

export enum DBMigrationStatus {
    NOT_STARTED,
    FAILED,
    EXPORTING,
    UPLOADING,
    IMPORTING,
    DONE,
    UNKNOWN,
}

export const toI18nProp = (status: DBMigrationStatus): string => {
    const name = DBMigrationStatus[status].toLowerCase();
    return `atlassian.migration.datacenter.db.status.${name}`;
};

// Partial mapping of backend migration status to simplified
// UI-friendly version of the DB-related states. See
// MigrationStage.java for the canonical list.
/* eslint-disable prettier/prettier */
const dbMigrationStatusMap: Record<string, DBMigrationStatus> = {
    'NOT_STARTED': DBMigrationStatus.NOT_STARTED,
    'AUTHENTICATION': DBMigrationStatus.NOT_STARTED,
    'PROVISION_APPLICATION': DBMigrationStatus.NOT_STARTED,
    'PROVISION_APPLICATION_WAIT': DBMigrationStatus.NOT_STARTED,
    'PROVISION_MIGRATION_STACK': DBMigrationStatus.NOT_STARTED,
    'PROVISION_MIGRATION_STACK_WAIT': DBMigrationStatus.NOT_STARTED,

    'FS_MIGRATION_COPY': DBMigrationStatus.NOT_STARTED,
    'FS_MIGRATION_COPY_WAIT': DBMigrationStatus.NOT_STARTED,

    'OFFLINE_WARNING': DBMigrationStatus.NOT_STARTED,

    'DB_MIGRATION_EXPORT': DBMigrationStatus.EXPORTING,
    'DB_MIGRATION_EXPORT_WAIT': DBMigrationStatus.EXPORTING,

    'DB_MIGRATION_UPLOAD': DBMigrationStatus.UPLOADING,
    'DB_MIGRATION_UPLOAD_WAIT': DBMigrationStatus.UPLOADING,

    'DATA_MIGRATION_IMPORT': DBMigrationStatus.IMPORTING,
    'DATA_MIGRATION_IMPORT_WAIT': DBMigrationStatus.IMPORTING,

    'VALIDATE': DBMigrationStatus.DONE,
    'CUTOVER': DBMigrationStatus.DONE,
    'FINISHED': DBMigrationStatus.DONE,
    'ERROR': DBMigrationStatus.FAILED,
};

export const toUIStatus = (backendStatus: string): DBMigrationStatus => {
    if (dbMigrationStatusMap[backendStatus] === undefined) {
        return DBMigrationStatus.UNKNOWN;
    }
    return dbMigrationStatusMap[backendStatus];
};
