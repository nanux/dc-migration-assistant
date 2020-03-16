package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.migration.datacenter.core.exceptions.FileUploadException;
import com.atlassian.migration.datacenter.core.util.UploadQueue;

import java.nio.file.Path;

public interface Uploader {
    void upload(UploadQueue<Path> queue) throws FileUploadException;
    Integer maxConcurrent();
}
