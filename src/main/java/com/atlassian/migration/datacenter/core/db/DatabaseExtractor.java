package com.atlassian.migration.datacenter.core.db;

import com.atlassian.migration.datacenter.core.exceptions.DatabaseMigrationFailure;

import java.io.File;

/**
 * Copyright Atlassian: 10/03/2020
 */
public interface DatabaseExtractor
{
    Process startDatabaseDump(File target) throws DatabaseMigrationFailure;
    Process startDatabaseDump(File target, Boolean parallel) throws DatabaseMigrationFailure;

    void dumpDatabase(File to) throws DatabaseMigrationFailure;
}
