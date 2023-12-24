package com.ogreten.catan.game.util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.ogreten.catan.game.domain.ResourceSettlementMapping;

public class ResourceSettlementMapper {

    // Last index of village is 53
    // Last index of road is 71

    private List<ResourceSettlementMapping> resourceSettlementMappins = new ArrayList<>();
    private String fileName = "src/main/java/com/ogreten/catan/game/service/resource_to_settlement_map.csv";

    private static ResourceSettlementMapper instance;

    public static ResourceSettlementMapper getInstance() {
        if (instance == null) {
            instance = new ResourceSettlementMapper();
        }
        return instance;
    }

    private ResourceSettlementMapper() {
        try {
            List<String> resourceSettlementMappingRaw = Files.readAllLines(Paths.get(fileName));
            for (String line : resourceSettlementMappingRaw) {
                String[] items = line.split(",");
                resourceSettlementMappins
                        .add(new ResourceSettlementMapping(Integer.parseInt(items[0]), Integer.parseInt(items[1])));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Integer> getResourceIndexesNearSettlement(int settlementIndex) {
        List<Integer> resources = new ArrayList<>();
        for (ResourceSettlementMapping resourceSettlementMapping : resourceSettlementMappins) {
            if (resourceSettlementMapping.getSettlementIndex() == settlementIndex) {
                resources.add(resourceSettlementMapping.getResourceIndex());
            }
        }
        return resources;
    }

}
