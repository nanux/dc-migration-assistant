package com.atlassian.migration.datacenter.core.fs;

import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.nio.file.Path;

/**
 * Contains configuration for S3 upload calls
 */
public class S3UploadConfig {
    private String bucketName;
    private S3AsyncClient s3AsyncClient;
    private Path sharedHome;

    public S3UploadConfig(String bucketName, S3AsyncClient s3AsyncClient, Path sharedHome) {
        this.bucketName = bucketName;
        this.s3AsyncClient = s3AsyncClient;
        this.sharedHome = sharedHome;
    }

    /**
     * Destination S3 bucket name where to upload files
     *
     * @return bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Asynchronous S3 client to call AWS API
     * @return async S3 client
     */
    public S3AsyncClient getS3AsyncClient() {
        return s3AsyncClient;
    }

    /**
     * Application shared home. In case of Server distribution, it should return the regular home.
     * @return application home (shared for DC, normal for Server distribution)
     */
    public Path getSharedHome() {
        return sharedHome;
    }
}
