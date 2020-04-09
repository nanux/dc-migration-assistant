import React, { FunctionComponent } from 'react';
import ProgressBar, { SuccessProgressBar } from '@atlaskit/progress-bar';
import SectionMessage from '@atlaskit/section-message';
import styled from 'styled-components';
import { Button } from '@atlaskit/button/dist/cjs/components/Button';
import { Link } from 'react-router-dom';
import { overviewPath } from '../../utils/RoutePaths';

const TransferPageContainer = styled.div`
    display: flex;
    flex-direction: column;
    width: 25%;
    margin-right: auto;
    margin-left: auto;
    margin-bottom: auto;
`;

const TransferContentContainer = styled.div`
    display: flex;
    flex-direction: column;

    padding-bottom: 20px;
    border-bottom: 2px solid gray;
`;

const TransferActionsContainer = styled.div`
    display: flex;
    flex-direction: row;
    justify-content: space-between;

    margin-top: 20px;
`;

export const MigrationTransferPage: FunctionComponent<{}> = ({ children }) => {
    return (
        <TransferPageContainer>
            <TransferContentContainer>
                <h1>Copy Over files</h1>
                <p>Text describing the step</p>
                <SectionMessage
                    title="Copying over files takes ~ time depending on the size of your instance"
                    actions={[
                        {
                            key: 'learn',
                            href: 'https://en.wikipedia.org/wiki/Mary_Shelley',
                            text: 'Learn more',
                        },
                    ]}
                >
                    You can close this window and get back to it at any time. We recommend to start
                    planning simulatde downtime at this point of time i.e. informing your users etc.
                </SectionMessage>
                <h4>Phase of copying</h4>
                <ProgressBar isIndeterminate />
                <p>Started today</p>
                <p>10 hours, 15 minutes elapsed</p>
                <p>45 000 files copied</p>
            </TransferContentContainer>
            <TransferActionsContainer>
                <Link to={overviewPath}>
                    <Button>Cancel migration</Button>
                </Link>
                <Button appearance="primary">Start downtime</Button>
            </TransferActionsContainer>
        </TransferPageContainer>
    );
};
