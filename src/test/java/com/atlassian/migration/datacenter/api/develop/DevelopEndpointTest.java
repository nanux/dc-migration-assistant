package com.atlassian.migration.datacenter.api.develop;

import com.atlassian.migration.datacenter.spi.MigrationService;
import com.atlassian.migration.datacenter.spi.MigrationStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevelopEndpointTest {
    @Mock
    MigrationService migrationService;

    DevelopEndpoint endpoint;

    @BeforeEach
    void setup() {
        endpoint = new DevelopEndpoint(migrationService);
    }

    @Test
    void shouldSetStageWithCorrectTargetStage() throws Exception {
        final MigrationStage initialStage = MigrationStage.AUTHENTICATION;
        when(migrationService.getCurrentStage()).thenReturn(initialStage);
        String param = "FS_MIGRATION_COPY";

        endpoint.setMigrationStage(new DevelopEndpoint.Stage(param));

        MigrationStage expectedStage = MigrationStage.valueOf(param);
        verify(migrationService).transition(initialStage, expectedStage);
    }
}
