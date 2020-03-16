package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.migration.datacenter.core.aws.SSMApi;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ssm.model.CommandInvocationStatus;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("integration")
@ExtendWith(MockitoExtension.class)
class S3SyncFileSystemDownloaderIT {

    @Mock
    SSMApi mockSsmApi;

    @InjectMocks
    S3SyncFileSystemDownloader sut;

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

    /**
     * This test takes 10 seconds hence integration test
     */
    @Test
    void shouldThrowWhenCommandDoesNotSucceedWithinTimeout() {
        when(mockSsmApi.getSSMCommand(any(), anyString())).thenReturn(
                GetCommandInvocationResponse.builder()
                        .status(CommandInvocationStatus.DELAYED)
                        .build());

        assertThrows(S3SyncFileSystemDownloader.CannotLaunchCommandException.class, () -> sut.initiateFileSystemDownload());
    }
}