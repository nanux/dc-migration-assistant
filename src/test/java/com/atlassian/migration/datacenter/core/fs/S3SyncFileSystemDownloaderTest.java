package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.migration.datacenter.core.aws.SSMApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

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

}