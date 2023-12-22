package com.ogreten.catan.game.service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.ogreten.catan.game.domain.SettlementRoadMapping;

public class SettlementRoadMapper {

    // Last index of village is 53
    // Last index of road is 71

    private List<SettlementRoadMapping> settlementRoadMappings = new ArrayList<>();
    private String fileName = "src/main/java/com/ogreten/catan/game/service/settlement_to_road_map.csv";

    private static SettlementRoadMapper instance;

    public static SettlementRoadMapper getInstance() {
        if (instance == null) {
            instance = new SettlementRoadMapper();
        }
        return instance;
    }

    private SettlementRoadMapper() {
        try {
            List<String> settlementRoadMappingRaw = Files.readAllLines(Paths.get(fileName));
            for (String line : settlementRoadMappingRaw) {
                String[] items = line.split(",");
                settlementRoadMappings
                        .add(new SettlementRoadMapping(Integer.parseInt(items[0]), Integer.parseInt(items[1])));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<SettlementRoadMapping> getSettlementRoadMappings() {
        return settlementRoadMappings;
    }

    public List<Integer> getRoadsOfVillage(int settlementIndex) {
        List<Integer> roads = new ArrayList<>();
        for (SettlementRoadMapping villageRoadMapping : settlementRoadMappings) {
            if (villageRoadMapping.getSettlementIndex() == settlementIndex) {
                roads.add(villageRoadMapping.getRoadIndex());
            }
        }
        return roads;
    }

}
