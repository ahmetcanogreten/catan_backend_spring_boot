package com.ogreten.catan.game.repository;

import org.springframework.data.repository.CrudRepository;

import com.ogreten.catan.game.domain.Trade;

public interface TradeRepository extends CrudRepository<Trade, Integer> {

}
