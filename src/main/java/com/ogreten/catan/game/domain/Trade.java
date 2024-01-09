package com.ogreten.catan.game.domain;

import com.ogreten.catan.auth.domain.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

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

    @ManyToOne
    private User offerer;
}
