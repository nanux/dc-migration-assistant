package com.atlassian.migration.datacenter.core.fs.download.s3sync;

import com.atlassian.migration.datacenter.core.aws.SSMApi;
import com.atlassian.migration.datacenter.core.fs.download.s3sync.S3SyncFileSystemDownloader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ssm.model.CommandInvocationStatus;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3SyncFileSystemDownloaderTest {

    @Mock
    SSMApi mockSsmApi;

    @InjectMocks
    S3SyncFileSystemDownloader sut;

    @Test
    void shouldIssueCommandToInstance() {
        sut.initiateFileSystemDownload();

        verify(mockSsmApi).runSSMDocument(anyString(), anyString(), anyMap());
    }

    @Test
    void shouldGetStatusOfSsmCommand() throws S3SyncFileSystemDownloader.IndeterminateS3SyncStatusException {
        when(mockSsmApi.runSSMDocument(anyString(), anyString(), anyMap())).thenReturn("status-command-invocation");

        GetCommandInvocationResponse mockStatusResponse = GetCommandInvocationResponse.builder()
                .status(CommandInvocationStatus.SUCCESS)
                .standardOutputContent("{\"finished\": true, \"code\": \"0\"}")
                .build();
        when(mockSsmApi.getSSMCommand(anyString(), anyString())).thenReturn(mockStatusResponse);

        S3SyncCommandStatus status = sut.getFileSystemDownloadStatus();

        assertTrue(status.isComplete());
        assertEquals(status.getExitCode(), 0);
        assertFalse(status.hasErrors());
    }

}