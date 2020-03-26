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

import com.atlassian.jira.config.util.JiraHome
import com.atlassian.migration.datacenter.core.exceptions.ConfigurationReadException
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import java.io.IOException
import java.nio.file.Paths
import java.util.Optional

class JiraConfiguration(private val jiraHome: JiraHome) : ApplicationConfiguration {
    private val xmlMapper: XmlMapper = XmlMapper()
    private var databaseConfiguration: Optional<DatabaseConfiguration> = Optional.empty()

    @Throws(ConfigurationReadException::class)
    override fun getDatabaseConfiguration(): DatabaseConfiguration {
        if (!databaseConfiguration.isPresent) {
            databaseConfiguration = Optional.of(parseDatabaseConfigurationFromXmlFile())
        }
        return databaseConfiguration.get()
    }

    @Throws(ConfigurationReadException::class)
    private fun parseDatabaseConfigurationFromXmlFile(): DatabaseConfiguration {
        val databaseConfig = Paths.get(jiraHome.localHomePath).resolve("dbconfig.xml")
        return try {
            val xmlElement = xmlMapper.readValue(databaseConfig.toFile(), DatabaseConfigurationXmlElement::class.java)
            xmlElement.toDatabaseConfiguration()
        } catch (e: IOException) {
            throw ConfigurationReadException("Unable to parse database configuration XML file", e)
        }
    }
}