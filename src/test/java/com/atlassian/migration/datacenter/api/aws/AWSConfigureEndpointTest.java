package com.atlassian.migration.datacenter.api.aws;

import com.atlassian.migration.datacenter.core.aws.cloud.AWSConfigurationService;
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AWSConfigureEndpointTest {

    @Mock
    AWSConfigurationService configurationService;

    @InjectMocks
    AWSConfigureEndpoint sut;

    @Test
    void shouldConfigureAWS() throws InvalidMigrationStageError {
        AWSConfigureEndpoint.AWSConfigureWebObject payload = new AWSConfigureEndpoint.AWSConfigureWebObject();
        final String accessKeyId = "accessKeyId";
        final String secretKey = "secretKey";
        final String region = "us-east-1";
        payload.setAccessKeyId(accessKeyId);
        payload.setSecretAccessKey(secretKey);
        payload.setRegion(region);

        Response response = sut.storeAWSCredentials(payload);

        verify(configurationService).configureCloudProvider(accessKeyId, secretKey, region);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    void shouldReturnFailedWhenInvalidMigrationStage() throws InvalidMigrationStageError {
        AWSConfigureEndpoint.AWSConfigureWebObject payload = new AWSConfigureEndpoint.AWSConfigureWebObject();
        payload.setAccessKeyId("accessKeyId");
        payload.setSecretAccessKey("secretKey");
        payload.setRegion("us-east-1");

        doThrow(new InvalidMigrationStageError("")).when(configurationService).configureCloudProvider(anyString(), anyString(), anyString());

        Response response = sut.storeAWSCredentials(payload);

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
    }

}