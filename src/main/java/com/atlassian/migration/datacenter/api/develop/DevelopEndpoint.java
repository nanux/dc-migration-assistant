package com.atlassian.migration.datacenter.api.develop;

import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.spi.MigrationService;
import com.atlassian.migration.datacenter.spi.MigrationStage;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.ImmutableMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/develop")
public class DevelopEndpoint {
    private MigrationService migrationService;

    public DevelopEndpoint(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @PUT
    @Path("/migration/stage")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setMigrationStage(Stage stage) {
        try {
            final MigrationStage targetStage = MigrationStage.valueOf(stage.getStage());
            final MigrationStage currentStage = migrationService.getCurrentStage();
            migrationService.transition(currentStage, targetStage);
            return Response
                    .ok(ImmutableMap.of("stage", migrationService.getCurrentStage()))
                    .build();
        } catch (InvalidMigrationStageError | IllegalArgumentException e) {
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity(ImmutableMap.of("error", String.format("Unable to transition migration to %s", stage.getStage())))
                    .build();
        }
    }

    @JsonAutoDetect
    static class Stage {
        private String stage;

        public Stage(String stage) {
            this.stage = stage;
        }

        public String getStage() {
            return stage;
        }

        public void setStage(String stage) {
            this.stage = stage;
        }
    }
}
