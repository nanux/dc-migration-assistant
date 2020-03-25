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

import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFileSystemMigrationReport
import com.atlassian.migration.datacenter.core.util.UploadQueue
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationReport
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

@ExtendWith(MockitoExtension::class)
internal class S3UploaderTest {
    @Mock
    lateinit var s3AsyncClient: S3AsyncClient

    @Mock
    lateinit var s3response: CompletableFuture<PutObjectResponse>

    @Mock
    lateinit var sdkHttpResponse: SdkHttpResponse

    val queue: UploadQueue<Path> = UploadQueue(20)
    lateinit var uploader: S3Uploader
    val report: FileSystemMigrationReport = DefaultFileSystemMigrationReport()

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        val config = S3UploadConfig("bucket-name", s3AsyncClient, tempDir)
        uploader = S3Uploader(config, report)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun uploadShouldConsumePathsWhileCrawlingIsRunning() {
        val putObjectResponse = PutObjectResponse.builder().sdkHttpResponse(sdkHttpResponse).build() as PutObjectResponse
        Mockito.`when`(sdkHttpResponse.isSuccessful).thenReturn(true)
        Mockito.`when`(s3response.get()).thenReturn(putObjectResponse)
        Mockito.`when`(s3AsyncClient.putObject(ArgumentMatchers.any(PutObjectRequest::class.java), ArgumentMatchers.any(Path::class.java))).thenReturn(s3response)
        addFileToQueue("file1")
        val submit = Executors.newFixedThreadPool(1).submit { uploader.upload(queue) }

        // verify consumption of the first path
        Thread.sleep(1000)
        Assertions.assertFalse(submit.isDone)
        Assertions.assertTrue(queue.isEmpty())

        // add new file when the crawler isn't finish
        addFileToQueue("file2")

        // finish crawling
        queue.finish()
        submit.get()

        // upload should finish and there shouldn't be more paths to process
        Assertions.assertTrue(submit.isDone)
        Assertions.assertTrue(queue.isEmpty())
        Assertions.assertTrue(report.getFailedFiles().isEmpty())
    }

    @Test
    @Throws(IOException::class, ExecutionException::class, InterruptedException::class)
    fun uploadShouldReportFileAsMigrated() {
        val putObjectResponse = PutObjectResponse.builder().sdkHttpResponse(sdkHttpResponse).build() as PutObjectResponse
        Mockito.`when`(sdkHttpResponse.isSuccessful).thenReturn(true)
        Mockito.`when`(s3response.get()).thenReturn(putObjectResponse)
        Mockito.`when`(s3AsyncClient.putObject(ArgumentMatchers.any(PutObjectRequest::class.java), ArgumentMatchers.any(Path::class.java))).thenReturn(s3response)
        addFileToQueue("file1")
        queue.finish()
        val submit = Executors.newFixedThreadPool(1).submit { uploader.upload(queue) }
        submit.get()
        Assertions.assertEquals(1, report.getCountOfMigratedFiles())
    }

    @Test
    @Throws(InterruptedException::class)
    fun uploadNonExistentDirectoryShouldReturnFailedCollection() {
        val nonExistentFile = tempDir.resolve("non-existent")
        queue.put(nonExistentFile)
        queue.finish()
        uploader.upload(queue)
        Assertions.assertEquals(report.getFailedFiles().size, 1)
    }

    @Test
    @Throws(Exception::class)
    fun shouldReportFileAsInFlightWhenUploadStarts() {
        val putObjectResponse = PutObjectResponse.builder().sdkHttpResponse(sdkHttpResponse).build() as PutObjectResponse
        Mockito.`when`(sdkHttpResponse.isSuccessful).thenReturn(true)
        Mockito.`when`(s3response.get()).thenReturn(putObjectResponse)
        Mockito.`when`(s3AsyncClient.putObject(ArgumentMatchers.any(PutObjectRequest::class.java), ArgumentMatchers.any(Path::class.java))).thenReturn(s3response)
        addFileToQueue("file1")
        val submit = Executors.newFixedThreadPool(1).submit { uploader.upload(queue) }
        Thread.sleep(100)
        Assertions.assertEquals(1, report.getNumberOfCommencedFileUploads())
        queue.finish()
        submit.get()
    }

    @Throws(IOException::class, InterruptedException::class)
    fun addFileToQueue(fileName: String): Path {
        val file = tempDir.resolve(fileName)
        Files.write(file, "".toByteArray())
        queue.put(file)
        return file
    }
}