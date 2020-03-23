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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import javax.ws.rs.core.Response

@ExtendWith(MockitoExtension::class)
class AWSGlobalInfrastructureEndpointTest {
    @Mock
    private val mockGlobalInfrastructure: GlobalInfrastructure? = null
    @InjectMocks
    private val sut: AWSGlobalInfrastructureEndpoint? = null

    @Test
    fun itShouldReturnServerErrorWhenGlobalInfrastructureModuleFails() {
        Mockito.`when`(mockGlobalInfrastructure?.regions).thenReturn(null)
        val res = sut?.getRegions()
        Assertions.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.statusCode, res?.status)
    }

    @Test
    fun itShouldReturnAllRegions() {
        val regionOne = "atlassian-east-1"
        val regionTwo = "atlassian-west-1"
        Mockito.`when`(mockGlobalInfrastructure?.regions).thenReturn(listOf(regionOne, regionTwo))
        val res = sut?.getRegions()
        Assertions.assertEquals(Response.Status.OK.statusCode, res?.status)
        Assertions.assertEquals(listOf(regionOne, regionTwo), res?.entity)
    }
}