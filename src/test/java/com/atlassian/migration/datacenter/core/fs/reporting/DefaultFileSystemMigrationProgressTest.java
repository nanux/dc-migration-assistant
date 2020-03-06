package com.atlassian.migration.datacenter.core.fs.reporting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultFileSystemMigrationProgressTest {

    private DefaultFilesystemMigrationProgress sut;

    @BeforeEach
    void setUp() {
        sut = new DefaultFilesystemMigrationProgress();
    }

    @Test
    void shouldBeInitialisedWithNoCompleteFiles() {
        assertEquals(0, sut.getCountOfMigratedFiles());
    }

    @Test
    void shouldAddMigratedFileToMigratedFiles() {
        sut.reportFileMigrated();

        assertEquals(1, sut.getCountOfMigratedFiles());
    }

    @Test
    void shouldCountMigratingFile() {
        sut.reportFileFound();

        assertEquals(1, sut.getNumberOfFilesFound());
    }

    @Test
    void shouldCountInFlightFile() {
        sut.reportFileInFlight();

        assertEquals(1, sut.getNumberOfFilesInFlight());
    }

    @Test
    void ShouldRemoveFileFromInFlightWhenItIsMigrated() {
        sut.reportFileInFlight();
        sut.reportFileInFlight();

        assertEquals(2, sut.getNumberOfFilesInFlight());

        sut.reportFileMigrated();

        assertEquals(1, sut.getNumberOfFilesInFlight());
    }

    @Test
    void shouldHandleLargeNumberOfMigratedFiles() {
        int numFilesToMigrate = 1000000;
        IntStream.range(0, numFilesToMigrate).forEach(i -> sut.reportFileMigrated());

        assertEquals(numFilesToMigrate, sut.getCountOfMigratedFiles());
    }
}
