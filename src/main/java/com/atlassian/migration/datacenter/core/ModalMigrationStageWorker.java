package com.atlassian.migration.datacenter.core;

import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.spi.MigrationServiceV2;
import com.atlassian.migration.datacenter.spi.MigrationStage;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ModalMigrationStageWorker {

    private static final Logger logger = LoggerFactory.getLogger(ModalMigrationStageWorker.class);
    private static final String MIGRATION_MODE_PLUGIN_SETTINGS_KEY = "com.atlassian.migration.datacenter.core.mode";

    private final MigrationServiceV2 migrationService;
    private final PluginSettingsFactory pluginSettingsFactory;

    public ModalMigrationStageWorker(@ComponentImport PluginSettingsFactory pluginSettingsFactory, MigrationServiceV2 migrationService) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.migrationService = migrationService;
    }

    /**
     * Determines whether a migration operation should be run given the current mode the app is in.
     * If the app is in the default mode, it will verify the current stage the expected current stage and run the migration operation
     * If the app is in no-verify mode, it will run the migration operation without verifying the current stage
     * If the app is in passthrough mode, it will skip the stage verification, ignore the operation and simply set the migration stage to the passThroughStage
     * @param operation The migration operation e.g. upload files, restore database
     * @param expectedCurrentStage The stage that the migration is expected to be in to run the given operation
     * @param passThroughStage The stage that should be transitioned to if passing through this migration operation
     */
    public void runAccordingToCurrentMode(MigrationOperation operation, MigrationStage expectedCurrentStage, MigrationStage passThroughStage) {
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        String mode = (String) settings.get(MIGRATION_MODE_PLUGIN_SETTINGS_KEY);

        if (mode == null) {
            return;
        }

        if (mode.equals("passthrough")) {
            try {
                migrationService.transition(migrationService.getCurrentStage(), passThroughStage);
            } catch (InvalidMigrationStageError e) {
                logger.error("Unable to transition from current stage to {}", passThroughStage, e);
            }
        } else if (mode.equals("no-verify")) {
            operation.migrate();
        }

    }

    /**
     * Represents one operation in a migration. It should do whatever tasks it deems necessary to facilitate the
     * migration and manage the transitions of the migration service.
     */
    @FunctionalInterface
    public interface MigrationOperation {

        void migrate();
    }

}
