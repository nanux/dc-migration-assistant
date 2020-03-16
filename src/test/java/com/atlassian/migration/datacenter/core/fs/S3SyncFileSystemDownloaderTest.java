package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.migration.datacenter.core.aws.SSMApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ssm.model.CommandInvocationStatus;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3SyncFileSystemDownloaderTest {

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

}