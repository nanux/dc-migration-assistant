/*
 * Copyright 2020 Atlassian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atlassian.migration.datacenter.core.db;

import com.atlassian.migration.datacenter.core.aws.db.DatabaseMigrationService;
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.core.util.MigrationJobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class DatabaseMigrationJobRunner implements MigrationJobRunner
{
    private static Logger log = LoggerFactory.getLogger(DatabaseMigrationJobRunner.class);
    private final DatabaseMigrationService databaseMigrationService;
    public static final String KEY = DatabaseMigrationJobRunner.class.getName();

    public DatabaseMigrationJobRunner(DatabaseMigrationService databaseMigrationService)
    {
        this.databaseMigrationService = databaseMigrationService;
    }

    @Override
    public String getKey()
    {
        return KEY;
    }

    @Nullable
    @Override
    public JobRunnerResponse runJob(JobRunnerRequest request)
    {
        if (databaseMigrationService.getStatus().isRunning()) {
            return JobRunnerResponse.aborted("Database migration job is already running");
        }

        log.info("Starting database migration job");
        try {
            databaseMigrationService.performMigration();
        } catch (InvalidMigrationStageError e) {
            log.error("Invalid migration transition - {}", e.getMessage());
            return JobRunnerResponse.failed(e);
        }

        log.info("Finished S3 migration job: {}", databaseMigrationService.getStatus());

        return JobRunnerResponse.success("Database migration complete");

    }
}
