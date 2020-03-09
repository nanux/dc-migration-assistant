package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.migration.datacenter.core.aws.auth.AtlassianPluginAWSCredentialsProvider;
import com.atlassian.migration.datacenter.core.aws.region.RegionService;
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.spi.MigrationServiceV2;
import com.atlassian.migration.datacenter.spi.MigrationStage;
import com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.regions.Region;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3FilesystemMigrationServiceTest {
    @Mock
    RegionService regionService;

    @Mock
    AtlassianPluginAWSCredentialsProvider credentialsProvider;

    @Mock
    JiraHome jiraHome;

    @Mock
    MigrationServiceV2  migrationService;

    @InjectMocks
    S3FilesystemMigrationService fsService;

    @BeforeEach
    void setUp() {
        when(this.migrationService.getCurrentStage()).thenReturn(MigrationStage.FS_MIGRATION_COPY);
    }

    @Test
    void migrationShouldFailWithIncorrectDirectory() throws InvalidMigrationStageError {
        Path nonexistentDir = Paths.get(UUID.randomUUID().toString());
        when(jiraHome.getHome()).thenReturn(nonexistentDir.toFile());
        when(regionService.getRegion()).thenReturn(Region.US_EAST_1.toString());

        fsService.startMigration();

        assertEquals(FilesystemMigrationStatus.FAILED, fsService.getReport().getStatus());
        verify(migrationService).transition(MigrationStage.FS_MIGRATION_COPY, MigrationStage.WAIT_FS_MIGRATION_COPY);
    }
}
