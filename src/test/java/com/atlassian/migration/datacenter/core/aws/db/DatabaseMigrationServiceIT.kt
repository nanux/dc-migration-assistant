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
package com.atlassian.migration.datacenter.core.aws.db

import com.atlassian.migration.datacenter.core.application.ApplicationConfiguration
import com.atlassian.migration.datacenter.core.application.DatabaseConfiguration
import com.atlassian.migration.datacenter.util.AwsCredentialsProviderShim
import com.atlassian.util.concurrent.Supplier
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.MountableFile
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.ExecutionException

@Tag("integration")
@Testcontainers
@ExtendWith(MockitoExtension::class)
internal class DatabaseMigrationServiceIT {
    @Container
    var s3 = LocalStackContainer()
            .withServices(LocalStackContainer.Service.S3)
            .withEnv("DEFAULT_REGION", Region.US_EAST_1.toString())
    lateinit var s3client: S3AsyncClient
    val bucket = "trebuchet-testing"

    @Mock(lenient = true)
    var configuration: ApplicationConfiguration? = null

    @TempDir
    var tempDir: Path? = null

    @BeforeEach
    @Throws(Exception::class)
    fun setUp() {
        Mockito.`when`(configuration!!.getDatabaseConfiguration())
                .thenReturn(DatabaseConfiguration(DatabaseConfiguration.DBType.POSTGRESQL,
                        postgres.containerIpAddress,
                        postgres.getMappedPort(5432),
                        postgres.databaseName,
                        postgres.username,
                        postgres.password))
        s3client = S3AsyncClient.builder()
                .endpointOverride(URI(s3.getEndpointConfiguration(LocalStackContainer.Service.S3).serviceEndpoint))
                .credentialsProvider(AwsCredentialsProviderShim(s3.defaultCredentialsProvider))
                .region(Region.US_EAST_1)
                .build()
        val req = CreateBucketRequest.builder()
                .bucket(bucket)
                .build()
        val resp = s3client?.createBucket(req)?.get()
        resp?.sdkHttpResponse()?.isSuccessful?.let { Assertions.assertTrue(it) }
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testDatabaseMigration() {
        val service = DatabaseMigrationService(configuration!!, tempDir!!, Supplier { s3client })
        service.postConstruct()
        service.performMigration()
        val req = HeadObjectRequest.builder()
                .bucket(bucket)
                .key("db.dump/toc.dat")
                .build()
        val resp = s3client!!.headObject(req).get()
        Assertions.assertTrue(resp.sdkHttpResponse().isSuccessful)
    }

    companion object {
        @Container
        var postgres = PostgreSQLContainer<Nothing>("postgres:9.6").apply {
            withDatabaseName("jira")
            withCopyFileToContainer(MountableFile.forClasspathResource("db/jira.sql"), "/docker-entrypoint-initdb.d/jira.sql") as PostgreSQLContainer<*>
        }
    }
}