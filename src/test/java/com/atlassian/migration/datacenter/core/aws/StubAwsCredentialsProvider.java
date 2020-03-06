package com.atlassian.migration.datacenter.core.aws;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public class StubAwsCredentialsProvider implements AwsCredentialsProvider {

    @Override
    public AwsCredentials resolveCredentials() {
        return new AwsCredentials() {
            @Override
            public String accessKeyId() {
                return "stub-access-key";
            }

            @Override
            public String secretAccessKey() {
                return "stub-secret-key";
            }
        };
    }
}
