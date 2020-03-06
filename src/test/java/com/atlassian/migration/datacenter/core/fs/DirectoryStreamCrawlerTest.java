package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFileSystemMigrationErrorReport;
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFilesystemMigrationProgress;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationErrorReport;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoryStreamCrawlerTest {
    @TempDir
    Path tempDir;

    private Crawler directoryStreamCrawler;
    private ConcurrentLinkedQueue<Path> queue;
    private Set<Path> expectedPaths;
    private FileSystemMigrationErrorReport errorReport;
    private FileSystemMigrationProgress progress;

    @BeforeEach
    void createFiles() throws Exception {
        queue = new ConcurrentLinkedQueue<>();
        expectedPaths = new HashSet<>();
        errorReport = new DefaultFileSystemMigrationErrorReport();
        progress = new DefaultFilesystemMigrationProgress();
        directoryStreamCrawler = new DirectoryStreamCrawler(errorReport, progress);

        expectedPaths.add(Files.write(tempDir.resolve("newfile.txt"), "newfile content".getBytes()));
        final Path subdirectory = Files.createDirectory(tempDir.resolve("subdirectory"));
        expectedPaths.add(Files.write(subdirectory.resolve("subfile.txt"), "subfile content in the subdirectory".getBytes()));
    }

    @Test
    void shouldListAllSubdirectories() throws Exception {
        directoryStreamCrawler = new DirectoryStreamCrawler(errorReport, progress);
        directoryStreamCrawler.crawlDirectory(tempDir, queue);

        expectedPaths.forEach(path -> assertTrue(queue.contains(path), String.format("Expected %s is absent from crawler queue", path)));
    }

    @Test
    void incorrectStartDirectoryShouldReport() {
        assertThrows(IOException.class, () -> directoryStreamCrawler.crawlDirectory(Paths.get("nonexistent-directory-2010"), queue));
    }

    @Test
    void shouldReportFileAsFoundWhenCrawled() throws Exception {
        directoryStreamCrawler = new DirectoryStreamCrawler(errorReport, progress);
        directoryStreamCrawler.crawlDirectory(tempDir, queue);

        assertEquals(expectedPaths.size(), progress.getNumberOfFilesFound());
    }

    @Test
    @Disabled("Simulating AccessDenied permission proved complicated in an unit test")
    void inaccessibleSubdirectoryIsReportedAsFailed() throws IOException {
        final Path directory = Files.createDirectory(tempDir.resolve("non-readable-subdir"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("-wx-wx-wx")));

        directoryStreamCrawler.crawlDirectory(tempDir, queue);

        assertEquals(errorReport.getFailedFiles().size(), 1);
    }
}
