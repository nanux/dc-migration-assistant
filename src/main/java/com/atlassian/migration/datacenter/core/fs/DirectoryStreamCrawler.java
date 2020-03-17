package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.migration.datacenter.core.util.UploadQueue;
import com.atlassian.migration.datacenter.spi.fs.reporting.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DirectoryStreamCrawler implements Crawler {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryStreamCrawler.class);

    private FileSystemMigrationReport report;

    public DirectoryStreamCrawler(FileSystemMigrationReport report) {
        this.report = report;
    }

    @Override
    public void crawlDirectory(Path start, UploadQueue<Path> queue) throws IOException {
        try {
            final DirectoryStream<Path> paths;
            paths = Files.newDirectoryStream(start);
            listDirectories(queue, paths);
            logger.info("Crawled and added {} files for upload.", report.getNumberOfFilesFound());

        } catch (NoSuchFileException e) {
            logger.error("Failed to find path "+start, e);
            report.reportFileNotMigrated(new FailedFileMigration(start, e.getMessage()));
            report.setStatus(FilesystemMigrationStatus.FAILED);
            throw e;

        } finally {
            try {
                queue.finish();
            } catch (InterruptedException e) {
                logger.error("Failed to finalise upload queue.", e);
            }
        }
    }

    private void listDirectories(UploadQueue<Path> queue, DirectoryStream<Path> paths) {
        paths.forEach(p -> {
            if (Files.isDirectory(p)) {
                try (final DirectoryStream<Path> newPaths = Files.newDirectoryStream(p.toAbsolutePath())) {
                    listDirectories(queue, newPaths);
                } catch (Exception e) {
                    logger.error("Error when traversing directory {}, with exception {}", p, e);
                    report.reportFileNotMigrated(new FailedFileMigration(p, e.getMessage()));
                }
            } else {
                try {
                    queue.put(p);
                } catch (InterruptedException e) {
                    logger.error("Error when queuing {}, with exception {}", p, e);
                    report.reportFileNotMigrated(new FailedFileMigration(p, e.getMessage()));
                }
                report.reportFileFound();
            }
        });
    }
}
