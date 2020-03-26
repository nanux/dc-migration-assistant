/*
 * Copyright 2020 Atlassian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atlassian.migration.datacenter.api.aws

import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError
import com.atlassian.migration.datacenter.spi.infrastructure.ApplicationDeploymentService
import com.atlassian.migration.datacenter.spi.infrastructure.ProvisioningConfig
import com.google.common.collect.ImmutableMap
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import org.slf4j.LoggerFactory

/**
 * REST API Endpoint for managing AWS provisioning.
 */
@Path("/aws/stack")
class CloudFormationEndpoint(private val deploymentService: ApplicationDeploymentService) {
    companion object {
        private val log = LoggerFactory.getLogger(CloudFormationEndpoint::class.java)
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun provisionInfrastructure(provisioningConfig: ProvisioningConfig): Response {
        return try {
            val stackName = provisioningConfig.stackName
            deploymentService.deployApplication(stackName, provisioningConfig.params)
            // Should be updated to URI location after get stack details Endpoint is built
            Response.status(Response.Status.ACCEPTED).entity(stackName).build()
        } catch (e: InvalidMigrationStageError) {
            log.error("Migration stage is not valid.", e)
            Response
                    .status(Response.Status.CONFLICT)
                    .entity(ImmutableMap.of("error", e.message))
                    .build()
        }
    }

    @GET
    @Path("/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun infrastructureStatus(): Response {
        return try {
            val status = deploymentService.deploymentStatus()
            Response.ok(ImmutableMap.of("status", status)).build()
        } catch (e: Exception) {
            Response.status(Response.Status.NOT_FOUND).entity(ImmutableMap.of("error", e.message)).build()
        }
    }
}