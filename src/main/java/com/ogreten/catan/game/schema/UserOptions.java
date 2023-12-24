package com.ogreten.catan.game.schema;

import java.util.List;

import lombok.Data;

@Data
public class UserOptions {
    List<Integer> availableRoads;
    List<Integer> availableSettlements;
    List<Integer> availableCities;
}
