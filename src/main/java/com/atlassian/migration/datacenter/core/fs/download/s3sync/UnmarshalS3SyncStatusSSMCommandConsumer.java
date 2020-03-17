package com.atlassian.migration.datacenter.core.fs.download.s3sync;

import com.atlassian.migration.datacenter.core.aws.ssm.SSMApi;
import com.atlassian.migration.datacenter.core.aws.ssm.SuccessfulSSMCommandConsumer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;

public class UnmarshalS3SyncStatusSSMCommandConsumer extends SuccessfulSSMCommandConsumer<S3SyncCommandStatus> {
    private static Logger logger = LoggerFactory.getLogger(UnmarshalS3SyncStatusSSMCommandConsumer.class);

    protected UnmarshalS3SyncStatusSSMCommandConsumer(SSMApi ssmApi, String commandId, String instanceId) {
        super(ssmApi, commandId, instanceId);
    }

    @Override
    protected S3SyncCommandStatus handleSuccessfulCommand(GetCommandInvocationResponse commandInvocation) throws SSMCommandInvocationProcessingError {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        try {
            return mapper.readValue(commandInvocation.standardOutputContent(), S3SyncCommandStatus.class);
        } catch (JsonProcessingException e) {
            logger.error("unable to unmarshal output from s3 sync status command", e);
            throw new SSMCommandInvocationProcessingError("unable to read status of sync command", e);
        }
    }
}
