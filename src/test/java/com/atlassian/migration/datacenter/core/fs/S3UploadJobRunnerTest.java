package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFileSystemMigrationReport;
import com.atlassian.migration.datacenter.spi.fs.FilesystemMigrationService;
import com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.scheduler.status.RunOutcome;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3UploadJobRunnerTest {

    @Mock
    FilesystemMigrationService filesystemMigrationService;

    @Mock
    JobRunnerRequest jobRunnerRequest;

    @InjectMocks
    S3UploadJobRunner s3UploadJobRunner;

    @Test
    void shouldNotRunJobWhenJobIsAlreadyRunning() {
        when(filesystemMigrationService.isRunning()).thenReturn(true);
        JobRunnerResponse jobRunnerResponse = s3UploadJobRunner.runJob(jobRunnerRequest);
        Assertions.assertEquals(RunOutcome.ABORTED, jobRunnerResponse.getRunOutcome());
    }


    @Test
    void shouldNotRunJobWhenMigrationStageIsInvalid() throws Exception {
        when(filesystemMigrationService.isRunning()).thenReturn(false);
        Mockito.doThrow(InvalidMigrationStageError.class).when(filesystemMigrationService).startMigration();

        JobRunnerResponse jobRunnerResponse = s3UploadJobRunner.runJob(jobRunnerRequest);
        Assertions.assertEquals(RunOutcome.FAILED, jobRunnerResponse.getRunOutcome());
    }

    @Test
    void shouldCompleteJobRunSuccessfully() throws Exception {
        DefaultFileSystemMigrationReport report = new DefaultFileSystemMigrationReport(null,null);
        report.setStatus(FilesystemMigrationStatus.DONE);

        when(filesystemMigrationService.isRunning()).thenReturn(false);

        when(filesystemMigrationService.getReport()).thenReturn(report);
        Mockito.doNothing().when(filesystemMigrationService).startMigration();

        JobRunnerResponse jobRunnerResponse = s3UploadJobRunner.runJob(jobRunnerRequest);
        Assertions.assertEquals(RunOutcome.SUCCESS, jobRunnerResponse.getRunOutcome());
    }
}