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

import com.atlassian.migration.datacenter.core.application.DatabaseConfiguration.DBType
import com.atlassian.migration.datacenter.core.exceptions.ConfigurationReadException
import com.atlassian.migration.datacenter.core.exceptions.UnsupportedPasswordEncodingException
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import java.net.URI
import java.util.Base64
import java.util.stream.Stream
import org.apache.commons.lang3.StringUtils

@JacksonXmlRootElement(localName = "jira-database-config")
class DatabaseConfigurationXmlElement {
    @JacksonXmlProperty(localName = "jdbc-datasource")
    private val dbConfigXmlElement: DbConfigXmlElement? = null

    fun toDatabaseConfiguration(): DatabaseConfiguration {
        val urlString = dbConfigXmlElement!!.getUrl()
        val userName = dbConfigXmlElement.getUserName()
        val password = dbConfigXmlElement.getPassword()
        validateRequiredValues(urlString!!, userName!!, password!!)
        val absURI = URI.create(urlString)
        val dbURI = URI.create(absURI.schemeSpecificPart)
        val type = DBType.valueOf(dbURI.scheme.toUpperCase())
        val host = dbURI.host
        var port = dbURI.port
        if (port == -1) port = 5432
        // TODO: handle connection param '?;
        val name = dbURI.path.substring(1) // Remove leading '/'
        return DatabaseConfiguration(type, host, port, name, userName, password)
    }

    @Throws(ConfigurationReadException::class)
    private fun validateRequiredValues(vararg values: String) {
        val allValuesValid = Stream.of(*values).allMatch { cs: String? -> StringUtils.isNotBlank(cs) }
        if (!allValuesValid) {
            throw ConfigurationReadException("Database configuration file has invalid values")
        }
    }
}

internal class DbConfigXmlElement {
    private val url: String? = null

    @JacksonXmlProperty(localName = "username")
    private val userName: String? = null

    @JacksonXmlProperty(localName = "atlassian-password-cipher-provider")
    private val cipher: String? = null
    private val password: String? = null
    fun getUrl(): String? {
        return url
    }

    fun getUserName(): String? {
        return userName
    }

    @Throws(UnsupportedPasswordEncodingException::class)
    fun getPassword(): String? {
        if (cipher != null) {
            if (cipher != BASE64_CLASS) {
                throw UnsupportedPasswordEncodingException("Unsupported database password encryption in dbconfig.xml; see documentation for detail: $cipher")
            }
            return String(Base64.getDecoder().decode(password))
        }
        return password
    }

    companion object {
        private const val BASE64_CLASS = "com.atlassian.db.config.password.ciphers.base64.Base64Cipher"
    }
}