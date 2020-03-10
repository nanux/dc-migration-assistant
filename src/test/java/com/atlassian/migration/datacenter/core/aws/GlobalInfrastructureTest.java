package com.atlassian.migration.datacenter.core.aws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static software.amazon.awssdk.regions.Region.AP_EAST_1;
import static software.amazon.awssdk.regions.Region.AP_NORTHEAST_1;
import static software.amazon.awssdk.regions.Region.AP_NORTHEAST_2;
import static software.amazon.awssdk.regions.Region.AP_SOUTHEAST_1;
import static software.amazon.awssdk.regions.Region.AP_SOUTHEAST_2;
import static software.amazon.awssdk.regions.Region.AP_SOUTH_1;
import static software.amazon.awssdk.regions.Region.CA_CENTRAL_1;
import static software.amazon.awssdk.regions.Region.EU_CENTRAL_1;
import static software.amazon.awssdk.regions.Region.EU_NORTH_1;
import static software.amazon.awssdk.regions.Region.EU_WEST_1;
import static software.amazon.awssdk.regions.Region.EU_WEST_2;
import static software.amazon.awssdk.regions.Region.EU_WEST_3;
import static software.amazon.awssdk.regions.Region.ME_SOUTH_1;
import static software.amazon.awssdk.regions.Region.SA_EAST_1;
import static software.amazon.awssdk.regions.Region.US_EAST_1;
import static software.amazon.awssdk.regions.Region.US_EAST_2;
import static software.amazon.awssdk.regions.Region.US_WEST_1;
import static software.amazon.awssdk.regions.Region.US_WEST_2;

public class GlobalInfrastructureTest {

    GlobalInfrastructure sut;

    @BeforeEach
    public void initialiseSut() {
        sut = new GlobalInfrastructure();
    }

    @Test
    public void itShouldReturnAllRegions() {
        List<String> regions = sut.getRegions();

        final List<String> supportedRegions = Arrays.asList(AP_SOUTH_1, EU_NORTH_1, EU_WEST_3, EU_WEST_2, EU_WEST_1, AP_NORTHEAST_2, AP_NORTHEAST_1, ME_SOUTH_1, CA_CENTRAL_1, SA_EAST_1, AP_EAST_1, AP_SOUTHEAST_1, AP_SOUTHEAST_2, EU_CENTRAL_1, US_EAST_1, US_EAST_2, US_WEST_1, US_WEST_2)
                .stream()
                .map(Region::toString)
                .collect(Collectors.toList());

        assertIterableEquals(supportedRegions, regions);
    }

}
