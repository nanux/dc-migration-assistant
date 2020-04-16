package com.atlassian.migration.datacenter

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest
import java.io.File

@Mojo(name = "upload", defaultPhase = LifecyclePhase.DEPLOY)
class ArtifactUploader(private var environment: MutableMap<String, String>? = System.getenv()) : AbstractMojo() {

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

    @Parameter(property = "upload.external_id")
    lateinit var externalId: String

    @Parameter(property = "project.build.directory")
    lateinit var outputDirectory: String

    @Parameter(property = "project.build.finalName")
    lateinit var artifactName: String

    override fun execute() {
        environment?.putIfAbsent(REGION, this.region)
        environment?.putIfAbsent(ACCESS_KEY, this.accessKeyId)
        environment?.putIfAbsent(SECRET_KEY, this.secretKeyId)
        environment?.putIfAbsent(ROLE_ARN, this.roleArn)
        environment?.putIfAbsent(EXTERNAL_ID, this.externalId)

        try {
            val s3 = clientBuilder()
            val artifactFile = File(this.outputDirectory + "/" + this.artifactName)

            val bucketList = s3?.listBuckets()?.buckets()
            var bucketExists = false
            bucketList?.forEach { bucket ->
                if (bucket?.name().equals(this.bucketName)) {
                    bucketExists = true
                }
            }
            if (!bucketExists) {
                val request = CreateBucketRequest.builder().bucket(this.bucketName).build()
                s3?.createBucket(request)
            }
            val acl = ObjectCannedACL.PUBLIC_READ
            val putRequest = PutObjectRequest.builder().bucket(this.bucketName).acl(acl).build()
            val response = s3?.putObject(putRequest, artifactFile.toPath())
            log.info(response?.versionId())
        } catch (e: Exception) {
            log.error(e.localizedMessage)
        }
    }

    private fun clientBuilder(): S3Client? {
        val builder = S3Client.builder()

        if (environment?.get(REGION) != null) {
            builder.region(Region.of(environment?.get(REGION)))
        }

        builder.credentialsProvider(credentialsProvider())
        return builder.build()


    }

    private fun credentialsProvider(): AwsCredentialsProvider? {
        val basicCredentials: AwsCredentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(environment?.get(ACCESS_KEY), environment?.get(SECRET_KEY)))

        if (environment?.get(ROLE_ARN) != null) {
            val clientBuilder = StsClient.builder()
            if (environment?.get(REGION) != null) {
                clientBuilder.region(Region.of(environment?.get(REGION)))
            }
            clientBuilder.credentialsProvider(basicCredentials)
            val client = clientBuilder.build()
            val requestBuilder = AssumeRoleRequest.builder()
            requestBuilder.roleArn(environment?.get(ROLE_ARN))
            if (environment?.get(EXTERNAL_ID) != null) {
                requestBuilder.externalId(environment?.get(EXTERNAL_ID))
            }
            val request = requestBuilder.build()
            val response = client.assumeRole(request)
            response.credentials()
            val stsCredentials = StsAssumeRoleCredentialsProvider.builder().stsClient(client).build()
            return stsCredentials
        } else {
            return basicCredentials
        }
    }

    companion object {
        const val REGION: String = "AWS_REGION"
        const val ACCESS_KEY: String = "AWS_ACCESS_KEY_ID"
        const val SECRET_KEY: String = "AWS_SECRET_ACCESS_KEY"
        const val ROLE_ARN: String = "ROLE_ARN"
        const val EXTERNAL_ID: String = "EXTERNAL_ID"
        const val SESSION_TOKEN: String = "AWS_SESSION_TOKEN"
    }
}