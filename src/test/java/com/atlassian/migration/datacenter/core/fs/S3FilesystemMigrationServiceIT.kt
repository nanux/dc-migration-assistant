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
package com.atlassian.migration.datacenter.core.fs

import com.atlassian.jira.config.util.JiraHome
import com.atlassian.migration.datacenter.core.aws.auth.AtlassianPluginAWSCredentialsProvider
import com.atlassian.migration.datacenter.core.aws.region.RegionService
import com.atlassian.migration.datacenter.core.fs.download.s3sync.S3SyncFileSystemDownloadManager
import com.atlassian.migration.datacenter.spi.MigrationService
import com.atlassian.migration.datacenter.spi.MigrationStage
import com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus
import com.atlassian.migration.datacenter.util.AwsCredentialsProviderShim
import com.atlassian.scheduler.SchedulerService
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
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*

@Tag("integration")
@Testcontainers
@ExtendWith(MockitoExtension::class)
internal class S3FilesystemMigrationServiceIT {
    @TempDir
    lateinit var dir: Path

    @Mock
    lateinit var regionService: RegionService

    @Mock
    lateinit var migrationService: MigrationService

    @Mock
    lateinit var jiraHome: JiraHome

    @Mock
    lateinit var schedulerService: SchedulerService

    @Mock
    var fileSystemDownloader: S3SyncFileSystemDownloadManager? = null

    lateinit var s3AsyncClient: S3AsyncClient

    private val bucket = "trebuchet-testing"

    @Container
    var s3 = LocalStackContainer()
            .withServices(LocalStackContainer.Service.S3)
            .withEnv("DEFAULT_REGION", Region.US_EAST_1.toString())
    var credentialsProvider: AtlassianPluginAWSCredentialsProvider? = null

    @BeforeEach
    @Throws(Exception::class)
    fun setup() {
        credentialsProvider = AtlassianPluginAWSCredentialsProvider(AwsCredentialsProviderShim(s3.defaultCredentialsProvider))
        s3AsyncClient = S3AsyncClient.builder()
                .endpointOverride(URI(s3.getEndpointConfiguration(LocalStackContainer.Service.S3).serviceEndpoint))
                .credentialsProvider(AwsCredentialsProviderShim(s3.defaultCredentialsProvider))
                .region(Region.US_EAST_1)
                .build()
        val req = CreateBucketRequest.builder()
                .bucket(bucket)
                .build()
        val resp = s3AsyncClient.createBucket(req).get()
        Assertions.assertTrue(resp.sdkHttpResponse().isSuccessful)
    }

    @Throws(IOException::class)
    private fun genRandFile(): Path {
        val file = dir.resolve(UUID.randomUUID().toString())
        val rand = String.format("Testing string %s", Instant.now())
        Files.write(file, setOf(rand))
        return file
    }

    @Test
    @Throws(Exception::class)
    fun testSuccessfulDirectoryMigration(@TempDir dir: Path) {
        Mockito.`when`(jiraHome.home).thenReturn(dir.toFile())
        Mockito.`when`(migrationService.currentStage).thenReturn(MigrationStage.FS_MIGRATION_COPY)
        val file = genRandFile()
        val fsService = S3FilesystemMigrationService(Supplier { s3AsyncClient }, jiraHome, fileSystemDownloader!!, migrationService, schedulerService)
        fsService.postConstruct()
        fsService.startMigration()
        Assertions.assertNotEquals(FilesystemMigrationStatus.FAILED, fsService.getReport().status)
        val req = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(file.fileName.toString())
                .build()
        val resp = s3AsyncClient.headObject(req).get()
        Assertions.assertTrue(resp.sdkHttpResponse().isSuccessful)
    }
}