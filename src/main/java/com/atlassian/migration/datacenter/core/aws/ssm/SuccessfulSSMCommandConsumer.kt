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
package com.atlassian.migration.datacenter.core.aws.ssm

import com.atlassian.migration.datacenter.core.aws.ssm.SuccessfulSSMCommandConsumer.SSMCommandInvocationProcessingError
import com.atlassian.migration.datacenter.core.aws.ssm.SuccessfulSSMCommandConsumer.UnsuccessfulSSMCommandInvocationException
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.ssm.model.CommandInvocationStatus
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse

abstract class SuccessfulSSMCommandConsumer<T> protected constructor(private val ssmApi: SSMApi, private val commandId: String?, private val instanceId: String?) {
    @Throws(UnsuccessfulSSMCommandInvocationException::class, SSMCommandInvocationProcessingError::class)
    fun handleCommandOutput(maxCommandStatusRetries: Int): T {
        var command: GetCommandInvocationResponse? = null
        for (i in 0 until maxCommandStatusRetries) {
            command = ssmApi.getSSMCommand(commandId, instanceId)
            val status = command.status()
            logger.debug("Checking delivery of s3 sync ssm command. Attempt {}. Status is: {}", i, status.toString())
            if (status == CommandInvocationStatus.SUCCESS) {
                return handleSuccessfulCommand(command)
            }
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                logger.error("interrupted while waiting for s3 sync ssm command to be delivered", e)
                throw UnsuccessfulSSMCommandInvocationException("Interrupted while waiting to check command status", e)
            }
        }
        throw UnsuccessfulSSMCommandInvocationException("Command never completed successfully. Latest status is: " + command!!.status().toString())
    }

    @Throws(SSMCommandInvocationProcessingError::class)
    protected abstract fun handleSuccessfulCommand(commandInvocation: GetCommandInvocationResponse?): T

    class UnsuccessfulSSMCommandInvocationException : Exception {
        constructor(message: String?) : super(message) {}
        constructor(message: String?, cause: Throwable?) : super(message, cause) {}
    }

    class SSMCommandInvocationProcessingError : Exception {
        constructor(message: String?) : super(message) {}
        constructor(message: String?, cause: Throwable?) : super(message, cause) {}
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SuccessfulSSMCommandConsumer::class.java)
    }

}