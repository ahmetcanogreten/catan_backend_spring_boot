package com.ogreten.catan.game.domain;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ogreten.catan.auth.domain.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class UserState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Game game;

    private int numberOfBrick;
    private int numberOfLumber;
    private int numberOfOre;
    private int numberOfGrain;
    private int numberOfWool;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> buildings;
}
