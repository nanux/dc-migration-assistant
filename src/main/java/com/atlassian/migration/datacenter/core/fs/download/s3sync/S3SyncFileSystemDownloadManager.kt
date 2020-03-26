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
package com.atlassian.migration.datacenter.core.fs.download.s3sync

import com.atlassian.migration.datacenter.core.fs.download.s3sync.S3SyncFileSystemDownloader.CannotLaunchCommandException
import com.atlassian.migration.datacenter.core.fs.download.s3sync.S3SyncFileSystemDownloader.IndeterminateS3SyncStatusException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

class S3SyncFileSystemDownloadManager(private val downloader: S3SyncFileSystemDownloader) {
    @Throws(CannotLaunchCommandException::class)
    fun downloadFileSystem() {
        downloader.initiateFileSystemDownload()
        val syncCompleteFuture: CompletableFuture<*> = CompletableFuture<Any>()
        val scheduledFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
            try {
                downloader.getFileSystemDownloadStatus()?.let {
                    logger.debug("got status of file system download: $it")
                    if (it.isComplete()) {
                        logger.debug("file system download is complete")
                        syncCompleteFuture.complete(null)
                    }
                }
            } catch (e: IndeterminateS3SyncStatusException) {
                logger.error("error when retrieving s3 sync status", e)
            }
        }, 0, 5, TimeUnit.MINUTES)
        syncCompleteFuture.whenComplete { _: Any?, _: Throwable? -> scheduledFuture.cancel(true) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(S3SyncFileSystemDownloadManager::class.java)
    }
}