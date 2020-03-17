package com.atlassian.migration.datacenter.core.fs.download.s3sync;

import com.atlassian.migration.datacenter.core.aws.SSMApi;
import com.atlassian.migration.datacenter.core.fs.download.s3sync.S3SyncFileSystemDownloader.IndeterminateS3SyncStatusException;
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

    public static final String SYNC_STATUS_SUCCESS_COMPLETE_JSON = "{\"finished\": true, \"code\": \"0\"}";
    private static final String SYNC_STATUS_DETERMINED_PARTIAL = "{\"status\": {\"progress\": 49492787.2, \"files_remaining\": 528, \"total\": 451411968.0, \"isCalculating\": false}, \"hasErrors\": false}\n";
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
    void shouldGetStatusOfSsmCommand() throws IndeterminateS3SyncStatusException {
        when(mockSsmApi.runSSMDocument(anyString(), anyString(), anyMap())).thenReturn("status-command-invocation");

        GetCommandInvocationResponse mockStatusResponse = GetCommandInvocationResponse.builder()
                .status(CommandInvocationStatus.SUCCESS)
                .standardOutputContent(SYNC_STATUS_SUCCESS_COMPLETE_JSON)
                .build();
        when(mockSsmApi.getSSMCommand(anyString(), anyString())).thenReturn(mockStatusResponse);

        S3SyncCommandStatus status = sut.getFileSystemDownloadStatus();

        assertTrue(status.isComplete());
        assertEquals(status.getExitCode(), 0);
        assertFalse(status.hasErrors());
    }

    @Test
    void shouldGetStatusWhenSyncIsPartiallyCompleteButNoLongerCalculating() throws IndeterminateS3SyncStatusException {
        when(mockSsmApi.runSSMDocument(anyString(), anyString(), anyMap())).thenReturn("status-command-invocation");

        GetCommandInvocationResponse mockStatusResponse = GetCommandInvocationResponse.builder()
                .status(CommandInvocationStatus.SUCCESS)
                .standardOutputContent(SYNC_STATUS_DETERMINED_PARTIAL)
                .build();
        when(mockSsmApi.getSSMCommand(anyString(), anyString())).thenReturn(mockStatusResponse);

        S3SyncCommandStatus status = sut.getFileSystemDownloadStatus();

        assertFalse(status.isCalculating());
        assertFalse(status.hasErrors());
        assertEquals(49492787.2, status.getBytesDownloaded());
        assertEquals(451411968.0, status.getTotalBytesToDownload());
        assertEquals(528, status.getFilesRemainingToDownload());
    }
}