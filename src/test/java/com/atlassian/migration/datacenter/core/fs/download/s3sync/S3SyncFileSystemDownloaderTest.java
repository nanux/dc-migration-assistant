package com.atlassian.migration.datacenter.core.fs.download.s3sync;

import com.atlassian.migration.datacenter.core.aws.SSMApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ssm.model.CommandInvocationStatus;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3SyncFileSystemDownloaderTest {

    @Mock
    SSMApi mockSsmApi;

    S3SyncFileSystemDownloader sut;

    @BeforeEach
    void setUp() {
        sut = new S3SyncFileSystemDownloader(mockSsmApi, 1);
    }

    @Test
    void shouldIssueCommandToInstance() throws S3SyncFileSystemDownloader.CannotLaunchCommandException {
        when(mockSsmApi.getSSMCommand(any(), anyString())).thenReturn(
                GetCommandInvocationResponse.builder()
                        .status(CommandInvocationStatus.SUCCESS)
                        .build());

        sut.initiateFileSystemDownload();

        verify(mockSsmApi).runSSMDocument(anyString(), anyString(), anyMap());
    }

    @Test
    void shouldNotThrowWhenCommandIsIssuedAndSucceeds() {
        when(mockSsmApi.getSSMCommand(any(), anyString())).thenReturn(
                GetCommandInvocationResponse.builder()
                        .status(CommandInvocationStatus.SUCCESS)
                        .build());

        try {
            sut.initiateFileSystemDownload();
        } catch (S3SyncFileSystemDownloader.CannotLaunchCommandException e) {
            fail();
        }
    }

    @Test
    void shouldThrowWhenCommandDoesNotSucceedWithinTimeout() {
        when(mockSsmApi.getSSMCommand(any(), anyString())).thenReturn(
                GetCommandInvocationResponse.builder()
                        .status(CommandInvocationStatus.DELAYED)
                        .build());

        assertThrows(S3SyncFileSystemDownloader.CannotLaunchCommandException.class, () -> sut.initiateFileSystemDownload());
    }

    @Test
    void shouldGetStatusOfSsmCommand() throws com.atlassian.migration.datacenter.core.fs.download.s3sync.S3SyncFileSystemDownloader.IndeterminateS3SyncStatusException {
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