package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.migration.datacenter.core.util.UploadQueue;

import java.io.IOException;
import java.nio.file.Path;

public interface Crawler {
    void crawlDirectory(Path start, UploadQueue<Path> queue) throws IOException;
}
