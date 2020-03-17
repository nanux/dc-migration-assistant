package com.atlassian.migration.datacenter.core.aws.auth;

import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.util.concurrent.Supplier;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EncryptedCredentialsStorageTest {

    EncryptedCredentialsStorage encryptedCredentialsStorage;

    @Mock
    static JiraHome jiraHome;

    @Mock
    PluginSettingsFactory pluginSettingsFactory;


    @AfterAll
    static void tearDown() {
        File keyFile = new File(jiraHome.getHome().getAbsolutePath().concat("/").concat("keyFile"));
        File saltFile = new File(jiraHome.getHome().getAbsolutePath().concat("/").concat("saltFile"));
        if (keyFile.exists()) {
            keyFile.delete();
        }
        if (saltFile.exists()) {
            saltFile.delete();
        }
    }

    @BeforeEach
    void setup() {
        encryptedCredentialsStorage = new EncryptedCredentialsStorage(() -> this.pluginSettingsFactory, jiraHome);
        when(jiraHome.getHome()).thenReturn(new File("."));
        when(this.pluginSettingsFactory.createGlobalSettings()).thenReturn(new PluginSettings() {
            Map<String, Object> settings = new HashMap<>();

            @Override
            public Object get(String s) {
                return this.settings.get(s);
            }

            @Override
            public Object put(String s, Object o) {
                return this.settings.put(s, o);
            }

            @Override
            public Object remove(String s) {
                return this.settings.remove(s);
            }
        });
        this.encryptedCredentialsStorage.afterPropertiesSet();
    }

    @Test
    void testEncryption() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final String testString = RandomStringUtils.randomAlphanumeric(new Random().nextInt(50));

        Method encryptMethod = encryptedCredentialsStorage.getClass().getDeclaredMethod("encryptString", String.class);
        encryptMethod.setAccessible(true);

        Method decryptMethod = encryptedCredentialsStorage.getClass().getDeclaredMethod("decryptString", String.class);
        decryptMethod.setAccessible(true);

        String encrypted = (String) encryptMethod.invoke(encryptedCredentialsStorage, testString);
        String decrypted = (String) decryptMethod.invoke(encryptedCredentialsStorage, encrypted);
        assertNotNull(encrypted);
        assertNotNull(decrypted);
        assertEquals(decrypted, testString);
    }

    @Test
    void testSaveAccessKeyId() {
        final String testAccessKeyId = RandomStringUtils.randomAlphanumeric(new Random().nextInt(50));

        this.encryptedCredentialsStorage.setAccessKeyId(testAccessKeyId);

        String retrievedValue = this.encryptedCredentialsStorage.getAccessKeyId();
        assertEquals(testAccessKeyId, retrievedValue);
    }

    @Test
    void testSaveSecretKey() {
        final String testSecretAccessKey = RandomStringUtils.randomAlphanumeric(new Random().nextInt(50));

        this.encryptedCredentialsStorage.setSecretAccessKey(testSecretAccessKey);

        String retrievedValue = this.encryptedCredentialsStorage.getSecretAccessKey();
        assertEquals(testSecretAccessKey, retrievedValue);
    }

}
