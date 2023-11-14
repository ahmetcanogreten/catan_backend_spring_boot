package com.ogreten.catan.room.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import com.ogreten.catan.room.domain.Room;

public interface RoomRepository extends CrudRepository<Room, Integer> {
    Page<Room> findAllByIsGameStartedFalse(Pageable pageable);

    Optional<Room> findByCode(String code);
}
