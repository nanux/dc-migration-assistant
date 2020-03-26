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
package com.atlassian.migration.datacenter.spi.fs.reporting

import com.fasterxml.jackson.annotation.JsonAutoDetect
import java.nio.file.Path

@JsonAutoDetect
class FailedFileMigration {
    var filePath: Path? = null
        private set
    var reason: String? = null
        private set

    constructor()
    constructor(filePath: Path?, reason: String?) {
        this.filePath = filePath
        this.reason = reason
    }

    override fun equals(other: Any?): Boolean {
        if (other is FailedFileMigration) {
            val that = other
            return filePath == that.filePath && reason == that.reason
        }
        return false
    }
}