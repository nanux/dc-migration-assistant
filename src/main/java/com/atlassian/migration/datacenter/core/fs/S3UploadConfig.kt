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
package com.atlassian.migration.datacenter.core.fs

import software.amazon.awssdk.services.s3.S3AsyncClient
import java.nio.file.Path

/**
 * Contains configuration for S3 upload calls
 */
class S3UploadConfig(private val bucketName: String, private val s3AsyncClient: S3AsyncClient, private val sharedHome: Path) {
    /**
     * Destination S3 bucket name where to upload files
     *
     * @return bucket name
     */
    fun getBucketName(): String {
        return bucketName
    }

    /**
     * Asynchronous S3 client to call AWS API
     *
     * @return async S3 client
     */
    fun getS3AsyncClient(): S3AsyncClient {
        return s3AsyncClient
    }

    /**
     * Application shared home. In case of Server distribution, it should return the regular home.
     *
     * @return application home (shared for DC, normal for Server distribution)
     */
    fun getSharedHome(): Path {
        return sharedHome
    }

}