package com.atlassian.migration.datacenter.api.aws;

import com.atlassian.migration.datacenter.core.aws.cloud.AWSConfigurationService;
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("aws/configure")
public class AWSConfigureEndpoint {

    private final AWSConfigurationService awsConfigurationService;

    public AWSConfigureEndpoint(AWSConfigurationService awsConfigurationService) {
        this.awsConfigurationService = awsConfigurationService;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response storeAWSCredentials(AWSConfigureWebObject configure) {
        try {
            awsConfigurationService.configureCloudProvider(configure.accessKeyId, configure.secretAccessKey, configure.region);
        } catch (InvalidMigrationStageError invalidMigrationStageError) {
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity(invalidMigrationStageError.getMessage())
                    .build();
        }

        return Response
                .noContent()
                .build();
    }

    @JsonAutoDetect
    static class AWSConfigureWebObject {

        private String accessKeyId;
        private String secretAccessKey;
        private String region;

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public String getRegion() {
            return region;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }
}
