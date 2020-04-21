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

const path = require('path');

const paths = {
    mocksRoot: path.resolve('./src/main/dc-migration-assistant-fe/atlassian/mocks'),
    testsRoot: path.resolve('./src/main/dc-migration-assistant-fe'),
};

module.exports = {
    clearMocks: true,
    verbose: false,
    roots: [paths.testsRoot],
    moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json'],
    transformIgnorePatterns: ['node_modules/(?!(@atlaskit)/)'],
    moduleNameMapper: {
        '^wrm/context-path$': path.join(paths.mocksRoot, './wrm/context-path.ts'),
        '^wrm/format$': path.join(paths.mocksRoot, './wrm/format.ts'),
        '^@atlassian/wrm-react-i18n$': path.join(paths.mocksRoot, './@atlassian/wrm-react-i18n.ts'),
        '\\.(css|less)$': 'identity-obj-proxy',
    },
    testMatch: ['**/__tests__/**/*.+(ts|tsx|js)', '**/?(*.)+(spec|test).+(ts|tsx|js)'],
    transform: {
        '^.+\\.(ts|tsx)$': 'ts-jest',
    },
    testEnvironment: 'jest-environment-jsdom-sixteen',
};
