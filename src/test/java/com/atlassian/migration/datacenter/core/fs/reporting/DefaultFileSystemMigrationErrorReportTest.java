package com.atlassian.migration.datacenter.core.fs.reporting;

import com.atlassian.migration.datacenter.spi.fs.reporting.FailedFileMigration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultFileSystemMigrationErrorReportTest {

    private DefaultFileSystemMigrationErrorReport sut;

    @BeforeEach
    void setUp() {
        sut = new DefaultFileSystemMigrationErrorReport();
    }

    @Test
    void shouldBeInitialisedWithNoErrors() {
        assertTrue(sut.getFailedFiles().isEmpty(), "expected failed files to be empty on fresh report");
    }

    @Test
    void shouldAddReportedErrorsToFailedFiles() {
        final Path testFile = Paths.get("file");
        final String testReason = "it broke";
        sut.reportFileNotMigrated(new FailedFileMigration(testFile, testReason));

        assertEquals(1, sut.getFailedFiles().size());

        assertTrue(sut.getFailedFiles().contains(new FailedFileMigration(testFile, testReason)));
    }
}
