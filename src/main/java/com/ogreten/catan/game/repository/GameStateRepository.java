package com.ogreten.catan.game.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.ogreten.catan.game.domain.GameState;

public interface GameStateRepository extends CrudRepository<GameState, Integer> {
    Optional<GameState> findByGameId(int gameId);
}
