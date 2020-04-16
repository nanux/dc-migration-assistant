package com.atlassian.migration.datacenter
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
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.core.SdkSystemSetting
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest
import java.io.File
import java.net.URI

@Mojo(name = "upload", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
class ArtifactUploader : AbstractMojo() {

    @Parameter(property = "upload.bucket", required = true)
    lateinit var bucketName: String

    @Parameter(property = "upload.aws_region")
    lateinit var region: String

    @Parameter(property = "upload.aws_access_key_id")
    lateinit var accessKeyId: String

    @Parameter(property = "upload.aws_secret_key_id")
    lateinit var secretKeyId: String

    @Parameter(property = "upload.role_arn")
    lateinit var roleArn: String

    @Parameter(property = "upload.profile")
    lateinit var profile: String

    @Parameter(property = "upload.external_id")
    lateinit var externalId: String

    @Parameter(property = "project.build.directory")
    lateinit var outputDirectory: String

    @Parameter(property = "project.build.finalName")
    lateinit var artifactName: String

    @Parameter(property = "project.packaging")
    lateinit var packaging: String

    @Parameter(property = "project.enabled", required = false)
    private var enabled: Boolean = false

    override fun execute() {
        if (this.enabled) {

            setupProperties()

            try {
                log.info("Starting upload of artifact to S3 bucket $bucketName")
                var s3 = clientBuilder(SdkSystemSetting.AWS_REGION.resolveSdkSystemSetting(false))
                val artifactFile = File(this.outputDirectory + "/" + this.artifactName + "." + this.packaging)
                val key: String? = this.artifactName + "." + this.packaging

                val bucketExists = checkForBucketExistence(s3)

                if (!bucketExists!!) {
                    throw MojoFailureException("Bucket $bucketName does not exist.")
                } else {
                    val bucketRegion = findBucketRegion()
                    s3 = clientBuilder(bucketRegion)
                    val putRequest = buildPutObjectRequest(key)
                    val response = s3?.putObject(putRequest, artifactFile.toPath())
                    log.info(response?.versionId())
                    log.info("Successfully uploaded $artifactFile to the S3 bucket $bucketName")
                }

            } catch (e: Exception) {
                log.error(e.localizedMessage)
                throw e
            }
        } else {
            log.info("Plugin not enabled. Skipping.")
        }
    }

    private fun buildPutObjectRequest(key: String?): PutObjectRequest? {
        val acl = ObjectCannedACL.PUBLIC_READ
        return PutObjectRequest.builder()
                .bucket(this.bucketName)
                .key(key)
                .acl(acl)
                .build()
    }

    private fun setupProperties() {
        if (this::region.isInitialized) {
            System.getProperties()[SdkSystemSetting.AWS_REGION.property()] = this.region
        } else {
            checkValue(SdkSystemSetting.AWS_REGION)
        }
        if (this::accessKeyId.isInitialized) {
            System.getProperties()[SdkSystemSetting.AWS_ACCESS_KEY_ID.property()] = this.accessKeyId
        } else {
            checkValue(SdkSystemSetting.AWS_ACCESS_KEY_ID)
        }
        if (this::secretKeyId.isInitialized) {
            System.getProperties()[SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property()] = this.secretKeyId
        } else {
            checkValue(SdkSystemSetting.AWS_SECRET_ACCESS_KEY)
        }
        if (this::roleArn.isInitialized) {
            System.getProperties()[SdkSystemSetting.AWS_ROLE_ARN.property()] = this.roleArn
        }
        if (this::externalId.isInitialized) {
            System.getProperties()[EXTERNAL_ID] = this.externalId
        }
        if (this::profile.isInitialized) {
            System.getProperties()[PROFILE] = this.profile
        }
    }

    private fun findBucketRegion(): String? {
        val request = GetBucketLocationRequest.builder()
                .bucket(this.bucketName)
                .build()
        val location = locationClientBuilder()?.getBucketLocation(request)
        return if (location?.locationConstraintAsString().equals(""))
            Region.US_EAST_1.toString()
        else
            location?.locationConstraintAsString()
    }

    private fun checkForBucketExistence(s3: S3Client?): Boolean? {
        val bucketList = s3?.listBuckets()?.buckets()
        var bucketExists = false
        bucketList?.forEach { bucket ->
            if (bucket?.name().equals(this.bucketName)) {
                bucketExists = true
            }
        }
        return bucketExists
    }

    private fun checkValue(setting: SdkSystemSetting?) {
        val exists = System.getProperties().containsKey(setting?.property()) || System.getenv().containsKey(setting?.environmentVariable())
        if (!exists) {
            throw MojoExecutionException("If not defined in plugin configuration, ${setting?.environmentVariable()} must be defined in environment variables.")
        }
    }

    private fun clientBuilder(bucketRegion: String?): S3Client? {
        val builder = S3Client.builder()
        builder.region(Region.of(bucketRegion))
        builder.serviceConfiguration(S3Configuration.builder().dualstackEnabled(true).build())
        builder.credentialsProvider(credentialsProvider())
        return builder.build()
    }

    private fun locationClientBuilder(): S3Client? {
        val builder = S3Client.builder()
        val endpoint = "https://$bucketName.s3.amazonaws.com"
        builder.region(Region.US_EAST_1)
        builder.serviceConfiguration(S3Configuration.builder().dualstackEnabled(true).build())
        builder.endpointOverride(URI(endpoint))
        builder.credentialsProvider(credentialsProvider())
        return builder.build()
    }

    private fun credentialsProvider(): AwsCredentialsProvider? {
        val defaultCredentials: AwsCredentialsProvider = if (!System.getProperties().containsKey(PROFILE)) {
            DefaultCredentialsProvider.builder()
                    .asyncCredentialUpdateEnabled(true)
                    .reuseLastProviderEnabled(true)
                    .build()
        } else {
            ProfileCredentialsProvider.builder().profileName(System.getProperty(PROFILE)).build()
        }

        val isSTS = defaultCredentials.resolveCredentials().javaClass.equals(AwsSessionCredentials::class.java)

        if (!isSTS && System.getProperties().containsKey(SdkSystemSetting.AWS_ROLE_ARN.property())) {
            val clientBuilder = StsClient.builder()
            if (SdkSystemSetting.AWS_REGION.resolveSdkSystemSetting(false) != null) {
                clientBuilder.region(Region.of(SdkSystemSetting.AWS_REGION.resolveSdkSystemSetting(false)))
            }
            clientBuilder.credentialsProvider(defaultCredentials)
            val client = clientBuilder.build()
            val requestBuilder = AssumeRoleRequest.builder()
            requestBuilder.roleArn(SdkSystemSetting.AWS_ROLE_ARN.resolveSdkSystemSetting(false))
            if (System.getProperties().containsKey(EXTERNAL_ID)) {
                requestBuilder.externalId(System.getProperty(EXTERNAL_ID))
            }
            requestBuilder.roleSessionName("MavenPlugin")
            val request = requestBuilder.build()
            return StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(client)
                    .refreshRequest(request)
                    .asyncCredentialUpdateEnabled(true)
                    .build()
        } else {
            return defaultCredentials
        }
    }

    private fun SdkSystemSetting?.resolveSdkSystemSetting(environmentVar: Boolean?): String? {
        return if (environmentVar!!)
            System.getenv(this?.environmentVariable())!!
        else
            System.getProperty(this?.property()!!)!!
    }

    companion object {
        const val EXTERNAL_ID: String = "aws.externalId"
        const val PROFILE: String = "aws.profile"
    }
}