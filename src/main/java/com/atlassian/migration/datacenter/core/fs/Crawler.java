package com.atlassian.migration.datacenter.core.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface Crawler {
    void crawlDirectory(Path start, BlockingQueue<Optional<Path>> queue) throws IOException;
}
