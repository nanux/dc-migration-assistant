package com.atlassian.migration.datacenter.core.aws.ssm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ssm.model.CommandInvocationStatus;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;

public abstract class SuccessfulSSMCommandConsumer<T> {

    private static final Logger logger = LoggerFactory.getLogger(SuccessfulSSMCommandConsumer.class);


    private final SSMApi ssmApi;
    private final String commandId;
    private final String instanceId;

    protected SuccessfulSSMCommandConsumer(SSMApi ssmApi, String commandId, String instanceId) {
        this.ssmApi = ssmApi;
        this.commandId = commandId;
        this.instanceId = instanceId;
    }

    public T handleCommandOutput(int maxCommandStatusRetries) throws UnsuccessfulSSMCommandInvocationException, SSMCommandInvocationProcessingError {
        GetCommandInvocationResponse command = null;
        for (int i = 0; i < maxCommandStatusRetries; i++) {
            command = ssmApi.getSSMCommand(commandId, instanceId);
            final CommandInvocationStatus status = command.status();

            logger.debug("Checking delivery of s3 sync ssm command. Attempt {}. Status is: {}", i, status.toString());

            if (status.equals(CommandInvocationStatus.SUCCESS)) {
                return handleSuccessfulCommand(command);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("interrupted while waiting for s3 sync ssm command to be delivered", e);
                throw new UnsuccessfulSSMCommandInvocationException("Interrupted while waiting to check command status", e);
            }
        }
        throw new UnsuccessfulSSMCommandInvocationException("Command never completed successfully. Latest status is: " + command.status().toString());
    }

    protected abstract T handleSuccessfulCommand(GetCommandInvocationResponse commandInvocation) throws SSMCommandInvocationProcessingError;

    public static class UnsuccessfulSSMCommandInvocationException extends Exception {
        public UnsuccessfulSSMCommandInvocationException(String message) {
            super(message);
        }

        public UnsuccessfulSSMCommandInvocationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SSMCommandInvocationProcessingError extends Exception {
        public SSMCommandInvocationProcessingError(String message) {
            super(message);
        }

        public SSMCommandInvocationProcessingError(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
