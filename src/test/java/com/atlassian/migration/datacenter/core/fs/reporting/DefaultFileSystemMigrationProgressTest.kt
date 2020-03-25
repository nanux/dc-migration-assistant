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
package com.atlassian.migration.datacenter.core.fs.reporting

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.stream.IntStream

class DefaultFileSystemMigrationProgressTest {
    private var sut: DefaultFilesystemMigrationProgress? = null

    @BeforeEach
    fun setUp() {
        sut = DefaultFilesystemMigrationProgress()
    }

    @Test
    fun shouldBeInitialisedWithNoCompleteFiles() {
        Assertions.assertEquals(0, sut!!.getCountOfMigratedFiles())
    }

    @Test
    fun shouldAddMigratedFileToMigratedFiles() {
        sut!!.reportFileMigrated()
        Assertions.assertEquals(1, sut!!.getCountOfMigratedFiles())
    }

    @Test
    fun shouldCountMigratingFile() {
        sut!!.reportFileFound()
        Assertions.assertEquals(1, sut!!.getNumberOfFilesFound())
    }

    @Test
    fun shouldCountCommencedFileUploads() {
        sut!!.reportFileUploadCommenced()
        Assertions.assertEquals(1, sut!!.getNumberOfCommencedFileUploads())
    }

    @Test
    fun shouldHandleLargeNumberOfMigratedFiles() {
        val numFilesToMigrate = 1000000
        IntStream.range(0, numFilesToMigrate).forEach { i: Int -> sut!!.reportFileMigrated() }
        Assertions.assertEquals(numFilesToMigrate.toLong(), sut!!.getCountOfMigratedFiles())
    }
}