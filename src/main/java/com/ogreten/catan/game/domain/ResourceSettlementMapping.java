package com.ogreten.catan.game.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResourceSettlementMapping {
        private int resourceIndex;
        private int settlementIndex;
}
