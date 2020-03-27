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

import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationProgress
import java.util.concurrent.atomic.AtomicLong

class DefaultFilesystemMigrationProgress : FileSystemMigrationProgress {
    private val numFilesMigrated = AtomicLong(0)
    private val filesFound = AtomicLong(0)
    private val fileUploadsCommenced = AtomicLong(0)
    override fun getNumberOfFilesFound(): Long {
        return filesFound.get()
    }

    override fun reportFileFound() {
        filesFound.incrementAndGet()
    }

    override fun getNumberOfCommencedFileUploads(): Long {
        return fileUploadsCommenced.get()
    }

    override fun reportFileUploadCommenced() {
        fileUploadsCommenced.incrementAndGet()
    }

    override fun getCountOfMigratedFiles(): Long {
        return numFilesMigrated.get()
    }

    override fun reportFileMigrated() {
        numFilesMigrated.incrementAndGet()
    }
}