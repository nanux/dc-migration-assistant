package com.atlassian.migration.datacenter.api;

import com.atlassian.migration.datacenter.core.ModalMigrationStageWorker;
import com.atlassian.migration.datacenter.core.ModalMigrationStageWorker.DCMigrationAssistantMode;
import com.atlassian.migration.datacenter.spi.MigrationService;
import com.atlassian.migration.datacenter.spi.MigrationStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API Endpoint for managing in-product DC migrations.
 * Supports get and create.
 */
@Path("/migration")
public class MigrationEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(MigrationEndpoint.class);
    private final MigrationService migrationService;

    private final ModalMigrationStageWorker modalMigrationStageWorker;

    public MigrationEndpoint(MigrationService migrationService, ModalMigrationStageWorker modalMigrationStageWorker) {
        this.migrationService = migrationService;
        this.modalMigrationStageWorker = modalMigrationStageWorker;
    }

    /**
     * @return A response with the status of the current migration
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMigrationStatus() {
        if (migrationService.getMigrationStage() == MigrationStage.NOT_STARTED) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .build();
        } else {
            return Response
                    .ok(migrationService.getMigrationStage().toString())
                    .build();
        }
    }

    /**
     * Creates a new migration if none exists. Otherwise will respond with a 400 and an error message.
     *
     * @return no content if successful or 400 and error  message if a migration already exists.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMigration() {
        if (migrationService.startMigration()) {
            return Response
                    .noContent()
                    .build();
        } else {
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity("migration already exists")
                    .build();
        }
    }

    /**
     * Sets the current mode of the migration assistant
     * @see DCMigrationAssistantMode
     * @param request a JSON object with a single key - "mode", the value of which must be a valid mode
     *                - no-verify
     *                - passthrough
     *                - default
     */
    @Path("/mode")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setAppMode(Map<String, String> request) {
        String requestMode = request.get("mode");
        try {
            DCMigrationAssistantMode mode = DCMigrationAssistantMode.fromModeName(requestMode);
            modalMigrationStageWorker.setMode(mode);
            return Response
                    .ok()
                    .build();
        } catch (IllegalArgumentException e) {
            logger.error("unable to set application mode to: {}", requestMode, e);
            Map<String, String> error = new HashMap<String, String>() {{
                put("error", "mode: " + requestMode + " is not a valid mode");
            }};

            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .build();
        }
    }

    /**
     * Gets the current mode of the migration assistant
     * @see DCMigrationAssistantMode
     */
    @Path("/mode")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAppMode() {
        return Response
                .ok(modalMigrationStageWorker.getMode())
                .build();
    }

}
