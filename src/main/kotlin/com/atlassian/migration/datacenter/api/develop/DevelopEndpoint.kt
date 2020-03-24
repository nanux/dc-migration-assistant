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
package com.atlassian.migration.datacenter.api.develop

import com.atlassian.migration.datacenter.spi.MigrationService
import com.atlassian.migration.datacenter.spi.MigrationStage
import com.google.common.collect.ImmutableMap
import org.slf4j.LoggerFactory
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/develop")
class DevelopEndpoint(private val migrationService: MigrationService) {

    companion object {
        private val logger = LoggerFactory.getLogger(DevelopEndpoint::class.java)
    }

    @PUT
    @Path("/migration/stage")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun setMigrationStage(targetStage: MigrationStage): Response {
        return try {
            val currentStage = migrationService.currentStage
            migrationService.transition(currentStage, targetStage)
            Response
                    .ok(ImmutableMap.of("targetStage", migrationService.currentStage.toString()))
                    .build()
        } catch (e: Exception) {
            logger.warn("Cannot parse the migration stage", e)
            Response
                    .status(Response.Status.CONFLICT)
                    .entity(ImmutableMap.of<String?, String?>("error", "Unable to transition migration to $targetStage"))
                    .build()
        }
    }

}