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
import software.amazon.awssdk.services.s3.model.S3Exception
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

@Tag("integration")
@ExtendWith(LocalstackDockerExtension::class, MockitoExtension::class)
@LocalstackDockerProperties(services = ["s3"], imageTag = "0.10.8")
internal class S3MultiPartUploaderIT {
    @TempDir
    var tempDir: Path? = null

    @Mock
    private val mockCredentialsProvider: AwsCredentialsProvider? = null
    private var config: S3UploadConfig? = null

    @BeforeEach
    fun setup() {
        val localStackS3Client = S3AsyncClient.builder()
                .credentialsProvider(mockCredentialsProvider)
                .endpointOverride(URI.create(LOCALSTACK_S3_ENDPOINT))
                .region(Region.US_EAST_1)
                .build()
        config = S3UploadConfig(TREBUCHET_LOCALSTACK_BUCKET, localStackS3Client, tempDir!!)
        Mockito.`when`(mockCredentialsProvider!!.resolveCredentials()).thenReturn(object : AwsCredentials {
            override fun accessKeyId(): String {
                return "fake-access-key"
            }

            override fun secretAccessKey(): String {
                return "fake-secret-key"
            }
        })
    }

    @Test
    @Throws(Exception::class)
    fun multiuploadShouldUploadFileToS3() {
        val filename = "file_to_upload.txt"
        // we use subfolder to verify we are writing the correct key
        val file = createFile("subfolder", filename)
        val key = tempDir!!.relativize(file).toString()
        Files.write(file, "123456789".toByteArray())
        val uploader = S3MultiPartUploader(config!!, file.toFile(), key)
        val s3Client = TestUtils.getClientS3()
        s3Client.createBucket(TREBUCHET_LOCALSTACK_BUCKET)
        uploader.upload()
        val objectSummaries = s3Client.listObjects(TREBUCHET_LOCALSTACK_BUCKET).objectSummaries
        Assertions.assertEquals(1, objectSummaries.size, "Bucket should contain only one file")
        Assertions.assertEquals(key, objectSummaries[0].key, "Object key is different to the filename")
    }

    @Test
    @Throws(Exception::class)
    fun uploadFileInMultipleParts() {
        val filename = "file_to_upload.txt"
        // we use subfolder to verify we are writing the correct key
        val file = createFile("subfolder", filename)
        val key = tempDir!!.relativize(file).toString()
        Files.write(file, "123456789".toByteArray())
        val uploader = S3MultiPartUploader(config!!, file.toFile(), key)
        uploader.setSizeToUpload(1)
        val s3Client = TestUtils.getClientS3()
        s3Client.createBucket(TREBUCHET_LOCALSTACK_BUCKET)
        try {
            uploader.upload()
        } catch (e: Exception) {
            Assertions.assertTrue(e.cause is S3Exception)
            e.cause!!.message!!.contains("Your proposed upload is smaller than the minimum allowed object size")
        }
    }

    @Throws(IOException::class)
    fun createFile(subfolder: String?, fileName: String?): Path {
        Files.createDirectory(tempDir!!.resolve(subfolder))
        val file = tempDir!!.resolve(subfolder).resolve(fileName)
        Files.write(file, "".toByteArray())
        return file
    }

    companion object {
        private const val LOCALSTACK_S3_ENDPOINT = "http://localhost:4572"
        private const val TREBUCHET_LOCALSTACK_BUCKET = "trebuchet-localstack-bucket"
    }
}