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
package com.atlassian.migration.datacenter.core.fs.download.s3sync

import com.atlassian.migration.datacenter.core.aws.ssm.SSMApi
import com.atlassian.migration.datacenter.core.aws.ssm.SuccessfulSSMCommandConsumer
import com.atlassian.migration.datacenter.core.aws.ssm.SuccessfulSSMCommandConsumer.SSMCommandInvocationProcessingError
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse

class UnmarshalS3SyncStatusSSMCommandConsumer(ssmApi: SSMApi, commandId: String?, instanceId: String?) :
    SuccessfulSSMCommandConsumer<S3SyncCommandStatus>(ssmApi, commandId, instanceId) {
    @Throws(SSMCommandInvocationProcessingError::class)
    override fun handleSuccessfulCommand(commandInvocation: GetCommandInvocationResponse?): S3SyncCommandStatus {
        val mapper = ObjectMapper()
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
        return try {
            mapper.readValue(commandInvocation!!.standardOutputContent(), S3SyncCommandStatus::class.java)
        } catch (e: JsonProcessingException) {
            logger.error("unable to unmarshal output from s3 sync status command", e)
            throw SSMCommandInvocationProcessingError("unable to read status of sync command", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UnmarshalS3SyncStatusSSMCommandConsumer::class.java)
    }
}