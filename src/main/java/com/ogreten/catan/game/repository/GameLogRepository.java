package com.ogreten.catan.game.repository;

import org.springframework.data.repository.CrudRepository;

import com.ogreten.catan.game.domain.GameLog;

import java.util.List;

public interface GameLogRepository extends CrudRepository<GameLog, Integer> {
    List<GameLog> findByGameId(int gameId);
}
