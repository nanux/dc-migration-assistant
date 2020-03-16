package com.atlassian.migration.datacenter.util;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.atlassian.migration.datacenter.core.aws.auth.ReadCredentialsService;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * Copyright Atlassian: 12/03/2020
 */
public class AwsCredentialsProviderShim implements AwsCredentialsProvider, AwsCredentials, ReadCredentialsService
{
    private final AWSCredentialsProvider v1Creds;

    public AwsCredentialsProviderShim(AWSCredentialsProvider v1Creds)
    {
        this.v1Creds = v1Creds;
    }

    @Override
    public AwsCredentials resolveCredentials()
    {
        return this;
    }

    @Override
    public String accessKeyId()
    {
        return v1Creds.getCredentials().getAWSAccessKeyId();
    }

    @Override
    public String secretAccessKey()
    {
        return v1Creds.getCredentials().getAWSSecretKey();
    }

    @Override
    public String getAccessKeyId()
    {
        return v1Creds.getCredentials().getAWSAccessKeyId();
    }

    @Override
    public String getSecretAccessKey()
    {
        return v1Creds.getCredentials().getAWSSecretKey();
    }
}
