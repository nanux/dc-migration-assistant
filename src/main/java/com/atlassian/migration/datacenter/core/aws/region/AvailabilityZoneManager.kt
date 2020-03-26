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
package com.atlassian.migration.datacenter.core.aws.region

import com.atlassian.migration.datacenter.core.aws.GlobalInfrastructure
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.AvailabilityZone

class AvailabilityZoneManager(
    private val credentialsProvider: AwsCredentialsProvider,
    private val globalInfrastructure: GlobalInfrastructure
) : AvailabilityZoneService {
    /**
     * @param region
     * @return a list of [AvailabilityZone][software.amazon.awssdk.services.ec2.model.AvailabilityZone] for a region
     * @throws InvalidAWSRegionException
     */
    @Throws(InvalidAWSRegionException::class)
    override fun getAZForRegion(region: Region): List<AvailabilityZone> {
        return if (isValidRegion(region.toString())) {
            val ec2 = getEC2Client(region)
            val zonesResponse = ec2.describeAvailabilityZones()
            zonesResponse.availabilityZones()
        } else {
            throw InvalidAWSRegionException()
        }
    }

    private fun getEC2Client(region: Region): Ec2Client {
        return Ec2Client.builder().region(region).credentialsProvider(credentialsProvider).build()
    }

    private fun isValidRegion(testRegion: String): Boolean {
        return globalInfrastructure
            .regions
            .stream()
            .anyMatch { region: String? -> region == testRegion }
    }
}