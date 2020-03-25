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
package com.atlassian.migration.datacenter.core.application

class DatabaseConfiguration(private val type: DBType, private val host: String, private val port: Int, private val name: String, private val username: String?, private val password: String?) {
    enum class DBType {
        POSTGRESQL, MYSQL, SQLSERVER, ORACLE
    }

    fun getHost(): String {
        return host
    }

    fun getName(): String {
        return name
    }

    fun getUsername(): String? {
        return username
    }

    fun getPassword(): String? {
        return password
    }

    fun getPort(): Int {
        return port
    }

    fun getType(): DBType {
        return type
    }

}