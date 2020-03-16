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

    private static final Logger logger = LoggerFactory.getLogger(S3SyncFileSystemDownloader.class);

    // FIXME: Should be loaded from migration stack. Defaults to a document created by a migration stack in us-east-1
    private static final String SSM_PLAYBOOK = System.getProperty("com.atlassian.migration.s3sync.documentName", "bpartridge-12-03t15-42-39-migration-helper-SharedHomeDownloadDocument-1C56C88F671YL");
    // FIXME: Should be loaded from migration stack.
    private static final String STATUS_SSM_PLAYBOOK = System.getProperty("com.atlassian.migration.s3sync.statusDocmentName", "fake-document");
    // FIXME: Should be loaded from migration stack. Defaults to an instance deployed by a migration stack in us-east-1
    private static final String MIGRATION_STACK_INSTANCE = System.getProperty("com.atlassian.migration.instanceId", "i-0353cc9a8ad7dafc2");

    private int maxCommandStatusRetries;

    private final SSMApi ssmApi;

    public S3SyncFileSystemDownloader(SSMApi ssmApi) {
        this(ssmApi, 10);
    }

    S3SyncFileSystemDownloader(SSMApi ssmApi, int maxCommandStatusRetries) {
        this.ssmApi = ssmApi;
        this.maxCommandStatusRetries = maxCommandStatusRetries;
    }

    public void initiateFileSystemDownload() throws CannotLaunchCommandException {
        // FIXME: Reload the migration stack instance ID in case instance has gone down during migration
        String commandID = ssmApi.runSSMDocument(SSM_PLAYBOOK, MIGRATION_STACK_INSTANCE, Collections.emptyMap());

        GetCommandInvocationResponse command = null;
        for (int i = 0; i < maxCommandStatusRetries; i++) {
            command = ssmApi.getSSMCommand(commandID, MIGRATION_STACK_INSTANCE);
            final CommandInvocationStatus status = command.status();

            logger.debug("Checking delivery of s3 sync ssm command. Attempt {}. Status is: {}", i, status.toString());

            if (status.equals(CommandInvocationStatus.SUCCESS)) {
                return;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("interrupted while waiting for s3 sync ssm command to be delivered", e);
                throw new CannotLaunchCommandException("unable to launch file system download command");
            }
        }
        throw new CannotLaunchCommandException("unable to launch file system download command successfully. Command status is " + command.status().toString());
    }

    public S3SyncCommandStatus getFileSystemDownloadStatus() throws IndeterminateS3SyncStatusException {
        String statusCommandId = ssmApi.runSSMDocument(STATUS_SSM_PLAYBOOK, MIGRATION_STACK_INSTANCE, Collections.emptyMap());

        for (int i = 0; i < maxCommandStatusRetries; i++) {
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

    public static class CannotLaunchCommandException extends Exception {
        CannotLaunchCommandException(String message) {
            super(message);
        }
    }
}
