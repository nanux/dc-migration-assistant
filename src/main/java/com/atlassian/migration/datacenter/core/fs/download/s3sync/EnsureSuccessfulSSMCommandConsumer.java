package com.atlassian.migration.datacenter.core.fs.download.s3sync;

import com.atlassian.migration.datacenter.core.aws.ssm.SSMApi;
import com.atlassian.migration.datacenter.core.aws.ssm.SuccessfulSSMCommandConsumer;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;

public class EnsureSuccessfulSSMCommandConsumer extends SuccessfulSSMCommandConsumer<Void> {
    protected EnsureSuccessfulSSMCommandConsumer(SSMApi ssmApi, String commandId, String instanceId) {
        super(ssmApi, commandId, instanceId);
    }

    @Override
    protected Void handleSuccessfulCommand(GetCommandInvocationResponse commandInvocation) throws SSMCommandInvocationProcessingError {
        return null;
    }
}
