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

package com.atlassian.migration.datacenter.core;

import com.atlassian.migration.datacenter.core.aws.db.DatabaseArchivalService;
import com.atlassian.migration.datacenter.core.aws.db.DatabaseArchiveStageTransitionCallback;
import com.atlassian.migration.datacenter.core.aws.db.DatabaseArtifactS3UploadService;
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.spi.MigrationStage;

public class MigrationEngine
{
    private final DatabaseArchivalService databaseArchivalService;
    private final DatabaseArchiveStageTransitionCallback stageTransitionCallback;
    private final DatabaseArtifactS3UploadService s3UploadService;

    private MigrationStage stage = MigrationStage.NOT_STARTED;

    public MigrationEngine(DatabaseArchivalService databaseArchivalService,
                           DatabaseArchiveStageTransitionCallback stageTransitionCallback,
                           DatabaseArtifactS3UploadService s3UploadService)
    {
        this.databaseArchivalService = databaseArchivalService;
        this.stageTransitionCallback = stageTransitionCallback;
        this.s3UploadService = s3UploadService;
    }

    public void startDatabaseMigration() throws InvalidMigrationStageError
    {
        transition(MigrationStage.DB_MIGRATION_EXPORT);
    }

    private synchronized void transition(MigrationStage to) throws InvalidMigrationStageError
    {
        if (!stage.isValidTransition(to)) {
            throw InvalidMigrationStageError.errorWithMessage(stage, to);
        }
        stage = to;
    }

    public MigrationStage getStage()
    {
        return stage;
    }
}
