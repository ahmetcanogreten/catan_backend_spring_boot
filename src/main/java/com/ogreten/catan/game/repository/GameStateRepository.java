package com.ogreten.catan.game.repository;

import org.springframework.data.repository.CrudRepository;

import com.ogreten.catan.game.domain.GameState;

public interface GameStateRepository extends CrudRepository<GameState, Integer> {

}
