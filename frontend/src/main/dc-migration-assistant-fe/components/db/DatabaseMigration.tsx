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

import React, { FunctionComponent } from 'react';

import { I18n } from '@atlassian/wrm-react-i18n';
import moment from 'moment';
import { MigrationTransferProps, MigrationTransferPage } from '../shared/MigrationTransferPage';

const props: MigrationTransferProps = {
    heading: I18n.getText('atlassian.migration.datacenter.db.title'),
    description: I18n.getText('atlassian.migration.datacenter.db.description'),
    nextText: I18n.getText('atlassian.migration.datacenter.fs.nextStep'),
    started: moment(),
    getProgress: () => {
        return new Promise(resolve => {
            setTimeout(() => {
                resolve({
                    completeness: 0.5,
                    phase: 'uploading files...',
                });
            }, 500);
        });
    },
};

export const DatabaseTransferPage: FunctionComponent = () => {
    return <MigrationTransferPage {...props} />;
};
