package com.ogreten.catan.game.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SettlementRoadMapping {
        private int settlementIndex;
        private int roadIndex;
}
