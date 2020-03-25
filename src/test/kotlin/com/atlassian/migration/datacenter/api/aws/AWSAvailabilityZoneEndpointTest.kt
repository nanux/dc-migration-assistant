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
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.model.AvailabilityZone
import java.util.*
import javax.ws.rs.core.Response

@ExtendWith(MockKExtension::class)
class AWSAvailabilityZoneEndpointTest {
    @MockK
    lateinit var availabilityZoneService: AvailabilityZoneService
    @MockK
    lateinit var regionService: RegionService
    @InjectMockKs
    lateinit var availabilityZoneEndpoint: AWSAvailabilityZoneEndpoint

    @BeforeEach
    fun init() = MockKAnnotations.init(this)

    @Test
    fun testAvailabilityZoneEndpointWithRegion() {
        val mockRegion = Region.of("eu-central-1")
        every { availabilityZoneService.getAZForRegion(mockRegion) } returns buildAZList()
        val response = availabilityZoneEndpoint.getAvailabilityZoneList(mockRegion.toString())
        val responseList = response.entity as List<*>
        Assertions.assertEquals(Response.Status.OK.statusCode, response.status)
        Assertions.assertEquals(3, responseList.size)
    }

    @Test
    fun testAvailabilityZoneEndpointWithInvalidRegion() {
        val mockRegion = Region.of("eu-central-2")
        every { availabilityZoneService.getAZForRegion(mockRegion) } throws InvalidAWSRegionException()
        val response = availabilityZoneEndpoint.getAvailabilityZoneList(mockRegion.toString())
        Assertions.assertEquals(Response.Status.NOT_FOUND.statusCode, response.status)
    }

    @Test
    fun testAvailabilityZoneEndpoint() {
        val mockRegion = Region.of("eu-central-1")
        every { regionService.getRegion() } returns "eu-central-1"
        every { availabilityZoneService.getAZForRegion(mockRegion) } returns buildAZList()

        val response = availabilityZoneEndpoint.getAvailabilityZoneList()

        val responseList = response.entity as List<*>
        Assertions.assertEquals(Response.Status.OK.statusCode, response.status)
        Assertions.assertEquals(3, responseList.size)
    }

    @Test
    fun testAvailabilityZoneEndpointNotFound() {
        val mockRegion = Region.of("eu-central-2")
        every { regionService.getRegion() } returns "eu-central-2"
        every { availabilityZoneService.getAZForRegion(mockRegion) } throws InvalidAWSRegionException()
        val response = availabilityZoneEndpoint.getAvailabilityZoneList()
        Assertions.assertEquals(Response.Status.NOT_FOUND.statusCode, response.status)
    }

    companion object {
        private fun buildAZList(): List<AvailabilityZone> {
            val azList: MutableList<AvailabilityZone> = ArrayList()
            azList.add(AvailabilityZone.builder().zoneName("eu-central-1a").build())
            azList.add(AvailabilityZone.builder().zoneName("eu-central-1b").build())
            azList.add(AvailabilityZone.builder().zoneName("eu-central-1c").build())
            return azList
        }
    }
}