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

import React, { FunctionComponent, useState, useEffect, ReactElement, ReactNode } from 'react';
import ProgressBar, { SuccessProgressBar } from '@atlaskit/progress-bar';
import SectionMessage from '@atlaskit/section-message';
import styled from 'styled-components';
import { Button } from '@atlaskit/button/dist/cjs/components/Button';
import { Link } from 'react-router-dom';
import moment, { Moment } from 'moment';
import Spinner from '@atlaskit/spinner';

import { I18n } from '@atlassian/wrm-react-i18n';
import { overviewPath } from '../../utils/RoutePaths';

const POLL_INTERVAL_MILLIS = 3000;

/**
 * **boldPrefix** - text that will be at the beginning of the message in bold. This should be used
 * to communicate *how much* data has been migrated
 *
 * **message** - the remainder of the complete text
 */
export type CompleteMessage = {
    boldPrefix: string;
    message: string;
};

export type Progress = {
    phase: string;
    completeness?: number;
    error?: string;
    completeMessage?: CompleteMessage;
};

export interface ProgressCallback {
    (): Promise<Progress>;
}

export type MigrationTransferProps = {
    heading: string;
    description: string;
    nextText: string;
    startMoment: moment.Moment;
    hasStarted: boolean;
    startMigrationPhase: () => Promise<void>;
    getProgress: ProgressCallback;
};

const TransferPageContainer = styled.div`
    display: flex;
    flex-direction: column;
    width: 100%;
    margin-right: auto;
    margin-bottom: auto;
    padding-left: 15px;
`;

const TransferContentContainer = styled.div`
    display: flex;
    flex-direction: column;
    padding-right: 30px;

    padding-bottom: 5px;
`;

const TransferActionsContainer = styled.div`
    display: flex;
    flex-direction: row;
    justify-content: flex-start;

    margin-top: 20px;
`;

const renderContentIfLoading = (
    loading: boolean,
    progress: Progress,
    started: Moment
): ReactElement => {
    if (loading) {
        return (
            <>
                <Spinner />
                <ProgressBar isIndeterminate />
                <Spinner />
            </>
        );
    }
    const elapsedTime = moment.duration(moment.now() - started.valueOf());
    const elapsedDays = elapsedTime.days();
    const elapsedHours = elapsedTime.hours();
    const elapsedMins = elapsedTime.minutes();
    return (
        <>
            <h4>
                {progress.phase}
                {progress.completeness === undefined &&
                    ` (${I18n.getText('atlassian.migration.datacenter.common.estimating')}...)`}
            </h4>
            {progress.completeness ? (
                <SuccessProgressBar value={progress.completeness} />
            ) : (
                <ProgressBar isIndeterminate />
            )}
            <p>
                {I18n.getText(
                    'atlassian.migration.datacenter.common.progress.started',
                    started.format('D/MMM/YY h:m A')
                )}
            </p>
            <p>
                {I18n.getText(
                    'atlassian.migration.datacenter.common.progress.mins_elapsed',
                    `${elapsedDays * 24 + elapsedHours}`,
                    `${elapsedMins}`
                )}
            </p>
        </>
    );
};

const renderMigrationProgress = (
    transferError: string,
    progress: Progress,
    loading: boolean,
    startedMoment: Moment
): ReactNode => {
    return (
        <>
            {transferError && <SectionMessage appearance="error">{transferError}</SectionMessage>}
            {progress?.completeness === 1 && (
                <SectionMessage appearance="confirmation">
                    <strong>{progress.completeMessage.boldPrefix}</strong>{' '}
                    {progress.completeMessage.message}
                </SectionMessage>
            )}
            {renderContentIfLoading(loading, progress, startedMoment)}
        </>
    );
};

const renderMigrationActions = (
    completeness: number,
    nextText: string,
    startMigrationPhase: () => Promise<void>,
    updateProgress: () => Promise<void>,
    started: boolean,
    loading: boolean
): ReactNode => {
    const defaultButtonStyle = {
        padding: '5px',
    };
    const marginButtonStyle = {
        ...defaultButtonStyle,
        marginRight: '20px',
    };

    if (completeness === 1) {
        return (
            <Button style={defaultButtonStyle} appearance="primary">
                {nextText}
            </Button>
        );
    }
    if (started) {
        return (
            <Button style={marginButtonStyle} isLoading={loading} onClick={updateProgress}>
                Refresh
            </Button>
        );
    }
    return (
        <Button
            style={marginButtonStyle}
            isLoading={loading}
            appearance="primary"
            onClick={startMigrationPhase}
        >
            Start
        </Button>
    );
};

export const MigrationTransferPage: FunctionComponent<MigrationTransferProps> = ({
    description,
    heading,
    nextText,
    startMoment,
    getProgress,
    startMigrationPhase,
    hasStarted,
}) => {
    const [progress, setProgress] = useState<Progress>();
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string>();
    const [started, setStarted] = useState<boolean>(hasStarted);

    const updateProgress = (): Promise<void> => {
        return getProgress()
            .then(result => {
                setProgress(result);
                setLoading(false);
            })
            .catch(err => {
                console.error(err);
                setError(err);
                setLoading(false);
            });
    };

    const startMigration = (): Promise<void> => {
        setLoading(true);
        return startMigrationPhase().then(() => {
            setStarted(true);
        });
    };

    useEffect(() => {
        if (started) {
            const id = setInterval(async () => {
                await updateProgress();
            }, POLL_INTERVAL_MILLIS);

            setLoading(true);
            updateProgress();

            return (): void => clearInterval(id);
        }
        return (): void => undefined;
    }, [started]);

    const transferError = progress?.error || error;

    return (
        <TransferPageContainer>
            <TransferContentContainer>
                <h1>{heading}</h1>
                <p>{description}</p>
                {started && renderMigrationProgress(transferError, progress, loading, startMoment)}
            </TransferContentContainer>
            <TransferActionsContainer>
                {renderMigrationActions(
                    progress?.completeness,
                    nextText,
                    startMigration,
                    updateProgress,
                    started,
                    loading
                )}
                <Link to={overviewPath}>
                    <Button style={{ marginLeft: '20px', paddingLeft: '5px' }}>
                        {I18n.getText('atlassian.migration.datacenter.generic.cancel')}
                    </Button>
                </Link>
            </TransferActionsContainer>
        </TransferPageContainer>
    );
};
