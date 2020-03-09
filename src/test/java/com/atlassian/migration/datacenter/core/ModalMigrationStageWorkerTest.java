package com.atlassian.migration.datacenter.core;

import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.spi.MigrationServiceV2;
import com.atlassian.migration.datacenter.spi.MigrationStage;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModalMigrationStageWorkerTest {

    @Mock
    MigrationServiceV2 mockMigrationService;

    @Mock
    PluginSettingsFactory mockPluginSettingsFactory;

    @Mock
    PluginSettings mockPluginSettings;

    @InjectMocks
    ModalMigrationStageWorker sut;

    @BeforeEach
    void setUp() {
        when(mockPluginSettingsFactory.createGlobalSettings()).thenReturn(mockPluginSettings);
    }

    @Test
    void shouldTransitionToShortCircuitWhenModeIsPassthrough() throws InvalidMigrationStageError {
        when(mockPluginSettings.get("com.atlassian.migration.datacenter.core.mode")).thenReturn("passthrough");
        when(mockMigrationService.getCurrentStage()).thenReturn(MigrationStage.PROVISION_APPLICATION);

        sut.runAccordingToCurrentMode(Assertions::fail, MigrationStage.AUTHENTICATION, MigrationStage.DB_MIGRATION_EXPORT);

        verify(mockMigrationService).transition(MigrationStage.PROVISION_APPLICATION, MigrationStage.DB_MIGRATION_EXPORT);
    }
}