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
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.util.HashSet
import java.util.function.Consumer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

internal class DirectoryStreamCrawlerTest {
    @TempDir
    lateinit var tempDir: Path
    lateinit var directoryStreamCrawler: Crawler
    val queue: UploadQueue<Path> = UploadQueue(10)
    val expectedPaths: MutableSet<Path> = HashSet()
    val report: FileSystemMigrationReport = DefaultFileSystemMigrationReport()

    @BeforeEach
    @Throws(Exception::class)
    fun createFiles() {
        directoryStreamCrawler = DirectoryStreamCrawler(report)
        expectedPaths.add(Files.write(tempDir.resolve("newfile.txt"), "newfile content".toByteArray()))
        val subdirectory = Files.createDirectory(tempDir.resolve("subdirectory"))
        expectedPaths.add(
            Files.write(
                subdirectory.resolve("subfile.txt"),
                "subfile content in the subdirectory".toByteArray()
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun shouldListAllSubdirectories() {
        directoryStreamCrawler.crawlDirectory(tempDir, queue)
        expectedPaths.forEach(Consumer { path: Path ->
            Assertions.assertTrue(
                queue.contains(path),
                String.format("Expected %s is absent from crawler queue", path)
            )
        })
    }

    @Test
    fun incorrectStartDirectoryShouldReport() {
        Assertions.assertThrows(IOException::class.java) {
            directoryStreamCrawler.crawlDirectory(
                Paths.get("nonexistent-directory-2010"),
                queue
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldReportFileAsFoundWhenCrawled() {
        directoryStreamCrawler.crawlDirectory(tempDir, queue)
        Assertions.assertEquals(expectedPaths.size.toLong(), report.getNumberOfFilesFound())
    }

    @Test
    @Disabled("Simulating AccessDenied permission proved complicated in an unit test")
    @Throws(IOException::class)
    fun inaccessibleSubdirectoryIsReportedAsFailed() {
        Files.createDirectory(
            tempDir.resolve("non-readable-subdir"),
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("-wx-wx-wx"))
        )
        directoryStreamCrawler.crawlDirectory(tempDir, queue)
        Assertions.assertEquals(report.getFailedFiles().size, 1)
    }
}