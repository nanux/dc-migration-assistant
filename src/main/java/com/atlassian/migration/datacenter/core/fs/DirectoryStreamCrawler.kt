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

import com.atlassian.migration.datacenter.core.util.UploadQueue
import com.atlassian.migration.datacenter.spi.fs.reporting.FailedFileMigration
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationReport
import com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.function.Consumer

class DirectoryStreamCrawler(private val report: FileSystemMigrationReport) : Crawler {
    @Throws(IOException::class)
    override fun crawlDirectory(start: Path, queue: UploadQueue<Path?>) {
        try {
            val paths: DirectoryStream<Path>
            paths = Files.newDirectoryStream(start)
            listDirectories(queue, paths)
            logger.info("Crawled and added {} files for upload.", report.getNumberOfFilesFound())
        } catch (e: NoSuchFileException) {
            logger.error("Failed to find path $start", e)
            report.reportFileNotMigrated(FailedFileMigration(start, e.message))
            report.status = FilesystemMigrationStatus.FAILED
            throw e
        } finally {
            try {
                queue.finish()
            } catch (e: InterruptedException) {
                logger.error("Failed to finalise upload queue.", e)
            }
        }
    }

    private fun listDirectories(queue: UploadQueue<Path?>, paths: DirectoryStream<Path>) {
        paths.forEach(Consumer { p: Path ->
            if (Files.isDirectory(p)) {
                logger.trace("Found directory while crawling home: {}", p)
                try {
                    Files.newDirectoryStream(p.toAbsolutePath()).use { newPaths -> listDirectories(queue, newPaths) }
                } catch (e: Exception) {
                    logger.error("Error when traversing directory {}, with exception {}", p, e)
                    report.reportFileNotMigrated(FailedFileMigration(p, e.message))
                }
            } else {
                try {
                    logger.trace("queueing file: {}", p)
                    queue.put(p)
                } catch (e: InterruptedException) {
                    logger.error("Error when queuing {}, with exception {}", p, e)
                    report.reportFileNotMigrated(FailedFileMigration(p, e.message))
                }
                report.reportFileFound()
            }
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DirectoryStreamCrawler::class.java)
    }

}