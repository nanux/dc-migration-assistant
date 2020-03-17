package com.atlassian.migration.datacenter.core.aws;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.PartitionMetadata;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;

import java.util.List;
import java.util.stream.Collectors;

public class GlobalInfrastructure {

    public List<String> getRegions() {
        return Region.regions()
                .stream()
                .filter(region -> {
                    RegionMetadata regionMetadata = RegionMetadata.of(region);
                    if (regionMetadata == null) {
                        return false;
                    }
                    PartitionMetadata partitionMetadata = regionMetadata.partition();
                    if (partitionMetadata == null) {
                        return false;
                    }
                    return partitionMetadata.id().equals("aws");
                })
                .map(Region::toString)
                .collect(Collectors.toList());
    }
}
