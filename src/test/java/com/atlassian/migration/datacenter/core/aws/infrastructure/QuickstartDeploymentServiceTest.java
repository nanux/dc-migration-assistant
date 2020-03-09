package com.atlassian.migration.datacenter.core.aws.infrastructure;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.migration.datacenter.core.ModalMigrationStageWorker;
import com.atlassian.migration.datacenter.core.aws.CfnApi;
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.dto.MigrationContext;
import com.atlassian.migration.datacenter.spi.MigrationServiceV2;
import com.atlassian.migration.datacenter.spi.MigrationStage;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;

import java.util.HashMap;

import static com.atlassian.migration.datacenter.core.ModalMigrationStageWorker.MIGRATION_MODE_PLUGIN_SETTINGS_KEY;
import static com.atlassian.migration.datacenter.spi.infrastructure.ApplicationDeploymentService.ApplicationDeploymentStatus.CREATE_IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuickstartDeploymentServiceTest {

    static final String STACK_NAME = "my-stack";
    static final HashMap<String, String> STACK_PARAMS = new HashMap<String, String>() {{
        put("parameter", "value");
    }};

    @Mock
    CfnApi mockCfnApi;

    @Mock
    MigrationServiceV2 mockMigrationService;

    @Mock
    ActiveObjects mockAo;

    @Mock
    PluginSettingsFactory mockPluginSettingsFactory;

    @Mock
    PluginSettings mockPluginSettings;

    @Mock
    MigrationContext mockContext;

    private QuickstartDeploymentService deploymentService;

    @BeforeEach
    void setUp() {
        when(mockPluginSettingsFactory.createGlobalSettings()).thenReturn(mockPluginSettings);
        when(mockPluginSettings.get(MIGRATION_MODE_PLUGIN_SETTINGS_KEY)).thenReturn(ModalMigrationStageWorker.DCMigrationAssistantMode.DEFAULT);

        when(mockMigrationService.getCurrentStage()).thenReturn(MigrationStage.PROVISION_APPLICATION);

        ModalMigrationStageWorker worker = new ModalMigrationStageWorker(mockPluginSettingsFactory, mockMigrationService);

        deploymentService = new QuickstartDeploymentService(mockAo, mockCfnApi, mockMigrationService, worker);
    }

    @Test
    void shouldDeployQuickStart() throws InvalidMigrationStageError {
        initialiseValidMigration();

        deploySimpleStack();

        verify(mockCfnApi).provisionStack("https://aws-quickstart.s3.amazonaws.com/quickstart-atlassian-jira/templates/quickstart-jira-dc-with-vpc.template.yaml", STACK_NAME, STACK_PARAMS);
    }

    @Test
    void shouldReturnInProgressWhileDeploying() throws InvalidMigrationStageError {
        initialiseValidMigration();
        when(mockContext.getApplicationDeploymentId()).thenReturn(STACK_NAME);
        when(mockCfnApi.getStatus(STACK_NAME)).thenReturn(StackStatus.CREATE_IN_PROGRESS);

        deploySimpleStack();

        assertEquals(CREATE_IN_PROGRESS, deploymentService.getDeploymentStatus());
    }

    @Test
    void shouldTransitionToWaitingForDeploymentWhileDeploymentIsCompleting() throws InvalidMigrationStageError, InterruptedException {
        initialiseValidMigration();
        when(mockCfnApi.getStatus(STACK_NAME)).thenReturn(StackStatus.CREATE_IN_PROGRESS);

        deploySimpleStack();

        Thread.sleep(100);

        verify(mockMigrationService).transition(MigrationStage.PROVISION_APPLICATION, MigrationStage.WAIT_PROVISION_APPLICATION);
    }

    @Test
    void shouldTransitionMigrationServiceStateWhenDeploymentFinishes() throws InterruptedException, InvalidMigrationStageError {
        initialiseValidMigration();
        when(mockCfnApi.getStatus(STACK_NAME)).thenReturn(StackStatus.CREATE_COMPLETE);

        deploySimpleStack();

        Thread.sleep(100);

        verify(mockMigrationService).transition(MigrationStage.WAIT_PROVISION_APPLICATION, MigrationStage.PROVISION_MIGRATION_STACK);
    }

    @Test
    void shouldTransitionMigrationServiceToErrorWhenDeploymentFails() throws InterruptedException, InvalidMigrationStageError {
        initialiseValidMigration();
        when(mockCfnApi.getStatus(STACK_NAME)).thenReturn(StackStatus.CREATE_FAILED);

        deploySimpleStack();

        Thread.sleep(100);

        verify(mockMigrationService).error();
    }

    @Test
    void shouldNotInitiateDeploymentIfNotInProvisionApplicationStage() throws InvalidMigrationStageError {
        doThrow(new InvalidMigrationStageError("")).when(mockMigrationService).transition(argThat(argument -> argument.equals(MigrationStage.PROVISION_APPLICATION)), any(MigrationStage.class));

        assertThrows(InvalidMigrationStageError.class, this::deploySimpleStack);
    }

    private void deploySimpleStack() throws InvalidMigrationStageError {
        deploymentService.deployApplication(STACK_NAME, STACK_PARAMS);
    }

    private void initialiseValidMigration() {
        when(mockAo.find(MigrationContext.class)).thenReturn(new MigrationContext[]{mockContext});
    }
}