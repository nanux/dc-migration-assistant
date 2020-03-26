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
package com.atlassian.migration.datacenter.core.aws.auth

import com.atlassian.migration.datacenter.core.aws.region.RegionService
import java.util.concurrent.ExecutionException
import java.util.stream.Collectors
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException
import software.amazon.awssdk.services.cloudformation.model.Stack

class ProbeAWSAuth(private val credentialsProvider: AwsCredentialsProvider, private val regionService: RegionService) {
    /**
     * Queries the Cloudformation stacks in the AWS account using the AWS Java SDK V2 to test the credentials
     * have Cloudformation access
     *
     * @return a list containing the names of the stacks in the account in the current region
     */
    fun probeSDKV2(): List<String> {
        val client = CloudFormationAsyncClient
            .builder()
            .region(Region.of(regionService.getRegion()))
            .credentialsProvider(credentialsProvider)
            .build()
        val futureResponse = client.describeStacks()
        return try {
            val response = futureResponse.get()
            response
                .stacks()
                .stream()
                .map { obj: Stack -> obj.stackName() }
                .collect(Collectors.toList())
        } catch (e: InterruptedException) {
            if (e.cause is CloudFormationException) {
                throw (e.cause as CloudFormationException?)!!
            }
            logger.error("unable to get DescribeStacksResponse", e)
            emptyList()
        } catch (e: ExecutionException) {
            if (e.cause is CloudFormationException) {
                throw (e.cause as CloudFormationException?)!!
            }
            logger.error("unable to get DescribeStacksResponse", e)
            emptyList()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProbeAWSAuth::class.java)
    }
}