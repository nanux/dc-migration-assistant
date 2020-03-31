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

package com.atlassian.migration.datacenter.core.aws.db.restore;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.migration.datacenter.core.util.EncryptionManager;
import com.atlassian.migration.datacenter.dto.MigrationContext;
import com.atlassian.migration.datacenter.spi.MigrationService;
import org.apache.commons.lang3.tuple.Pair;

import static java.util.Objects.requireNonNull;

/**
 * Service for managing credentials for the target database in the migrated environment.
 * The migration should store the target database password with this service. It will be stored securely.
 * The database username is managed as it is constant in Quick Start environments.
 */
public class TargetDbCredentialsStorageService {

    private static final String APPLICATION_DB_USER = System.getProperty("com.atlassian.migration.db.target.applicationUsername", "atljira");

    private final MigrationService migrationService;
    private final ActiveObjects ao;
    private final EncryptionManager encryptionManager;

    public TargetDbCredentialsStorageService(MigrationService migrationService, ActiveObjects ao, EncryptionManager encryptionManager) {
        this.migrationService = migrationService;
        this.ao = ao;
        this.encryptionManager = encryptionManager;
    }

    /**
     * Stores the given database password in the {@link com.atlassian.migration.datacenter.dto.MigrationContext}
     * to be used later to restore the database. The password will be encrypted before storage.
     * @param password the database password
     * @throws NullPointerException if the password is null
     */
    public void storeCredentials(String password) {
        requireNonNull(password);

        MigrationContext context = getMigrationContext();

        context.setTargetDbPasswordEncrypted(encryptionManager.encryptString(password));
    }

    /**
     * @return a pair where the left element is the target database username and the right element is the target database password
     */
    public Pair<String, String> getCredentials() {
        MigrationContext context = getMigrationContext();
        String decryptedPassword = encryptionManager.decryptString(context.getTargetDbPasswordEncrypted());
        return Pair.of(APPLICATION_DB_USER, decryptedPassword);
    }

    // TODO: put this behind the migration service
    private MigrationContext getMigrationContext() {
        MigrationContext[] migrationContexts = ao.find(MigrationContext.class);
        if (migrationContexts.length == 0) {
            migrationService.error();
            throw new RuntimeException("No migration context exists, are you really in a migration?");
        }
        return migrationContexts[0];
    }

}
