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

package com.atlassian.migration.datacenter.core.aws.region

import com.atlassian.migration.datacenter.core.aws.GlobalInfrastructure
import com.atlassian.sal.api.pluginsettings.PluginSettings
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.util.concurrent.Supplier
import java.util.ArrayList
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import software.amazon.awssdk.regions.Region

@ExtendWith(MockitoExtension::class)
internal class PluginSettingsRegionManagerTest {
    lateinit var pluginSettingsRegionManager: PluginSettingsRegionManager

    @Mock
    lateinit var globalInfrastructure: GlobalInfrastructure

    @Mock
    lateinit var pluginSettingsFactory: PluginSettingsFactory

    @Mock
    lateinit var pluginSettings: PluginSettings

    private val pluginSettingsRegionKey =
        PluginSettingsRegionManager.AWS_REGION_PLUGIN_STORAGE_KEY + PluginSettingsRegionManager.REGION_PLUGIN_STORAGE_SUFFIX

    @BeforeEach
    fun setUp() {
        pluginSettingsRegionManager =
            PluginSettingsRegionManager(Supplier { pluginSettingsFactory }, globalInfrastructure)
        Mockito.`when`(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings)
        pluginSettingsRegionManager.postConstruct()
    }

    @Test
    fun shouldGetRegionFromPluginSettingsWhenKeyExists() {
        Mockito.`when`(pluginSettings[pluginSettingsRegionKey]).thenReturn("area-51")
        val region = pluginSettingsRegionManager.getRegion()
        Assertions.assertEquals("area-51", region)
    }

    @Test
    fun shouldDefaultToUsEast1aRegionFromPluginSettingsWhenKeyDoesNotExists() {
        Mockito.`when`(pluginSettings[pluginSettingsRegionKey]).thenReturn("")
        val region = pluginSettingsRegionManager.getRegion()
        Assertions.assertEquals(Region.US_EAST_1.toString(), region)
    }

    @Test
    @Throws(Exception::class)
    fun shouldStoreValidRegion() {
        val validRegion = "area-52"
        Mockito.`when`(globalInfrastructure.regions).thenReturn(object : ArrayList<String>() {
            init {
                add("area-50")
                add("area-51")
                add("area-52")
            }
        })
        pluginSettingsRegionManager.storeRegion(validRegion)
        Mockito.verify(pluginSettings).put(pluginSettingsRegionKey, validRegion)
    }

    @Test
    fun shouldThrowExceptionWhenTryingToStoreAnInvalidRegion() {
        val invalidRegion = "area-53"
        Mockito.`when`(globalInfrastructure.regions).thenReturn(ArrayList())
        Assertions.assertThrows(InvalidAWSRegionException::class.java) {
            pluginSettingsRegionManager.storeRegion(
                invalidRegion
            )
        }
        Mockito.verify(pluginSettings, Mockito.never()).put(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
    }
}