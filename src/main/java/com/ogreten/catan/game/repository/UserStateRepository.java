package com.ogreten.catan.game.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.ogreten.catan.game.domain.UserState;

public interface UserStateRepository extends CrudRepository<UserState, Integer> {
    public List<UserState> findByGameId(int gameId);

    public Optional<UserState> findByGameIdAndUserId(int gameId, UUID userId);

}
