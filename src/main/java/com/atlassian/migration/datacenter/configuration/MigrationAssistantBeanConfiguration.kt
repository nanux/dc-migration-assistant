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
import com.atlassian.migration.datacenter.core.application.ApplicationConfiguration
import com.atlassian.migration.datacenter.core.application.JiraConfiguration
import com.atlassian.migration.datacenter.core.aws.AWSMigrationService
import com.atlassian.migration.datacenter.core.aws.CfnApi
import com.atlassian.migration.datacenter.core.aws.GlobalInfrastructure
import com.atlassian.migration.datacenter.core.aws.auth.*
import com.atlassian.migration.datacenter.core.aws.cloud.AWSConfigurationService
import com.atlassian.migration.datacenter.core.aws.db.DatabaseMigrationService
import com.atlassian.migration.datacenter.core.aws.infrastructure.QuickstartDeploymentService
import com.atlassian.migration.datacenter.core.aws.region.AvailabilityZoneManager
import com.atlassian.migration.datacenter.core.aws.region.PluginSettingsRegionManager
import com.atlassian.migration.datacenter.core.aws.region.RegionService
import com.atlassian.migration.datacenter.core.aws.ssm.SSMApi
import com.atlassian.migration.datacenter.core.fs.S3FilesystemMigrationService
import com.atlassian.migration.datacenter.core.fs.download.s3sync.S3SyncFileSystemDownloadManager
import com.atlassian.migration.datacenter.core.fs.download.s3sync.S3SyncFileSystemDownloader
import com.atlassian.migration.datacenter.spi.MigrationService
import com.atlassian.migration.datacenter.spi.fs.FilesystemMigrationService
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.scheduler.SchedulerService
import com.atlassian.util.concurrent.Supplier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.ssm.SsmClient
import java.nio.file.Paths

@Configuration
@ComponentScan
class MigrationAssistantBeanConfiguration {
    @Bean
    fun s3AsyncClientSupplier(credentialsProvider: AwsCredentialsProvider, regionService: RegionService): Supplier<S3AsyncClient> {
        return Supplier {
            S3AsyncClient.builder()
                    .credentialsProvider(credentialsProvider)
                    .region(Region.of(regionService.getRegion()))
                    .build()
        }
    }

    @Bean
    fun ssmClient(credentialsProvider: AwsCredentialsProvider?, regionService: RegionService): Supplier<SsmClient> {
        return Supplier {
            SsmClient.builder()
                    .credentialsProvider(credentialsProvider)
                    .region(Region.of(regionService.getRegion()))
                    .build()
        }
    }

    @Bean
    fun awsCredentialsProvider(readCredentialsService: ReadCredentialsService): AwsCredentialsProvider {
        return AtlassianPluginAWSCredentialsProvider(readCredentialsService)
    }

    @Bean
    fun readCredentialsService(pluginSettingsFactorySupplier: Supplier<PluginSettingsFactory>, jiraHome: JiraHome?): EncryptedCredentialsStorage {
        return EncryptedCredentialsStorage(pluginSettingsFactorySupplier, jiraHome)
    }

    @Bean
    fun regionService(pluginSettingsFactorySupplier: Supplier<PluginSettingsFactory>, globalInfrastructure: GlobalInfrastructure): RegionService {
        return PluginSettingsRegionManager(pluginSettingsFactorySupplier, globalInfrastructure)
    }

    @Bean
    fun globalInfrastructure(): GlobalInfrastructure {
        return GlobalInfrastructure()
    }

    @Bean
    fun probeAWSAuth(awsCredentialsProvider: AwsCredentialsProvider, regionService: RegionService): ProbeAWSAuth {
        return ProbeAWSAuth(awsCredentialsProvider, regionService)
    }

    @Bean
    fun jiraConfiguration(jiraHome: JiraHome): JiraConfiguration {
        return JiraConfiguration(jiraHome)
    }

    @Bean
    fun databaseMigrationService(jiraConfiguration: ApplicationConfiguration, s3AsyncClient: Supplier<S3AsyncClient>): DatabaseMigrationService {
        val tempDirectoryPath = System.getProperty("java.io.tmpdir")
        return DatabaseMigrationService(jiraConfiguration, Paths.get(tempDirectoryPath), s3AsyncClient)
    }

    @Bean
    fun ssmApi(client: Supplier<SsmClient>): SSMApi {
        return SSMApi(client)
    }

    @Bean
    fun s3SyncFileSystemDownloader(ssmApi: SSMApi): S3SyncFileSystemDownloader {
        return S3SyncFileSystemDownloader(ssmApi)
    }

    @Bean
    fun s3SyncFileSystemDownloadManager(downloader: S3SyncFileSystemDownloader): S3SyncFileSystemDownloadManager {
        return S3SyncFileSystemDownloadManager(downloader)
    }

    @Bean
    fun availabilityZoneManager(awsCredentialsProvider: AwsCredentialsProvider, globalInfrastructure: GlobalInfrastructure): AvailabilityZoneManager {
        return AvailabilityZoneManager(awsCredentialsProvider, globalInfrastructure)
    }

    @Bean
    fun awsConfigurationService(writeCredentialsService: WriteCredentialsService, regionService: RegionService, migrationService: MigrationService): AWSConfigurationService {
        return AWSConfigurationService(writeCredentialsService, regionService, migrationService)
    }

    @Bean
    fun cfnApi(awsCredentialsProvider: AwsCredentialsProvider, regionService: RegionService): CfnApi {
        return CfnApi(awsCredentialsProvider, regionService)
    }

    @Bean
    fun filesystemMigrationService(clientSupplier: Supplier<S3AsyncClient>, jiraHome: JiraHome, downloadManager: S3SyncFileSystemDownloadManager, migrationService: MigrationService, schedulerService: SchedulerService): FilesystemMigrationService {
        return S3FilesystemMigrationService(clientSupplier, jiraHome, downloadManager, migrationService, schedulerService)
    }

    @Bean
    fun migrationService(ao: ActiveObjects): MigrationService {
        return AWSMigrationService(ao)
    }

    @Bean
    fun quickstartDeploymentService(ao: ActiveObjects, cfnApi: CfnApi, migrationService: MigrationService): QuickstartDeploymentService {
        return QuickstartDeploymentService(ao, cfnApi, migrationService)
    }
}