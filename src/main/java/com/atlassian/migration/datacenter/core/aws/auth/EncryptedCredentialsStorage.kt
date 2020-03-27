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
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import javax.annotation.PostConstruct
import org.apache.log4j.Logger
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.security.crypto.encrypt.TextEncryptor
import org.springframework.security.crypto.keygen.KeyGenerators

/**
 * Class for managing the storage and retrieval of AWS Credentials. Should not be used for direct access to credentials
 * except for in a CredentialsProvider implementation. This class stores credentials encrypted with a key generated by
 * the Spring Security Crypto library using its default AES encryption.
 */
class EncryptedCredentialsStorage(
    private val pluginSettingsFactorySupplier: Supplier<PluginSettingsFactory>,
    private val jiraHome: JiraHome?
) : ReadCredentialsService, WriteCredentialsService {
    var textEncryptor: TextEncryptor
    lateinit var pluginSettings: PluginSettings

    @PostConstruct // FIXME: I do not work
    fun postConstruct() {
        pluginSettings = pluginSettingsFactorySupplier.get().createGlobalSettings()
        assert(jiraHome != null)
        val keyFilePath = jiraHome!!.home.path + "/" + ENCRYPTION_KEY_FILE_NAME
        val saltFilePath = jiraHome.home.path + "/" + ENCRYPTION_SALT_FILE_NAME
        val password = getEncryptionData(keyFilePath)
        val salt = getEncryptionData(saltFilePath)
        textEncryptor = Encryptors.text(password, salt)
    }

    override fun getAccessKeyId(): String? { // FIXME: Need to find a way to inject without calling the supplier every time
        val pluginSettings = pluginSettingsFactorySupplier.get().createGlobalSettings()
        val raw = pluginSettings[AWS_CREDS_PLUGIN_STORAGE_KEY + ACCESS_KEY_ID_PLUGIN_STORAGE_SUFFIX] as String
        return decryptString(raw)
    }

    fun setAccessKeyId(accessKeyId: String) { // FIXME: Need to find a way to inject without calling the supplier every time
        val pluginSettings = pluginSettingsFactorySupplier.get().createGlobalSettings()
        pluginSettings.put(
            AWS_CREDS_PLUGIN_STORAGE_KEY + ACCESS_KEY_ID_PLUGIN_STORAGE_SUFFIX,
            encryptString(accessKeyId)
        )
    }

    override fun storeAccessKeyId(accessKeyId: String) {
        setAccessKeyId(accessKeyId)
    }

    override fun storeSecretAccessKey(secretAccessKey: String) {
        setSecretAccessKey(secretAccessKey)
    }

    override fun getSecretAccessKey(): String? { // FIXME: Need to find a way to inject without calling the supplier every time
        val pluginSettings = pluginSettingsFactorySupplier.get().createGlobalSettings()
        val raw = pluginSettings[AWS_CREDS_PLUGIN_STORAGE_KEY + SECRET_ACCESS_KEY_PLUGIN_STORAGE_SUFFIX] as String
        return decryptString(raw)
    }

    fun setSecretAccessKey(secretAccessKey: String) { // FIXME: Need to find a way to inject without calling the supplier every time
        val pluginSettings = pluginSettingsFactorySupplier.get().createGlobalSettings()
        pluginSettings.put(
            AWS_CREDS_PLUGIN_STORAGE_KEY + SECRET_ACCESS_KEY_PLUGIN_STORAGE_SUFFIX,
            encryptString(secretAccessKey)
        )
    }

    /**
     * The string encryption function
     *
     * @param raw the string to be encrypted
     * @return the encrypted string
     */
    private fun encryptString(raw: String): String? {
        return try {
            textEncryptor.encrypt(raw)
        } catch (e: Exception) {
            logger.error(e.localizedMessage)
            null
        }
    }

    /**
     * The string decryption function
     *
     * @param encrypted string to be decrypted
     * @return the decrypted plaintext string
     */
    private fun decryptString(encrypted: String): String? {
        return try {
            textEncryptor.decrypt(encrypted)
        } catch (e: Exception) {
            logger.error(e.localizedMessage)
            null
        }
    }

    companion object {
        private const val AWS_CREDS_PLUGIN_STORAGE_KEY = "com.atlassian.migration.datacenter.core.aws.auth"
        private const val ACCESS_KEY_ID_PLUGIN_STORAGE_SUFFIX = ".accessKeyId"
        private const val SECRET_ACCESS_KEY_PLUGIN_STORAGE_SUFFIX = ".secretAccessKey"
        private const val ENCRYPTION_KEY_FILE_NAME = "keyFile"
        private const val ENCRYPTION_SALT_FILE_NAME = "saltFile"
        private val logger = Logger.getLogger(EncryptedCredentialsStorage::class.java)
        private fun getEncryptionData(fileName: String): String {
            val dataFile = File(fileName)
            return if (dataFile.exists()) {
                readFileData(dataFile)
            } else {
                generateAndWriteKey(dataFile)
            }
        }

        private fun readFileData(sourceFile: File): String {
            val dataBuilder = StringBuilder()
            try {
                Files.lines(Paths.get(sourceFile.path), StandardCharsets.UTF_8)
                    .use { stream -> stream.forEach { str: String? -> dataBuilder.append(str) } }
            } catch (e: IOException) {
                logger.error(e.localizedMessage)
            }
            return dataBuilder.toString()
        }

        private fun generateAndWriteKey(file: File): String {
            val keyString = KeyGenerators.string().generateKey()
            try {
                FileOutputStream(file).use { outputStream -> outputStream.write(keyString.toByteArray()) }
            } catch (ex: IOException) {
                logger.error(ex.localizedMessage)
            }
            file.setWritable(false, true)
            return keyString
        }
    }

    init {
        assert(jiraHome != null)
        val keyFilePath = jiraHome!!.home.path + "/" + ENCRYPTION_KEY_FILE_NAME
        val saltFilePath = jiraHome.home.path + "/" + ENCRYPTION_SALT_FILE_NAME
        val password = getEncryptionData(keyFilePath)
        val salt = getEncryptionData(saltFilePath)
        textEncryptor = Encryptors.text(password, salt)
    }
}