package com.ogreten.catan.game.schema;

import lombok.Data;

@Data
public class TradeOffer {
    private int wantHills;
    private int wantForest;
    private int wantMountains;
    private int wantFields;
    private int wantPasture;

    private int giveHills;
    private int giveForest;
    private int giveMountains;
    private int giveFields;
    private int givePasture;
}
