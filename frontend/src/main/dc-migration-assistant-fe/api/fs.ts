import { callAppRest } from '../utils/api';

enum RestApiPathConstants {
    fsStatusRestPath = `migration/fs/report`,
    fsStartRestPath = `migration/fs/start`,
}

type FailedFile = {
    path: string;
    reason: string;
};

type FileSystemMigrationStatusResponse = {
    status: 'NOT_STARTED' | 'FAILED' | 'UPLOADING' | 'DOWNLOADING' | 'DONE';
    failedFiles: Array<FailedFile>;
    uploadedFiles: number;
    filesFound: number;
    crawlingFinished: boolean;
    filesInFlight: number;
    downloadedFiles: number;
};

type FileSystemMigrationStartResponse = {
    error?: string;
    status?: string;
    migrationScheduled?: boolean;
};

export const fs = {
    getFsMigrationStatus: (): Promise<FileSystemMigrationStatusResponse> => {
        return callAppRest('GET', RestApiPathConstants.fsStatusRestPath).then(result =>
            result.json()
        );
    },

    startFsMigration: (): Promise<void> => {
        return callAppRest('PUT', RestApiPathConstants.fsStartRestPath).then(result => {
            if (result.ok) {
                return Promise.resolve();
            }
            return result.json().then(json => {
                const errorJson = json as FileSystemMigrationStartResponse;
                if (errorJson.error) {
                    // Probably invalid migration stage
                    return Promise.reject(new Error(errorJson.error));
                }
                if (errorJson.status) {
                    // FS migration is already in progress
                    return Promise.resolve();
                }
                if (errorJson.migrationScheduled === false) {
                    return Promise.reject(new Error('Unable to start file system migration'));
                }
                return Promise.reject(JSON.stringify(result));
            });
        });
    },
};
