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
package com.atlassian.migration.datacenter.core.aws.db

import java.util.*

class MigrationStatus {
    enum class State {
        NOT_STARTED, DUMP_IN_PROGRESS, DUMP_COMPLETE, UPLOAD_IN_PROGRESS, UPLOAD_COMPLETE, ERROR, FINISHED
    }

    private val state: State
    private val exception: Optional<Throwable>
    private val message: String

    internal constructor(state: State, message: String) {
        this.state = state
        this.message = message
        exception = Optional.empty()
    }

    internal constructor(state: State, message: String, exception: Throwable) {
        this.state = state
        this.exception = Optional.of(exception)
        this.message = message
    }

    companion object {
        val NOT_STARTED = MigrationStatus(State.NOT_STARTED, "Not started")
        val DUMP_IN_PROGRESS = MigrationStatus(State.DUMP_IN_PROGRESS, "Database dump in progress.")
        val DUMP_COMPLETE = MigrationStatus(State.DUMP_COMPLETE, "Database dump complete.")
        val UPLOAD_IN_PROGRESS = MigrationStatus(State.UPLOAD_IN_PROGRESS, "Database upload in progress.")
        val UPLOAD_COMPLETE = MigrationStatus(State.UPLOAD_COMPLETE, "Database upload complete.")
        val FINISHED = MigrationStatus(State.FINISHED, "Finished")
        fun error(message: String, exception: Throwable): MigrationStatus {
            return MigrationStatus(State.ERROR, message, exception)
        }
    }
}