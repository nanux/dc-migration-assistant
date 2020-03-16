package com.atlassian.migration.datacenter.core.fs.download.s3sync;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect
public class S3SyncCommandStatus {

    private boolean finished;
    private boolean hasErrors;
    private int code;

    public boolean isComplete() {
        return finished;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public int getExitCode() {
        return code;
    }
}
