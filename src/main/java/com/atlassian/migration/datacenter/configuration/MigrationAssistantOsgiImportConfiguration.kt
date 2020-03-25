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
package com.atlassian.migration.datacenter.configuration

import com.atlassian.activeobjects.external.ActiveObjects
import com.atlassian.jira.config.util.JiraHome
import com.atlassian.plugins.osgi.javaconfig.OsgiServices
import com.atlassian.sal.api.auth.LoginUriProvider
import com.atlassian.sal.api.permission.PermissionEnforcer
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.scheduler.SchedulerService
import com.atlassian.soy.renderer.SoyTemplateRenderer
import com.atlassian.util.concurrent.Supplier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MigrationAssistantOsgiImportConfiguration {
    @Bean
    fun getPluginSettingsFactory(): PluginSettingsFactory {
        return OsgiServices.importOsgiService(PluginSettingsFactory::class.java)
    }

    @Bean
    fun getSoyTemplateRenderer(): SoyTemplateRenderer {
        return OsgiServices.importOsgiService(SoyTemplateRenderer::class.java)
    }

    @Bean
    fun getPermissionEnforcer(): PermissionEnforcer {
        return OsgiServices.importOsgiService(PermissionEnforcer::class.java)
    }

    @Bean
    fun getLoginUriProvider(): LoginUriProvider {
        return OsgiServices.importOsgiService(LoginUriProvider::class.java)
    }

    @Bean
    fun getJiraHome(): JiraHome {
        return OsgiServices.importOsgiService(JiraHome::class.java)
    }

    @Bean
    fun ao(): ActiveObjects {
        return OsgiServices.importOsgiService(ActiveObjects::class.java)
    }

    @Bean
    fun schedulerService(): SchedulerService {
        return OsgiServices.importOsgiService(SchedulerService::class.java)
    }

    @Bean
    fun settingsFactorySupplier(): Supplier<PluginSettingsFactory> {
        return SpringOsgiConfigurationUtil.lazyImportOsgiService<PluginSettingsFactory>(PluginSettingsFactory::class.java)
    }
}