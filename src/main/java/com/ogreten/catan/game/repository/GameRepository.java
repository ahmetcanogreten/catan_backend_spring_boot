package com.ogreten.catan.game.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.ogreten.catan.game.domain.Game;

public interface GameRepository extends CrudRepository<Game, Integer> {

    Optional<Game> findByRoomId(int roomId);
}
