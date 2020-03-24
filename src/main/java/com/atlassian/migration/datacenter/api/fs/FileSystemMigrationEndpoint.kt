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
package com.atlassian.migration.datacenter.api.fs

import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError
import com.atlassian.migration.datacenter.spi.fs.FilesystemMigrationService
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.ImmutableMap
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/migration/fs")
class FileSystemMigrationEndpoint(private val fsMigrationService: FilesystemMigrationService) {
    private val mapper: ObjectMapper = ObjectMapper()

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/start")
    fun runFileMigration(): Response {
        return if (fsMigrationService.isRunning) {
            Response
                    .status(Response.Status.CONFLICT)
                    .entity(ImmutableMap.of("status", fsMigrationService.report.status))
                    .build()
        } else try {
            val started = fsMigrationService.scheduleMigration()
            val builder = if (started) Response.status(Response.Status.ACCEPTED) else Response.status(Response.Status.CONFLICT)
            builder
                    .entity(ImmutableMap.of("migrationScheduled", started))
                    .build()
        } catch (invalidMigrationStageError: InvalidMigrationStageError) {
            Response
                    .status(Response.Status.CONFLICT)
                    .entity(ImmutableMap.of("error", invalidMigrationStageError.message))
                    .build()
        }
    }

    @Path("/report")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    fun getFilesystemMigrationStatus(): Response {
        val report = fsMigrationService.report
                ?: return Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(ImmutableMap.of("error", "no file system migration exists"))
                        .build()
        return try {
            Response
                    .ok(mapper.writeValueAsString(report))
                    .build()
        } catch (e: JsonProcessingException) {
            Response
                    .serverError()
                    .entity(String.format("Unable to get file system status. Please contact support and show them this error: %s", e.message))
                    .build()
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/abort")
    fun abortFilesystemMigration(): Response {
        return try {
            fsMigrationService.abortMigration()
            Response
                    .ok(ImmutableMap.of("cancelled", true))
                    .build()
        } catch (e: InvalidMigrationStageError) {
            Response.status(Response.Status.CONFLICT)
                    .entity(ImmutableMap.of("error", "filesystem migration is not in progress"))
                    .build()
        }
    }

    init {
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
    }
}