package com.ogreten.catan.game.schema;

import java.util.UUID;
import lombok.Data;

@Data
public class UserWithInGamePoints {
    private UUID id;
    private int points;
}
