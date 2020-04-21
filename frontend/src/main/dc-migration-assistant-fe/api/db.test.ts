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

import { DBMigrationStatus, toUIStatus } from './db';

describe('DB', () => {
    it('Backend string to status map', () => {
        let status = toUIStatus("NOT_STARTED");

        expect(status).toBe(DBMigrationStatus.NOT_STARTED);
    });

    it('Backend string to status map FAIL', () => {
        let status = toUIStatus("NOT_A_VALID_STATUS");

        expect(status).toBe(DBMigrationStatus.UNKNOWN);
    });

});
