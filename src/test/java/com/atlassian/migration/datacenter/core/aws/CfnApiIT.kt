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

import cloud.localstack.docker.LocalstackDockerExtension
import cloud.localstack.docker.annotation.LocalstackDockerProperties
import com.atlassian.migration.datacenter.core.aws.region.RegionService
import java.io.IOException
import java.io.StringWriter
import java.net.URI
import java.util.HashMap
import java.util.Optional
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Function
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient
import software.amazon.awssdk.services.cloudformation.model.Stack
import software.amazon.awssdk.services.cloudformation.model.StackInstanceNotFoundException
import software.amazon.awssdk.services.cloudformation.model.StackStatus
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Tag("integration")
@ExtendWith(LocalstackDockerExtension::class)
@LocalstackDockerProperties(services = ["cloudformation", "s3"], imageTag = "0.10.8")
internal class CfnApiIT {
    lateinit var cfnApi: CfnApi

    @BeforeEach
    fun setUp() {
        val client = CloudFormationAsyncClient.builder()
            .credentialsProvider(StubAwsCredentialsProvider())
            .region(Region.AP_SOUTHEAST_2)
            .endpointOverride(LOCALSTACK_CLOUDFORMATION_URI)
            .build()
        cfnApi =
            CfnApi(client, Mockito.mock(AwsCredentialsProvider::class.java), Mockito.mock(RegionService::class.java))
    }

    @Test
    fun shouldRaiseExceptionWhenStackDoesNotExist() {
        val stackArn = "arn:aws:cloudformation:ap-southeast-2:1231231231:stack/i-do-not-exist"
        Assertions.assertThrows(StackInstanceNotFoundException::class.java) { cfnApi.getStatus(stackArn) }
    }

    @Test
    fun shouldProvisionNewCfnStack() {
        val random = UUID
            .randomUUID().toString().split("-").toTypedArray()[0]
        val stackName = String.format("trebuchet-test-%s", random)
        val provisionedStackId = cfnApi.provisionStack(S3_CFN_STACK_URL, stackName, object : HashMap<String, String>() {
            init {
                put("S3BucketName", "$stackName-bucket")
            }
        })
        val stackId = provisionedStackId.get()
        Assertions.assertNotNull(stackId)
        try {
            awaitStackCreation(
                stackName,
                Function { stackName: String? -> cfnApi.getStack(stackName) })[60, TimeUnit.SECONDS]
        } catch (e: Exception) {
            Assertions.fail<Any>("Timeout while waiting for stack creation to complete", e)
        }
    }

    private fun awaitStackCreation(
        stackId: String,
        statusFunc: Function<String, Optional<Stack>>
    ): CompletableFuture<String> {
        val completableFuture = CompletableFuture<String>()
        val scheduledFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
            val stack = statusFunc.apply(stackId)
            if (stack.isPresent) {
                if (stack.get().stackStatus() == StackStatus.CREATE_COMPLETE) {
                    completableFuture.complete(stackId)
                }
            }
        }, 0, 10, TimeUnit.SECONDS)
        completableFuture.whenComplete { result: String?, thrown: Throwable? -> scheduledFuture.cancel(true) }
        return completableFuture
    }

    companion object {
        val LOCALSTACK_CLOUDFORMATION_URI = URI.create("http://localhost:4581")
        val LOCALSTACK_S3_URI = URI.create("http://localhost:4572")
        const val CFN_TEMPLATE_S3_BUCKET_CREATE_JSON = "/cfn/create_s3_bucket.json"
        const val S3_BUCKET_NAME = "dcd-slingshot-templates"
        const val S3_TEMPLATE_BUCKET_KEY = "create_s3_bucket.json"
        val S3_CFN_STACK_URL =
            String.format("%s/%s/%s", LOCALSTACK_S3_URI.toString(), S3_BUCKET_NAME, S3_TEMPLATE_BUCKET_KEY)

        @BeforeAll
        @Throws(IOException::class)
        fun beforeAll() {
            val s3Client = S3Client.builder()
                .region(Region.AP_SOUTHEAST_2)
                .endpointOverride(LOCALSTACK_S3_URI)
                .credentialsProvider(StubAwsCredentialsProvider())
                .build()
            s3Client.createBucket(CreateBucketRequest.builder().bucket(S3_BUCKET_NAME).build())
            val inputStream = CfnApiIT::class.java.getResourceAsStream(CFN_TEMPLATE_S3_BUCKET_CREATE_JSON)
            val writer = StringWriter()
            IOUtils.copy(inputStream, writer)
            val putObjectRequest = PutObjectRequest.builder().bucket(S3_BUCKET_NAME).key(S3_TEMPLATE_BUCKET_KEY)
                .acl(ObjectCannedACL.PUBLIC_READ).build()
            s3Client.putObject(putObjectRequest, RequestBody.fromString(writer.toString()))
            val `object` = s3Client.getObject(
                GetObjectRequest.builder().bucket(S3_BUCKET_NAME).key(S3_TEMPLATE_BUCKET_KEY).build()
            )
            Assertions.assertEquals(`object`.response().contentLength(), writer.toString().length.toLong())
        }
    }
}