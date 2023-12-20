package com.ogreten.catan.game.schema;

import java.util.UUID;

import lombok.Data;

@Data
public class RollInfo {
    private UUID userId;
    private int dice1;
    private int dice2;
}
