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

package com.atlassian.migration.datacenter.core.aws.db;

import java.util.Optional;

public class MigrationStatus
{
    public enum State {
        NOT_STARTED,
        DUMP_IN_PROGRESS,
        DUMP_COMPLETE,
        UPLOAD_IN_PROGRESS,
        UPLOAD_COMPLETE,
        ERROR,
        FINISHED;
    }

    private final State state;
    private final Optional<Throwable> exception;
    private final String message;

    MigrationStatus(State state, String message)
    {
        this.state = state;
        this.message = message;
        this.exception = Optional.empty();
    }

    MigrationStatus(State state, String message, Throwable exception)
    {
        this.state = state;
        this.exception = Optional.of(exception);
        this.message = message;
    }

    public static final MigrationStatus NOT_STARTED = new MigrationStatus(State.NOT_STARTED, "Not started");
    public static final MigrationStatus DUMP_IN_PROGRESS = new MigrationStatus(State.DUMP_IN_PROGRESS, "Database dump in progress.");
    public static final MigrationStatus DUMP_COMPLETE = new MigrationStatus(State.DUMP_COMPLETE, "Database dump complete.");
    public static final MigrationStatus UPLOAD_IN_PROGRESS = new MigrationStatus(State.UPLOAD_IN_PROGRESS, "Database upload in progress.");
    public static final MigrationStatus UPLOAD_COMPLETE = new MigrationStatus(State.UPLOAD_COMPLETE, "Database upload complete.");
    public static final MigrationStatus FINISHED = new MigrationStatus(State.FINISHED, "Finished");

    public static MigrationStatus error(String message, Throwable exception) {
        return new MigrationStatus(State.ERROR, message, exception);
    }
}
