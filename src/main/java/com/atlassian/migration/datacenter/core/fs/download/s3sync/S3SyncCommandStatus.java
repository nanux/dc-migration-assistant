package com.atlassian.migration.datacenter.core.fs.download.s3sync;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonAutoDetect
public class S3SyncCommandStatus {

    private boolean finished;
    private boolean hasErrors;
    private int code;
    private double progress;
    private double total;
    private int filesRemaining;
    private boolean calculating;

    @JsonProperty("status")
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private void unpackStatus(Map<String, Object> status) {
        progress = (double)status.get("progress");
        total = (double)status.get("total");
        filesRemaining = (int)status.get("files_remaining");
        calculating = (boolean)status.get("isCalculating");
    }

    public boolean isComplete() {
        return finished;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public int getExitCode() {
        return code;
    }

    public boolean isCalculating() {
        return calculating;
    }

    public double getBytesDownloaded() {
        return progress;
    }

    public double getTotalBytesToDownload() {
        return total;
    }

    public int getFilesRemainingToDownload() {
        return filesRemaining;
    }
}
