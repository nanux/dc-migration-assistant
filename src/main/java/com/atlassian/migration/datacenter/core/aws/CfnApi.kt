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

import com.atlassian.migration.datacenter.core.aws.region.RegionService
import java.util.Optional
import java.util.Optional.of
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.stream.Collectors
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest
import software.amazon.awssdk.services.cloudformation.model.Parameter
import software.amazon.awssdk.services.cloudformation.model.Stack
import software.amazon.awssdk.services.cloudformation.model.StackInstanceNotFoundException
import software.amazon.awssdk.services.cloudformation.model.StackStatus
import software.amazon.awssdk.services.cloudformation.model.Tag

class CfnApi(val credentialsProvider: AwsCredentialsProvider, val regionManager: RegionService) {

    private var client: Optional<CloudFormationAsyncClient> = Optional.empty()

    constructor(
        client: CloudFormationAsyncClient,
        credentialsProvider: AwsCredentialsProvider,
        regionManager: RegionService
    ) : this(credentialsProvider, regionManager) {
        this.client = of(client)
    }

    /**
     * Lazily create a CFN client; should only be called after necessary AWS information has been provided.
     */
    private fun getClient(): CloudFormationAsyncClient {
        if (client.isPresent) {
            return client.get()
        }
        val constructedClient = CloudFormationAsyncClient.builder()
            .credentialsProvider(credentialsProvider)
            .region(Region.of(regionManager.getRegion()))
            .build()
        this.client = of(constructedClient)
        return constructedClient
    }

    fun getStatus(stackName: String): StackStatus {
        val stack = getStack(stackName)
        if (!stack.isPresent) {
            throw StackInstanceNotFoundException
                .builder()
                .message(String.format("Stack with name %s not found", stackName))
                .build()
        }
        return stack.get().stackStatus()
    }

    fun provisionStack(templateUrl: String, stackName: String, params: Map<String, String>): Optional<String> {
        val parameters = params.entries
            .stream()
            .map { e: Map.Entry<String, String> ->
                Parameter.builder().parameterKey(e.key).parameterValue(e.value).build()
            }
            .collect(Collectors.toSet())
        val tag = Tag.builder()
            .key("created_by")
            .value("atlassian-dcmigration")
            .build()
        val createStackRequest = CreateStackRequest.builder()
            .templateURL(templateUrl)
            .stackName(stackName)
            .parameters(parameters)
            .tags(tag)
            .build()
        return try {
            val stackId = getClient()
                .createStack(createStackRequest)
                .thenApply { obj: CreateStackResponse -> obj.stackId() }
                .get()
            Optional.ofNullable(stackId)
        } catch (e: InterruptedException) {
            Optional.empty()
        } catch (e: ExecutionException) {
            Optional.empty()
        }
    }

    fun getStack(stackName: String?): Optional<Stack> {
        val request = DescribeStacksRequest.builder()
            .stackName(stackName)
            .build()
        val asyncResponse = getClient()
            .describeStacks(request)
        return try {
            val response = asyncResponse.join()
            val stack = response.stacks()[0]
            Optional.ofNullable(stack)
        } catch (e: CompletionException) {
            Optional.empty()
        } catch (e: CancellationException) {
            Optional.empty()
        }
    }
}