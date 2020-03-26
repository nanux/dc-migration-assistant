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

import com.atlassian.jira.config.util.JiraHome
import com.atlassian.sal.api.pluginsettings.PluginSettings
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.util.concurrent.Supplier
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.util.HashMap
import java.util.Random
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Assert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class EncryptedCredentialsStorageTest {
    var encryptedCredentialsStorage: EncryptedCredentialsStorage? = null

    @Mock
    lateinit var pluginSettingsFactory: PluginSettingsFactory
    private var pluginSettings: PluginSettings? = null

    @BeforeEach
    fun setup() {
        pluginSettings = object : PluginSettings {
            var settings: MutableMap<String, Any> = HashMap()
            override fun get(s: String): Any {
                return settings[s]!!
            }

            override fun put(s: String, o: Any): Any {
                return settings.put(s, o)!!
            }

            override fun remove(s: String): Any {
                return settings.remove(s)!!
            }
        }
        Mockito.`when`(jiraHome!!.home).thenReturn(File("."))
        Mockito.`when`(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings)
        encryptedCredentialsStorage = EncryptedCredentialsStorage(Supplier { pluginSettingsFactory }, jiraHome)
        encryptedCredentialsStorage!!.postConstruct()
    }

    @Test
    @Throws(NoSuchMethodException::class, InvocationTargetException::class, IllegalAccessException::class)
    fun testEncryption() {
        val testString = RandomStringUtils.randomAlphanumeric(Random().nextInt(50))
        val encryptMethod =
            encryptedCredentialsStorage!!.javaClass.getDeclaredMethod("encryptString", String::class.java)
        encryptMethod.isAccessible = true
        val decryptMethod =
            encryptedCredentialsStorage!!.javaClass.getDeclaredMethod("decryptString", String::class.java)
        decryptMethod.isAccessible = true
        val encrypted = encryptMethod.invoke(encryptedCredentialsStorage, testString) as String
        val decrypted = decryptMethod.invoke(encryptedCredentialsStorage, encrypted) as String
        Assert.assertNotNull(encrypted)
        Assert.assertNotNull(decrypted)
        Assert.assertEquals(decrypted, testString)
    }

    @Test
    fun testSaveAccessKeyId() {
        val testAccessKeyId = RandomStringUtils.randomAlphanumeric(Random().nextInt(50))
        encryptedCredentialsStorage!!.setAccessKeyId(testAccessKeyId)
        val retrievedValue = encryptedCredentialsStorage!!.getAccessKeyId()
        Assert.assertEquals(testAccessKeyId, retrievedValue)
    }

    @Test
    fun testSaveSecretKey() {
        val testSecretAccessKey = RandomStringUtils.randomAlphanumeric(Random().nextInt(50))
        encryptedCredentialsStorage!!.setSecretAccessKey(testSecretAccessKey)
        val retrievedValue = encryptedCredentialsStorage!!.getSecretAccessKey()
        Assert.assertEquals(testSecretAccessKey, retrievedValue)
    }

    companion object {
        @Mock
        var jiraHome: JiraHome? = null

        @AfterAll
        fun tearDown() {
            val keyFile = File(jiraHome!!.home.absolutePath + "/" + "keyFile")
            val saltFile = File(jiraHome!!.home.absolutePath + "/" + "saltFile")
            if (keyFile.exists()) {
                keyFile.delete()
            }
            if (saltFile.exists()) {
                saltFile.delete()
            }
        }
    }
}