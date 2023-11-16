package com.ogreten.catan.game.domain;

import java.time.Instant;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ogreten.catan.auth.domain.User;
import com.ogreten.catan.room.domain.Room;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private Instant startedAt;
    private Instant finishedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    JsonType resources;

    @OneToOne
    private Room room;

    @OneToMany
    private Set<User> users;
}
