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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
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
        when(mockPluginSettings.get("com.atlassian.migration.datacenter.core.mode")).thenReturn(ModalMigrationStageWorker.DCMigrationAssistantMode.PASSTHROUGH);
        when(mockMigrationService.getCurrentStage()).thenReturn(MigrationStage.PROVISION_APPLICATION);

        sut.runAccordingToCurrentMode(Assertions::fail, MigrationStage.AUTHENTICATION, MigrationStage.DB_MIGRATION_EXPORT);

        verify(mockMigrationService).transition(MigrationStage.PROVISION_APPLICATION, MigrationStage.DB_MIGRATION_EXPORT);
    }

    @Test
    void shouldRunOperationEvenWhenCurrentStageIsNotExpectedStageWhenModeIsNoVerify() {
        when(mockPluginSettings.get("com.atlassian.migration.datacenter.core.mode")).thenReturn(ModalMigrationStageWorker.DCMigrationAssistantMode.NO_VERIFY);

        /*
         * This mock is lenient because we want to be explicit that the current stage is not equal to the expected
         * current stage, even when getCurrentStage is not even called for documentation purposes
         */
        lenient().when(mockMigrationService.getCurrentStage()).thenReturn(MigrationStage.PROVISION_APPLICATION);

        AtomicBoolean hasFuncBeenRun = new AtomicBoolean(false);
        sut.runAccordingToCurrentMode(() -> hasFuncBeenRun.set(true), MigrationStage.AUTHENTICATION, MigrationStage.DB_MIGRATION_EXPORT);

        assertTrue(hasFuncBeenRun.get(), "expected operation to be executed when mode is no-verify");
    }

    @ParameterizedTest
    @EnumSource(value = ModalMigrationStageWorker.DCMigrationAssistantMode.class, names = { "DEFAULT" })
    @NullSource
    void shouldRunOperationWhenModeIsDefaultAndCurrentStageMatchesExpectedStage(ModalMigrationStageWorker.DCMigrationAssistantMode mode) {
        when(mockPluginSettings.get("com.atlassian.migration.datacenter.core.mode")).thenReturn(mode);
        final MigrationStage currentStage = MigrationStage.PROVISION_APPLICATION;
        when(mockMigrationService.getCurrentStage()).thenReturn(currentStage);

        AtomicBoolean hasFuncBeenRun = new AtomicBoolean(false);
        sut.runAccordingToCurrentMode(() -> hasFuncBeenRun.set(true), currentStage, MigrationStage.DB_MIGRATION_EXPORT);

        assertTrue(hasFuncBeenRun.get(), "expected operation to be executed when mode is default and current stage matches expected stage");
    }

    @Test
    void shouldNotRunOperationWhenModeIsDefaultAndCurrentStageDoesNotMatchExpectedStage() {
        when(mockPluginSettings.get("com.atlassian.migration.datacenter.core.mode")).thenReturn(ModalMigrationStageWorker.DCMigrationAssistantMode.DEFAULT);
        when(mockMigrationService.getCurrentStage()).thenReturn(MigrationStage.PROVISION_APPLICATION);

        sut.runAccordingToCurrentMode(Assertions::fail, MigrationStage.AUTHENTICATION, MigrationStage.DB_MIGRATION_EXPORT);
    }
}
