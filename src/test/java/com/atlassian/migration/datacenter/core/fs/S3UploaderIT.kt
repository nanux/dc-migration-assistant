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

import cloud.localstack.TestUtils
import cloud.localstack.docker.LocalstackDockerExtension
import cloud.localstack.docker.annotation.LocalstackDockerProperties
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFileSystemMigrationReport
import com.atlassian.migration.datacenter.core.util.UploadQueue
import com.atlassian.migration.datacenter.spi.fs.reporting.FailedFileMigration
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationReport
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient

@Tag("integration")
@ExtendWith(LocalstackDockerExtension::class, MockitoExtension::class)
@LocalstackDockerProperties(services = ["s3"], imageTag = "0.10.8")
internal class S3UploaderIT {
    var queue: UploadQueue<Path> = UploadQueue(10)
    lateinit var uploader: S3Uploader
    var report: FileSystemMigrationReport = DefaultFileSystemMigrationReport()

    @Mock
    lateinit var mockCredentialsProvider: AwsCredentialsProvider

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        val localStackS3Client = S3AsyncClient.builder()
            .credentialsProvider(mockCredentialsProvider)
            .endpointOverride(URI.create(LOCALSTACK_S3_ENDPOINT))
            .region(Region.US_EAST_1)
            .build()
        val config = S3UploadConfig(TREBUCHET_LOCALSTACK_BUCKET, localStackS3Client, tempDir)
        Mockito.`when`(mockCredentialsProvider.resolveCredentials()).thenReturn(object : AwsCredentials {
            override fun accessKeyId(): String {
                return "fake-access-key"
            }

            override fun secretAccessKey(): String {
                return "fake-secret-key"
            }
        })
        uploader = S3Uploader(config, report)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun uploadShouldUploadPathsFromQueueToS3() {
        val file = addFileToQueue("file")
        queue.finish()
        val s3Client = TestUtils.getClientS3()
        s3Client.createBucket(TREBUCHET_LOCALSTACK_BUCKET)
        uploader.upload(queue)
        Assertions.assertTrue(queue.isEmpty())
        val objectSummaries = s3Client.listObjects(TREBUCHET_LOCALSTACK_BUCKET).objectSummaries
        Assertions.assertEquals(
            report.getFailedFiles().size,
            0, String.format(
                "expected no upload errors but found %s",
                report.getFailedFiles()
                    .stream()
                    .reduce("",
                        { acc: String?, failedMigration: FailedFileMigration ->
                            String.format(
                                "%s%s: %s\n", acc, failedMigration.filePath.toString(),
                                failedMigration.reason
                            )
                        }, { acc: String, partial: String ->
                            """
     $acc
     $partial
     """.trimIndent()
                        })
            )
        )
        Assertions.assertEquals(objectSummaries.size, 1)
        Assertions.assertEquals(objectSummaries[0].key, tempDir.relativize(file).toString())
        Assertions.assertEquals(1, report.getCountOfMigratedFiles())
    }

    @Throws(IOException::class, InterruptedException::class)
    fun addFileToQueue(fileName: String): Path {
        val file = tempDir.resolve(fileName)
        Files.write(file, "".toByteArray())
        queue.put(file)
        return file
    }

    companion object {
        private const val LOCALSTACK_S3_ENDPOINT = "http://localhost:4572"
        private const val TREBUCHET_LOCALSTACK_BUCKET = "trebuchet-localstack-bucket"
    }
}