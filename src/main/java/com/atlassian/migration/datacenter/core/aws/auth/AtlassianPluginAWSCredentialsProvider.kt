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
package com.atlassian.migration.datacenter.core.aws.auth

import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider

class AtlassianPluginAWSCredentialsProvider(private val readCredentialsService: ReadCredentialsService) :
    AwsCredentialsProvider {
    private fun secretKeyIsDefined(): Boolean {
        return readCredentialsService.getSecretAccessKey() != ""
    }

    private fun accessKeyIsDefined(): Boolean {
        return readCredentialsService.getAccessKeyId() != ""
    }

    /**
     * AWS SDK V2 credentials API
     *
     * @return AWS Credentials to be used with SDK V2 clients
     */
    override fun resolveCredentials(): AwsCredentials {
        return if (accessKeyIsDefined() && secretKeyIsDefined()) {
            object : AwsCredentials {
                override fun accessKeyId(): String {
                    return readCredentialsService.getAccessKeyId()!!
                }

                override fun secretAccessKey(): String {
                    return readCredentialsService.getSecretAccessKey()!!
                }
            }
        } else DefaultCredentialsProvider.create().resolveCredentials()
    }
}