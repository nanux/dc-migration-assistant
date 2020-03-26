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
package com.atlassian.migration.datacenter.spi

import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError
import com.atlassian.migration.datacenter.core.exceptions.MigrationAlreadyExistsException
import com.atlassian.migration.datacenter.dto.Migration

/**
 * Manages the lifecycle of the migration
 */
interface MigrationService {
    /**
     * Creates a new migration in the initial stage. Using this method will create just one migration object in the database
     * **or** find the existing migration object and return it.
     *
     * @throws [MigrationAlreadyExistsException] when a migration object already exists.
     */
    @Throws(MigrationAlreadyExistsException::class)
    fun createMigration(): Migration

    /**
     * Gets the current stage of the migration
     */
    val currentStage: MigrationStage

    /**
     * Gets the Migration Object that can only be read. Setter invocation must to happen through the [MigrationService] interface
     *
     * @return a read-only migration object.
     */
    val currentMigration: Migration

    /**
     * Tries to transition the migration state from one to another
     *
     * @param from the state you are expected to be in currently when beginning the transition
     * @param to the state you want to transition to
     * @throws InvalidMigrationStageError when the transition is invalid
     */
    @Throws(InvalidMigrationStageError::class)
    fun transition(from: MigrationStage, to: MigrationStage)

    /**
     * Moves the migration into an error stage
     *
     * @see MigrationStage.ERROR
     */
    fun error()
}