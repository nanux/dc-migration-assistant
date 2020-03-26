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
import javax.annotation.PostConstruct
import org.slf4j.LoggerFactory
import software.amazon.awssdk.regions.Region

/**
 * Manages the persistence and retrieval of the region used to make AWS SDK API calls.
 * The region is stored in the plugin settings of this app.
 */
class PluginSettingsRegionManager(
    private val pluginSettingsFactorySupplier: Supplier<PluginSettingsFactory>,
    private val globalInfrastructure: GlobalInfrastructure
) : RegionService {
    private var pluginSettings: PluginSettings? = null

    @PostConstruct // FIXME: I do not work
    fun postConstruct() {
        logger.debug("setting up plugin settings")
        pluginSettings = pluginSettingsFactorySupplier.get().createGlobalSettings()
    }

    /**
     * @return The id of the region that has been stored most recently (e.g. us-east-2, ap-southeast-1). If no region
     * has been configured, it will return the id of the default region.
     */
    override fun getRegion(): String { // FIXME: Need to find a way to inject without calling the supplier every time
        val pluginSettings = pluginSettingsFactorySupplier.get().createGlobalSettings()
        val pluginSettingsRegion =
            pluginSettings[AWS_REGION_PLUGIN_STORAGE_KEY + REGION_PLUGIN_STORAGE_SUFFIX] as String
        return if ("" == pluginSettingsRegion) {
            Region.US_EAST_1.toString()
        } else pluginSettingsRegion
    }

    /**
     * Sets the region to be used for AWS API calls
     *
     * @param region the id of the region to use e.g. us-east-1, eu-central-1
     * @throws InvalidAWSRegionException if the region id provided is not a supported AWS region.
     * @see GlobalInfrastructure
     */
    @Throws(InvalidAWSRegionException::class)
    override fun storeRegion(region: String) {
        if (!isValidRegion(region)) {
            throw InvalidAWSRegionException()
        }
        // FIXME: Need to find a way to inject without calling the supplier every time
        val pluginSettings = pluginSettingsFactorySupplier.get().createGlobalSettings()
        pluginSettings.put(AWS_REGION_PLUGIN_STORAGE_KEY + REGION_PLUGIN_STORAGE_SUFFIX, region)
    }

    private fun isValidRegion(testRegion: String): Boolean {
        return globalInfrastructure.regions
            .stream()
            .anyMatch { region: String? -> region == testRegion }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PluginSettingsRegionManager::class.java)
        const val AWS_REGION_PLUGIN_STORAGE_KEY = "com.atlassian.migration.datacenter.core.aws.region"
        const val REGION_PLUGIN_STORAGE_SUFFIX = ".region"
    }
}