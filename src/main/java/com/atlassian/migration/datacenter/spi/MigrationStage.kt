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

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents all possible states of an on-premise to cloud migration.
 */
enum class MigrationStage(@field:JsonProperty private val key: String) {
    ERROR("error"), FINISHED("finished"), CUTOVER("cutover"), VALIDATE("validate"), DB_MIGRATION_IMPORT("db_migration_down"), DB_MIGRATION_EXPORT("db_migration_up"), OFFLINE_WARNING("cutover_warning"), WAIT_FS_MIGRATION_COPY("wait_fs_migration_copy"), FS_MIGRATION_COPY("fs_migration_copy"), WAIT_PROVISION_MIGRATION_STACK("wait_provision_migration"), PROVISION_MIGRATION_STACK("provision_migration"), WAIT_PROVISION_APPLICATION("wait_provision_app"), PROVISION_APPLICATION("provision_app"), AUTHENTICATION("authentication"), NOT_STARTED("");

    override fun toString(): String {
        return key
    }

}