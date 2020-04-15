import { callAppRest } from '../utils/api';

enum RestApiPathConstants {
    fsStatusRestPath = `migration/fs/report`,
}

type FailedFile = {
    path: string;
    reason: string;
};

type FileSystemMigrationStatusResponse = {
    status: 'NOT_STARTED' | 'FAILED' | 'RUNNING' | 'DONE';
    failedFiles: Array<FailedFile>;
    uploadedFiles: number;
    filesFound: number;
    allFilesFound: boolean;
    filesInFlight: number;
    donwloadedFiles: number;
};

export const fs = {
    getFsMigrationStatus: (): Promise<FileSystemMigrationStatusResponse> => {
        return callAppRest('GET', RestApiPathConstants.fsStatusRestPath).then(result =>
            result.json()
        );
    },
};
