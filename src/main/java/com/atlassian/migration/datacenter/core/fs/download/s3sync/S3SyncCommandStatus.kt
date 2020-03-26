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

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.ImmutableList

/**
 * Data class for reading output from the s3 sync status SSM command.
 * In the event that values can't be read for any reason i.e. if they are absent
 * number values will be set to -1, the status will be assumed to be unfinished and
 * the download will be assumed to have stopped calculating.
 *
 *
 * NOTE: When [S3SyncCommandStatus.isComplete] is true, there will be no values for
 * progress, total or filesRemaining.
 */
@JsonAutoDetect
class S3SyncCommandStatus {
    private val finished = false
    private val code = 0
    private var progress = 0.0
    private var total = 0.0
    private var filesRemaining = 0
    var isCalculating = false
        private set
    private val errors: List<String> = listOf()

    @JsonProperty("status")
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private fun unpackStatus(status: Map<String, Any>?) {
        if (status == null) {
            return
        }
        progress = (status["progress"] ?: -1.0) as Double
        total = (status["total"] ?: -1.0) as Double
        filesRemaining = (status["files_remaining"] ?: -1) as Int
        isCalculating = (status["isCalculating"] ?: false) as Boolean
    }

    fun isComplete(): Boolean {
        return finished
    }

    fun getExitCode(): Int {
        return code
    }

    fun getBytesDownloaded(): Double {
        return progress
    }

    fun getTotalBytesToDownload(): Double {
        return total
    }

    fun getFilesRemainingToDownload(): Int {
        return filesRemaining
    }

    fun hasErrors(): Boolean {
        return errors.isNotEmpty()
    }

    fun getErrors(): List<String> {
        return ImmutableList.copyOf(errors)
    }

    override fun toString(): String {
        return "S3SyncCommandStatus{" +
            "finished=" + finished +
            ", code=" + code +
            ", progress=" + progress +
            ", total=" + total +
            ", filesRemaining=" + filesRemaining +
            ", calculating=" + isCalculating +
            ", errors=" + errors.toTypedArray().contentToString() +
            '}'
    }
}