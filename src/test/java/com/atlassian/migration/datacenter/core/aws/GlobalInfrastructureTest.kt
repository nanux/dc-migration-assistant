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
package com.atlassian.migration.datacenter.core.aws

import java.util.Arrays
import java.util.stream.Collectors
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.regions.Region

class GlobalInfrastructureTest {
    var sut: GlobalInfrastructure? = null

    @BeforeEach
    fun initialiseSut() {
        sut = GlobalInfrastructure()
    }

    @Test
    fun itShouldReturnAllRegions() {
        val regions: List<String?> = sut!!.regions
        val supportedRegions = Arrays.asList(
            Region.AP_SOUTH_1,
            Region.EU_NORTH_1,
            Region.EU_WEST_3,
            Region.EU_WEST_2,
            Region.EU_WEST_1,
            Region.AP_NORTHEAST_2,
            Region.AP_NORTHEAST_1,
            Region.ME_SOUTH_1,
            Region.CA_CENTRAL_1,
            Region.SA_EAST_1,
            Region.AP_EAST_1,
            Region.AP_SOUTHEAST_1,
            Region.AP_SOUTHEAST_2,
            Region.EU_CENTRAL_1,
            Region.US_EAST_1,
            Region.US_EAST_2,
            Region.US_WEST_1,
            Region.US_WEST_2
        )
            .stream()
            .map { obj: Region -> obj.toString() }
            .collect(Collectors.toList())
        Assertions.assertIterableEquals(supportedRegions, regions)
    }
}