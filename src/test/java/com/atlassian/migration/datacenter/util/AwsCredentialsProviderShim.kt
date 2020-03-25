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
package com.atlassian.migration.datacenter.util

import com.amazonaws.auth.AWSCredentialsProvider
import com.atlassian.migration.datacenter.core.aws.auth.ReadCredentialsService
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider

class AwsCredentialsProviderShim(private val v1Creds: AWSCredentialsProvider) : AwsCredentialsProvider, AwsCredentials, ReadCredentialsService {
    override fun resolveCredentials(): AwsCredentials {
        return this
    }

    override fun accessKeyId(): String {
        return v1Creds.credentials.awsAccessKeyId
    }

    override fun secretAccessKey(): String {
        return v1Creds.credentials.awsSecretKey
    }

    override fun getAccessKeyId(): String? {
        return v1Creds.credentials.awsAccessKeyId
    }

    override fun getSecretAccessKey(): String? {
        return v1Creds.credentials.awsSecretKey
    }

}