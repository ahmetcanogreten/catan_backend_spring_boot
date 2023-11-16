package com.ogreten.catan.game.repository;

import org.springframework.data.repository.CrudRepository;

import com.ogreten.catan.game.domain.Game;

public interface GameRepository extends CrudRepository<Game, Integer> {

}
