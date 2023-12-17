package com.ogreten.catan.game.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.ogreten.catan.game.domain.UserState;

public interface UserStateRepository extends CrudRepository<UserState, Integer> {
    public List<UserState> findByGameId(int gameId);

}
