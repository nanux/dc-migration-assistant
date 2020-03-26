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
import java.nio.file.Path
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.slf4j.LoggerFactory

class FilesystemUploader(private val crawler: Crawler, private val uploader: Uploader) {
    private val pool: ExecutorService = Executors.newFixedThreadPool(2)

    @Throws(FileUploadException::class)
    fun uploadDirectory(dir: Path) {
        val queue = UploadQueue<Path>(uploader.maxConcurrent())
        val crawlFuture = pool.submit<Boolean> {
            crawler.crawlDirectory(dir, queue)
            true
        }
        val uploadFuture = pool.submit<Boolean> {
            uploader.upload(queue)
            true
        }
        try {
            crawlFuture.get()
            uploadFuture.get()
        } catch (e: InterruptedException) {
            throw FileUploadException("Failed to traverse/upload filesystem: $dir", e)
        } catch (e: ExecutionException) {
            throw FileUploadException("Failed to traverse/upload filesystem: $dir", e.cause)
        }
        pool.shutdown()
    }

    /**
     * Abort the migration process and shuts down all executor services in the pool
     */
    fun abort() {
        val runnables = pool.shutdownNow()
        logger.warn("Shut down executors, list of task not commenced: {}", runnables)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FilesystemUploader::class.java)
    }
}