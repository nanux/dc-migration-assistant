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

import com.atlassian.migration.datacenter.core.exceptions.FileUploadException
import com.atlassian.migration.datacenter.core.util.UploadQueue
import com.atlassian.migration.datacenter.spi.fs.reporting.FailedFileMigration
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationReport
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.nio.file.Files
import java.nio.file.Path
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.function.Consumer

class S3Uploader(
    private val config: S3UploadConfig,
    private val report: FileSystemMigrationReport
) : Uploader {
    private val responsesQueue: Queue<S3UploadOperation> = LinkedList()
    override fun maxConcurrent(): Int {
        return MAX_OPEN_CONNECTIONS
    }

    @Throws(FileUploadException::class)
    override fun upload(queue: UploadQueue<Path>) {
        try {
            var opt = queue.take()
            while (opt.isPresent) {
                uploadFile(opt.get())
                opt = queue.take()
            }
        } catch (e: InterruptedException) {
            val msg = "InterruptedException while fetching file from queue"
            logger.error(msg, e)
            throw FileUploadException(msg, e)
        }
        responsesQueue.forEach(Consumer { operation: S3UploadOperation -> handlePutObjectResponse(operation) })
        logger.info("Finished uploading files to S3")
    }

    private fun uploadFile(path: Path) {
        if (responsesQueue.size >= MAX_OPEN_CONNECTIONS) {
            logger.trace("Response queue greater than connection threshold. Acknowledging response queue")
            responsesQueue.forEach(Consumer { operation: S3UploadOperation -> handlePutObjectResponse(operation) })
        }
        if (Files.exists(path)) {
            logger.trace("Consuming {} from upload queue", path)
            val key = config.getSharedHome().relativize(path).toString()
            if (path.toFile().length() > MAXIMUM_FILE_SIZE_TO_UPLOAD) {
                logger.debug(
                    "File {} is larger than {}, running multipart upload",
                    path,
                    FileUtils.byteCountToDisplaySize(MAXIMUM_FILE_SIZE_TO_UPLOAD)
                )
                val multiPartUploader = S3MultiPartUploader(config, path.toFile(), key)
                try {
                    multiPartUploader.upload()
                } catch (e: InterruptedException) {
                    logger.error("Error when running multi-part upload for file {} with exception {}", path, e.message)
                } catch (e: ExecutionException) {
                    logger.error("Error when running multi-part upload for file {} with exception {}", path, e.message)
                }
            } else {
                logger.trace("uploading file {}", path)
                val putRequest = PutObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key(key)
                    .build()
                val response = config.getS3AsyncClient().putObject(putRequest, path)
                val uploadOperation = S3UploadOperation(path, response)
                responsesQueue.add(uploadOperation)
                report.reportFileUploadCommenced()
            }
        } else {
            addFailedFile(path, String.format("File doesn't exist: %s", path))
        }
    }

    private fun handlePutObjectResponse(operation: S3UploadOperation) {
        try {
            logger.trace("acknowledging file upload for {}", operation.path)
            val evaluatedResponse = operation.response.get()
            if (!evaluatedResponse.sdkHttpResponse().isSuccessful) {
                val errorMessage = String.format(
                    "Error when uploading %s to S3, %s",
                    operation.path,
                    evaluatedResponse.sdkHttpResponse().statusText()
                )
                logger.warn("error uploading {} to S3 - {}", operation.path, evaluatedResponse)
                addFailedFile(operation.path, errorMessage)
            } else {
                logger.trace("{} migrated successfully", operation.path)
                report.reportFileMigrated()
            }
        } catch (e: InterruptedException) {
            addFailedFile(operation.path, e.message)
        } catch (e: ExecutionException) {
            addFailedFile(operation.path, e.message)
        }
    }

    private fun addFailedFile(path: Path, reason: String?) {
        report.reportFileNotMigrated(FailedFileMigration(path, reason))
        logger.error("File {} wasn't uploaded. Reason: {}", path, reason)
    }

    private class S3UploadOperation internal constructor(
        var path: Path,
        var response: CompletableFuture<PutObjectResponse>
    )

    companion object {
        private val logger = LoggerFactory.getLogger(S3Uploader::class.java)
        const val MAX_OPEN_CONNECTIONS = 50
        private const val MAXIMUM_FILE_SIZE_TO_UPLOAD =
            5 * 1024 * 1024 * 1024L // 5GB  https://docs.aws.amazon.com/AmazonS3/latest/dev/UploadingObjects.html
    }
}