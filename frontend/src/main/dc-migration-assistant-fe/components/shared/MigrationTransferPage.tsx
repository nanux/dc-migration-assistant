import React, { FunctionComponent } from 'react';
import ProgressBar, { SuccessProgressBar } from '@atlaskit/progress-bar';
import SectionMessage from '@atlaskit/section-message';

export const MigrationTransferPage: FunctionComponent<{}> = ({ children }) => {
    return (
        <>
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
        </>
    );
};
