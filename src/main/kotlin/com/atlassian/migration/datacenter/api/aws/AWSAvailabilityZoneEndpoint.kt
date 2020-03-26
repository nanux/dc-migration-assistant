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

import com.atlassian.migration.datacenter.core.aws.region.AvailabilityZoneService
import com.atlassian.migration.datacenter.core.aws.region.InvalidAWSRegionException
import com.atlassian.migration.datacenter.core.aws.region.RegionService
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import software.amazon.awssdk.regions.Region

@Path("aws/availabilityZones")
class AWSAvailabilityZoneEndpoint(
    private val availabilityZoneService: AvailabilityZoneService,
    private val regionService: RegionService
) {
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    fun getAvailabilityZoneList(): Response {
        val currentRegion = Region.of(regionService.getRegion())
        return findAZListForRegion(currentRegion)
    }

    @GET
    @Path("/{region}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAvailabilityZoneList(@PathParam("region") region: String?): Response {
        val searchRegion = Region.of(region)
        return findAZListForRegion(searchRegion)
    }

    private fun findAZListForRegion(region: Region): Response {
        return try {
            Response
                    .ok(availabilityZoneService.getAZForRegion(region).map { it.zoneName() }.sorted().toList())
                    .build()
        } catch (ex: InvalidAWSRegionException) {
            Response.status(Response.Status.NOT_FOUND).build()
        }
    }
}