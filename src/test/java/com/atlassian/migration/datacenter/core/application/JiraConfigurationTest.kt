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
import com.atlassian.migration.datacenter.core.exceptions.UnsupportedPasswordEncodingException
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class JiraConfigurationTest {
    @Mock
    var jiraHome: JiraHome? = null

    @TempDir
    var tempDir: Path? = null
    var jiraConfiguration: JiraConfiguration? = null

    @BeforeEach
    fun setUp() {
        jiraConfiguration = JiraConfiguration(jiraHome!!)
        Mockito.`when`(jiraHome!!.localHomePath).thenReturn(tempDir.toString())
    }

    @Test
    fun shouldRaiseFileNotFoundExceptionWhenDatabaseFileIsNotFound() {
        val e: Exception =
            Assertions.assertThrows(ConfigurationReadException::class.java) { jiraConfiguration!!.getDatabaseConfiguration() }
        Assertions.assertEquals(FileNotFoundException::class.java, e.cause!!.javaClass)
    }

    @Test
    @Throws(IOException::class)
    fun shouldRaiseConfigurationExceptionWhenDatabaseFileIsNotValid() {
        val file = tempDir!!.resolve("dbconfig.xml")
        Files.write(file, "not-xml".toByteArray())
        Assertions.assertThrows(ConfigurationReadException::class.java) { jiraConfiguration!!.getDatabaseConfiguration() }
    }

    @Test
    @Throws(IOException::class)
    fun shouldRaiseAnExceptionWhenDbconfigFileIsMissingElements() {
        val xml =
            "<jira-database-config><jdbc-datasource><username>jdbc_user</username><password>password</password></jdbc-datasource></jira-database-config>"
        val file = tempDir!!.resolve("dbconfig.xml")
        Files.write(file, xml.toByteArray())
        Assertions.assertThrows(ConfigurationReadException::class.java) { jiraConfiguration!!.getDatabaseConfiguration() }
    }

    @Test
    @Throws(Exception::class)
    fun shouldBeValidWhenConfigurationFileIsComplete() {
        val url = "jdbc:postgresql://dbhost:9876/dbname"
        val xml =
            "<jira-database-config><jdbc-datasource><url>$url</url><username>jdbc_user</username><password>password</password></jdbc-datasource></jira-database-config>"
        val file = tempDir!!.resolve("dbconfig.xml")
        Files.write(file, xml.toByteArray())
        val config = jiraConfiguration!!.getDatabaseConfiguration()
        Assertions.assertEquals("jdbc_user", config.getUsername())
        Assertions.assertEquals("password", config.getPassword())
        Assertions.assertEquals("dbhost", config.getHost())
        Assertions.assertEquals("dbname", config.getName())
        Assertions.assertEquals(9876, config.getPort())
        Assertions.assertEquals(DatabaseConfiguration.DBType.POSTGRESQL, config.getType())
    }

    @Test
    @Throws(Exception::class)
    fun shouldBeValidWhenConfigurationDoesNotContainValueForPort() {
        val url = "jdbc:postgresql://dbhost/dbname"
        val xml =
            "<jira-database-config><jdbc-datasource><url>$url</url><username>jdbc_user</username><password>password</password></jdbc-datasource></jira-database-config>"
        val file = tempDir!!.resolve("dbconfig.xml")
        Files.write(file, xml.toByteArray())
        val config = jiraConfiguration!!.getDatabaseConfiguration()
        Assertions.assertEquals("jdbc_user", config.getUsername())
        Assertions.assertEquals("password", config.getPassword())
        Assertions.assertEquals("dbhost", config.getHost())
        Assertions.assertEquals("dbname", config.getName())
        Assertions.assertEquals(5432, config.getPort())
    }

    @Test
    @Throws(Exception::class)
    fun shouldParseDatabaseConfigWithValidCipher() {
        val url = "jdbc:postgresql://dbhost:9876/dbname"
        val xml = "<jira-database-config><jdbc-datasource>" +
            "<url>" + url + "</url>" +
            "<username>jdbc_user</username>" +
            "<atlassian-password-cipher-provider>com.atlassian.db.config.password.ciphers.base64.Base64Cipher</atlassian-password-cipher-provider>" +
            "<password>cGFzc3dvcmQ=</password>" +
            "</jdbc-datasource></jira-database-config>"
        val file = tempDir!!.resolve("dbconfig.xml")
        Files.write(file, xml.toByteArray())
        val databaseConfiguration = jiraConfiguration!!.getDatabaseConfiguration()
        Assertions.assertNotNull(databaseConfiguration)
        Assertions.assertEquals("jdbc_user", databaseConfiguration.getUsername())
        Assertions.assertEquals("password", databaseConfiguration.getPassword())
        Assertions.assertEquals("dbhost", databaseConfiguration.getHost())
        Assertions.assertEquals("dbname", databaseConfiguration.getName())
    }

    @Test
    @Throws(Exception::class)
    fun shouldNotParseDatabaseConfigWithInvalidCipher() {
        val url = "jdbc:postgresql://dbhost:9876/dbname"
        val xml = "<jira-database-config><jdbc-datasource>" +
            "<url>" + url + "</url>" +
            "<username>jdbc_user</username>" +
            "<atlassian-password-cipher-provider>com.atlassian.db.config.password.ciphers.algorithm.AlgorithmCipher</atlassian-password-cipher-provider>" +
            "<password>cGFzc3dvcmQ=</password>" +
            "</jdbc-datasource></jira-database-config>"
        val file = tempDir!!.resolve("dbconfig.xml")
        Files.write(file, xml.toByteArray())
        Assertions.assertThrows(UnsupportedPasswordEncodingException::class.java) { jiraConfiguration!!.getDatabaseConfiguration() }
    }
}