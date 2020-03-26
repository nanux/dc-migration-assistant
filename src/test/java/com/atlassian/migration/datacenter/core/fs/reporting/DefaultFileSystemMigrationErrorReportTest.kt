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

import com.atlassian.migration.datacenter.spi.fs.reporting.FailedFileMigration
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultFileSystemMigrationErrorReportTest {
    private var sut: DefaultFileSystemMigrationErrorReport? = null

    @BeforeEach
    fun setUp() {
        sut = DefaultFileSystemMigrationErrorReport()
    }

    @Test
    fun shouldBeInitialisedWithNoErrors() {
        Assertions.assertTrue(sut!!.getFailedFiles().isEmpty(), "expected failed files to be empty on fresh report")
    }

    @Test
    fun shouldAddReportedErrorsToFailedFiles() {
        val testFile = Paths.get("file")
        val testReason = "it broke"
        sut!!.reportFileNotMigrated(FailedFileMigration(testFile, testReason))
        Assertions.assertEquals(1, sut!!.getFailedFiles().size)
        Assertions.assertTrue(sut!!.getFailedFiles().contains(FailedFileMigration(testFile, testReason)))
    }
}