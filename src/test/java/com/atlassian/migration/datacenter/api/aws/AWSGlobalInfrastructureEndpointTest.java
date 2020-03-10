package com.atlassian.migration.datacenter.api.aws;

import com.atlassian.migration.datacenter.core.aws.GlobalInfrastructure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AWSGlobalInfrastructureEndpointTest {

    @Mock
    private GlobalInfrastructure mockGlobalInfrastructure;

    @InjectMocks
    private AWSGlobalInfrastructureEndpoint sut;


    @Test
    public void itShouldReturnServerErrorWhenGlobalInfrastructureModuleFails() {
        when(mockGlobalInfrastructure.getRegions()).thenReturn(null);

        Response res = sut.getRegions();

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), res.getStatus());
    }

    @Test void itShouldReturnAllRegions() {
        final String regionOne = "atlassian-east-1";
        final String regionTwo = "atlassian-west-1";

        when(mockGlobalInfrastructure.getRegions()).thenReturn(Arrays.asList(regionOne, regionTwo));

        Response res = sut.getRegions();

        assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

        assertEquals(Arrays.asList(regionOne, regionTwo), res.getEntity());
    }
}
