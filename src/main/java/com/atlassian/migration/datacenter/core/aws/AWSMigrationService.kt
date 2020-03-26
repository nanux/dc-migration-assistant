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
package com.atlassian.migration.datacenter.core.aws

import com.atlassian.activeobjects.external.ActiveObjects
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError.Companion.errorWithMessage
import com.atlassian.migration.datacenter.core.exceptions.MigrationAlreadyExistsException
import com.atlassian.migration.datacenter.dto.Migration
import com.atlassian.migration.datacenter.dto.MigrationContext
import com.atlassian.migration.datacenter.spi.MigrationService
import com.atlassian.migration.datacenter.spi.MigrationStage
import org.slf4j.LoggerFactory

/**
 * Manages a migration from on-premise to self-hosted AWS.
 */
class AWSMigrationService(val ao: ActiveObjects) : MigrationService {
    @Throws(MigrationAlreadyExistsException::class)
    override fun createMigration(): Migration {
        val migration = findFirstOrCreateMigration()
        if (migration.stage == MigrationStage.NOT_STARTED) {
            return migration
        }
        throw MigrationAlreadyExistsException(
            String.format(
                "Found existing migration in Stage - `%s`",
                migration.stage
            )
        )
    }

    override val currentStage: MigrationStage = findFirstOrCreateMigration().stage

    override val currentMigration: Migration = findFirstOrCreateMigration()

    @Throws(InvalidMigrationStageError::class)
    override fun transition(from: MigrationStage, to: MigrationStage) {
        val migration = findFirstOrCreateMigration()
        val currentStage = migration.stage
        if (currentStage != from) {
            throw errorWithMessage(from, currentStage)
        }
        setCurrentStage(migration, to)
    }

    override fun error() {
        val migration = findFirstOrCreateMigration()
        setCurrentStage(migration, MigrationStage.ERROR)
    }

    private fun setCurrentStage(migration: Migration, stage: MigrationStage) {
        migration.stage = stage
        migration.save()
    }

    private fun findFirstOrCreateMigration(): Migration {
        val migrations = ao.find(Migration::class.java)
        return when {
            migrations.size == 1 -> { // In case we have interrupted migration (e.g. the node went down), we want to pick up where we've left off.
                migrations[0]
            }
            migrations.isEmpty() -> { // We didn't start the migration, so we need to create record in the db and a migration context
                val migration = ao.create(Migration::class.java)
                migration.stage = MigrationStage.NOT_STARTED
                migration.save()
                val context = ao.create(MigrationContext::class.java)
                context.migration = migration
                context.save()
                migration
            }
            else -> {
                log.error("Expected one Migration, found multiple.")
                throw RuntimeException("Invalid State - should only be 1 migration")
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AWSMigrationService::class.java)
    }
}