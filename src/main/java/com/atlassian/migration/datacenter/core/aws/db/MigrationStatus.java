package com.atlassian.migration.datacenter.core.aws.db;

import java.util.Optional;

/**
 * Copyright Atlassian: 11/03/2020
 */
public class MigrationStatus
{
    public enum State {
        NOT_STARTED,
        STARTED,
        IN_PROGRESS,
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
    public static final MigrationStatus STARTED = new MigrationStatus(State.STARTED, "Started");
    public static final MigrationStatus IN_PROGRESS = new MigrationStatus(State.IN_PROGRESS, "In Progress");
    public static final MigrationStatus FINISHED = new MigrationStatus(State.FINISHED, "Started");

    public static MigrationStatus error(String message, Throwable exception) {
        return new MigrationStatus(State.ERROR, message, exception);
    }
}
