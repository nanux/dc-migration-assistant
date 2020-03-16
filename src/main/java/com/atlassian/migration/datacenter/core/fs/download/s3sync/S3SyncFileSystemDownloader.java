package com.atlassian.migration.datacenter.core.fs.download.s3sync;

import com.atlassian.migration.datacenter.core.aws.SSMApi;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ssm.model.CommandInvocationStatus;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;

import java.util.Collections;

public class S3SyncFileSystemDownloader {

    private static final Logger logger = LoggerFactory.getLogger(S3SyncCommandStatus.class);

    // FIXME: Should be loaded from migration stack. Defaults to a document created by a migration stack in us-east-1
    private static final String SSM_PLAYBOOK = System.getProperty("com.atlassian.migration.s3sync.documentName", "bpartridge-12-03t15-42-39-migration-helper-SharedHomeDownloadDocument-1C56C88F671YL");
    // FIXME: Should be loaded from migration stack. Defaults to an instance deployed by a migration stack in us-east-1
    private static final String MIGRATION_STACK_INSTANCE = System.getProperty("com.atlassian.migration.instanceId", "i-0353cc9a8ad7dafc2");
    private static final String STATUS_SSM_PLAYBOOK = System.getProperty("com.atlasssian.migration.s3Sync.statusDocumentName", "bogus-document");
    private static final int MAX_STATUS_COMMAND_RETRIES = 3;

    private final SSMApi ssmApi;

    public S3SyncFileSystemDownloader(SSMApi ssmApi) {
        this.ssmApi = ssmApi;
    }

    public void initiateFileSystemDownload() {
        String commandID = ssmApi.runSSMDocument(SSM_PLAYBOOK, MIGRATION_STACK_INSTANCE, Collections.emptyMap());
    }

    public S3SyncCommandStatus getFileSystemDownloadStatus() throws IndeterminateS3SyncStatusException {
        String statusCommandId = ssmApi.runSSMDocument(STATUS_SSM_PLAYBOOK, MIGRATION_STACK_INSTANCE, Collections.emptyMap());

        for (int i = 0; i < MAX_STATUS_COMMAND_RETRIES; i++) {
            GetCommandInvocationResponse statusResponse = ssmApi.getSSMCommand(statusCommandId, MIGRATION_STACK_INSTANCE);

            if (statusResponse.status().equals(CommandInvocationStatus.SUCCESS)) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

                try {
                    return mapper.readValue(statusResponse.standardOutputContent(), S3SyncCommandStatus.class);
                } catch (JsonProcessingException e) {
                    logger.error("unable to unmarshal output from s3 sync status command", e);
                    throw new IndeterminateS3SyncStatusException("unable to read status of sync command");
                }
            }
        }
        return null;
    }

    static class IndeterminateS3SyncStatusException extends Exception {
        IndeterminateS3SyncStatusException(String message) {
            super(message);
        }
    }
}
