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

package com.atlassian.migration.api.aws

import io.restassured.RestAssured
import io.restassured.RestAssured.basic
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.core.IsIterableContaining.hasItems
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("rest")
class AWSGlobalInfrastructureEndpointRestTest {
    private val username: String = System.getProperty("JIRA_USERNAME", "admin")
    private val password: String = System.getProperty("JIRA_PASSWORD", "admin")

    init {
        RestAssured.authentication = basic(username, password)
        RestAssured.baseURI = System.getProperty("JIRA_BASE_URL", "http://localhost:2990/jira")
        RestAssured.basePath = System.getProperty("JIRA_BASE_PATH", "/rest/dc-migration/1.0")
    }

    @Test
    fun `GET AWS Regions should respond 200 HTTP`() {
        Given {
            param("os_authType", "basic")
        } When {
            get("/aws/global-infrastructure/regions")
        } Then {
            statusCode(200)
            body("$", hasItems("eu-west-1", "ap-southeast-2", "us-east-2"))
            body("size()", greaterThanOrEqualTo(17))
        }
    }
}