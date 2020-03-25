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

import org.hamcrest.Matchers
import org.junit.Assume
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

@ExtendWith(MockitoExtension::class)
internal class S3MultiPartUploaderTest {
    @TempDir
    lateinit var tempDir: Path

    @Mock
    lateinit var client: S3AsyncClient

    @Captor
    var valueCaptor: ArgumentCaptor<AsyncRequestBody>? = null
    private val content = "123"

    @Test
    @Throws(Exception::class)
    fun shouldExecuteInitiationAndUploadAndCompletion() {
        val file = createFile()
        val bucket = "bucket"
        val key = "key"
        val config = S3UploadConfig(bucket, client, tempDir)
        val uploader = S3MultiPartUploader(config, file, key)
        val createRequest = CreateMultipartUploadRequest.builder().key(key).bucket(bucket).build()
        Mockito.`when`(client.createMultipartUpload(createRequest))
                .thenReturn(CompletableFuture.completedFuture(CreateMultipartUploadResponse.builder().build()))
        Mockito.`when`(client.uploadPart(ArgumentMatchers.any(UploadPartRequest::class.java), ArgumentMatchers.any(AsyncRequestBody::class.java)))
                .thenReturn(CompletableFuture.completedFuture(UploadPartResponse.builder().build()))
        val completeRequest = CompleteMultipartUploadRequest
                .builder()
                .bucket(bucket)
                .multipartUpload(
                        CompletedMultipartUpload.builder().parts(CompletedPart.builder().partNumber(1).build()).build())
                .key(key)
                .build()
        Mockito.`when`(client.completeMultipartUpload(completeRequest))
                .thenReturn(CompletableFuture.completedFuture(CompleteMultipartUploadResponse.builder().build()))
        uploader.upload()
        Mockito.verify(client).createMultipartUpload(createRequest)
        Mockito.verify(client).uploadPart(ArgumentMatchers.any(UploadPartRequest::class.java), ArgumentMatchers.any(AsyncRequestBody::class.java))
        Mockito.verify(client).completeMultipartUpload(completeRequest)
    }

    @Test
    @Throws(Exception::class)
    fun lastChunkShouldHaveExactRemainingSize() {
        val file = createFile()
        val config = S3UploadConfig("bucket", client, tempDir)
        val uploader = S3MultiPartUploader(config, file, "file")
        val chunkSize = 2
        uploader.setSizeToUpload(chunkSize) // use 2 bytes chunks
        Assume.assumeThat(content.length % chunkSize, Matchers.greaterThan(0))
        Mockito.`when`(client.createMultipartUpload(ArgumentMatchers.any(CreateMultipartUploadRequest::class.java)))
                .thenReturn(CompletableFuture.completedFuture(CreateMultipartUploadResponse.builder().build()))
        Mockito.`when`(client.uploadPart(ArgumentMatchers.any(UploadPartRequest::class.java), ArgumentMatchers.any(AsyncRequestBody::class.java)))
                .thenReturn(CompletableFuture.completedFuture(UploadPartResponse.builder().build()))
        Mockito.`when`(client.completeMultipartUpload(ArgumentMatchers.any(CompleteMultipartUploadRequest::class.java)))
                .thenReturn(CompletableFuture.completedFuture(CompleteMultipartUploadResponse.builder().build()))
        uploader.upload()
        Mockito.verify(client, Mockito.times(2)).uploadPart(ArgumentMatchers.any(UploadPartRequest::class.java), valueCaptor!!.capture())
        val allValues = valueCaptor!!.allValues
        Assertions.assertEquals(2, allValues[0].contentLength().get())
        Assertions.assertEquals(1, allValues[1].contentLength().get())
    }

    @Throws(Exception::class)
    private fun createFile(): File {
        val filename = "file_to_upload.txt"
        val file = tempDir.resolve(filename)
        Files.write(file, content.toByteArray())
        return file.toFile()
    }
}