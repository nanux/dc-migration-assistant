package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.migration.datacenter.core.exceptions.FileUploadException;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

public interface Uploader {
    void upload(BlockingQueue<Optional<Path>> queue) throws FileUploadException;
}
