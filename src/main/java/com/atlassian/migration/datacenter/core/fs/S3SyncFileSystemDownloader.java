package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.migration.datacenter.core.aws.SSMApi;

import java.util.Collections;
import java.util.HashMap;

public class S3SyncFileSystemDownloader {

    // FIXME: Should be loaded from migration stack. Defaults to a document created by a migration stack in us-east-1
    private static final String SSM_PLAYBOOK = System.getProperty("com.atlassian.migration.s3sync.documentName", "bpartridge-12-03t15-42-39-migration-helper-SharedHomeDownloadDocument-1C56C88F671YL");
    // FIXME: Should be loaded from migration stack. Defaults to an instance deployed by a migration stack in us-east-1
    private static final String MIGRATION_STACK_INSTANCE = System.getProperty("com.atlassian.migration.instanceId", "i-0353cc9a8ad7dafc2");

    private final SSMApi ssmApi;

    public S3SyncFileSystemDownloader(SSMApi ssmApi) {
        this.ssmApi = ssmApi;
    }

    public void initiateFileSystemDownload() {
        String commandID = ssmApi.runSSMDocument(SSM_PLAYBOOK, MIGRATION_STACK_INSTANCE, Collections.emptyMap());
    }

}
