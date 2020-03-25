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

import cloud.localstack.TestUtils
import cloud.localstack.docker.LocalstackDockerExtension
import cloud.localstack.docker.annotation.LocalstackDockerProperties
import com.atlassian.migration.datacenter.core.aws.ssm.SSMApi
import com.atlassian.util.concurrent.Supplier
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.CommandInvocationStatus
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationRequest
import software.amazon.awssdk.services.ssm.model.SendCommandRequest
import java.net.URI
import java.util.*

@Tag("integration")
@ExtendWith(LocalstackDockerExtension::class)
@LocalstackDockerProperties(services = ["cloudformation", "s3", "ssm"], imageTag = "0.10.8")
class SSMApiIT {
    private var sut: SSMApi? = null

    companion object {
        private const val LOCALSTACK_SSM_ENDPOINT = "http://localhost:4583"
    }

    @BeforeEach
    fun setUp() {
        sut = SSMApi(Supplier { SsmClient.builder().build() })
    }

    @Test
    @Disabled("Requires true authentication to AWS and a specific EC2 instance to be running")
    fun shouldSendSsmCommandToAWS() {
        val documentName = "AWS-RunShellScript"
        // Node ID of ConfluenceGiveMeAllTheData Confluence Node in us-east-1
        val targetEc2InstanceId = "i-0d536acd983bade05"
        val commandParameters: HashMap<String?, List<String?>?> = object : HashMap<String?, List<String?>?>() {
            init {
                put("commands", listOf("echo 'hello, world'"))
            }
        }
        val commandId = sut!!.runSSMDocument(documentName, targetEc2InstanceId, commandParameters)
        Assertions.assertNotNull(commandId)
    }

    @Test
    @Disabled("Requires true authentication to AWS")
    fun shouldGetCommand() {
        // Command run by an earlier run of shouldSendSsmCommandToAWS
        val commandID = "5c45181c-ed0a-45db-9fb7-3a4cd1edef9d"
        // Node ID of ConfluenceGiveMeAllTheData Confluence Node in us-east-1
        val targetEc2InstanceId = "i-0d536acd983bade05"
        val command = sut!!.getSSMCommand(commandID, targetEc2InstanceId)
        Assertions.assertNotNull(command)
        Assertions.assertEquals(CommandInvocationStatus.SUCCESS, command.status())
        Assertions.assertEquals("hello, world\n", command.standardOutputContent())
    }

    @Test
    @Disabled("Localstack is giving 500s for SSM send-command")
    fun shouldStartSSMCommand() {
        val client = SsmClient.builder()
                .endpointOverride(URI.create(LOCALSTACK_SSM_ENDPOINT))
                .credentialsProvider {
                    object : AwsCredentials {
                        override fun accessKeyId(): String {
                            return TestUtils.TEST_ACCESS_KEY
                        }

                        override fun secretAccessKey(): String {
                            return TestUtils.TEST_SECRET_KEY
                        }
                    }
                }
                .region(Region.of(TestUtils.DEFAULT_REGION))
                .build()
        val request = SendCommandRequest.builder()
                .documentName("document")
                .documentVersion("\$LATEST")
                .timeoutSeconds(600)
                .comment("running command to pull files down as part of migration")
                .parameters(HashMap())
                .instanceIds("i-1234567")
                .outputS3BucketName("migration-bucket")
                .outputS3KeyPrefix("fs-copy-down-log")
                .build()
        client.sendCommand(request)
    }

    @Test
    @Disabled("Localstack is giving 400s for SSM getCommandInvocation")
    fun shouldGetStatusOfRunningCommand() {
        val client = SsmClient.builder()
                .endpointOverride(URI.create(LOCALSTACK_SSM_ENDPOINT))
                .credentialsProvider {
                    object : AwsCredentials {
                        override fun accessKeyId(): String {
                            return TestUtils.TEST_ACCESS_KEY
                        }

                        override fun secretAccessKey(): String {
                            return TestUtils.TEST_SECRET_KEY
                        }
                    }
                }
                .region(Region.of(TestUtils.DEFAULT_REGION))
                .build()
        val request = GetCommandInvocationRequest.builder()
                .commandId("commandID")
                .instanceId("theInstance")
                .build()
        val response = client.getCommandInvocation(request)
        println(response.standardOutputContent())
    }
}