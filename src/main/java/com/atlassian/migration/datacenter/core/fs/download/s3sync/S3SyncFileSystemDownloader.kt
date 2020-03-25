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
import com.atlassian.migration.datacenter.core.aws.ssm.SuccessfulSSMCommandConsumer.UnsuccessfulSSMCommandInvocationException
import com.atlassian.migration.datacenter.core.fs.download.s3sync.S3SyncFileSystemDownloader
import org.slf4j.LoggerFactory

class S3SyncFileSystemDownloader internal constructor(private val ssmApi: SSMApi, private val maxCommandStatusRetries: Int) {

    constructor(ssmApi: SSMApi) : this(ssmApi, 10) {}

    @Throws(CannotLaunchCommandException::class)
    fun initiateFileSystemDownload() { // FIXME: Reload the migration stack instance ID in case instance has gone down during migration
        val commandID = ssmApi.runSSMDocument(SSM_PLAYBOOK, MIGRATION_STACK_INSTANCE, emptyMap())
        val consumer: SuccessfulSSMCommandConsumer<*> = EnsureSuccessfulSSMCommandConsumer(ssmApi, commandID, MIGRATION_STACK_INSTANCE)
        try {
            consumer.handleCommandOutput(maxCommandStatusRetries)
        } catch (e: UnsuccessfulSSMCommandInvocationException) {
            logger.error("error launching s3 sync command", e)
            throw CannotLaunchCommandException("unable to launch file system download command successfully.")
        } catch (never: SSMCommandInvocationProcessingError) {
        }
    }

    @Throws(IndeterminateS3SyncStatusException::class)
    fun getFileSystemDownloadStatus(): S3SyncCommandStatus? {
        val statusCommandId = ssmApi.runSSMDocument(STATUS_SSM_PLAYBOOK, MIGRATION_STACK_INSTANCE, emptyMap())
        val consumer: SuccessfulSSMCommandConsumer<S3SyncCommandStatus> = UnmarshalS3SyncStatusSSMCommandConsumer(ssmApi, statusCommandId, MIGRATION_STACK_INSTANCE)
        return try {
            consumer.handleCommandOutput(maxCommandStatusRetries)
        } catch (e: UnsuccessfulSSMCommandInvocationException) {
            logger.error("Status command did not complete successfully", e)
            null
        } catch (e: SSMCommandInvocationProcessingError) {
            logger.error("Unable to read status of s3 sync command", e)
            null
        }
    }

    class IndeterminateS3SyncStatusException internal constructor(message: String?) : Exception(message)
    class CannotLaunchCommandException internal constructor(message: String?) : Exception(message)
    companion object {
        private val logger = LoggerFactory.getLogger(S3SyncFileSystemDownloader::class.java)
        // FIXME: Should be loaded from migration stack. Defaults to a document created by a migration stack in us-east-1
        private val SSM_PLAYBOOK = System.getProperty("com.atlassian.migration.s3sync.documentName", "bpartridge-12-03t15-42-39-migration-helper-SharedHomeDownloadDocument-1C56C88F671YL")
        // FIXME: Should be loaded from migration stack.
        private val STATUS_SSM_PLAYBOOK = System.getProperty("com.atlassian.migration.s3sync.statusDocmentName", "fake-document")
        // FIXME: Should be loaded from migration stack. Defaults to an instance deployed by a migration stack in us-east-1
        private val MIGRATION_STACK_INSTANCE = System.getProperty("com.atlassian.migration.instanceId", "i-0353cc9a8ad7dafc2")
    }

}