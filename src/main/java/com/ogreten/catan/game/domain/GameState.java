package com.ogreten.catan.game.domain;

import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ogreten.catan.auth.domain.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class GameState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    private Game game;

    @ManyToOne
    private User turnUser;

    private TurnState turnState;

    private int dice1;
    private int dice2;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<Integer> availableSettlementsForTurnUser;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<Integer> availableRoadsForTurnUser;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<Integer> availableCitiesForTurnUser;
}
