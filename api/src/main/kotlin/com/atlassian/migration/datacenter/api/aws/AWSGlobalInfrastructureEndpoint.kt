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

import com.atlassian.migration.datacenter.core.aws.GlobalInfrastructure
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/aws/global-infrastructure")
class AWSGlobalInfrastructureEndpoint(private val globalInfrastructure: GlobalInfrastructure) {
    /**
     * @return A response with all AWS regions
     */
    @GET
    @Path("/regions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun getRegions(): Response {
        val regions = globalInfrastructure.regions ?: return Response.serverError().build()
        return Response
            .ok(regions)
            .build()
    }
}