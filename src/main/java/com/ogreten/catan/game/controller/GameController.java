package com.ogreten.catan.game.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ogreten.catan.auth.domain.User;
import com.ogreten.catan.game.domain.Game;
import com.ogreten.catan.game.domain.GameState;
import com.ogreten.catan.game.repository.GameRepository;
import com.ogreten.catan.game.repository.GameStateRepository;
import com.ogreten.catan.room.domain.Room;
import com.ogreten.catan.room.repository.RoomRepository;

import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/games")
public class GameController {

    RoomRepository roomRepository;
    GameRepository gameRepository;
    GameStateRepository gameStateRepository;

    public GameController(RoomRepository roomRepository, GameRepository gameRepository,
            GameStateRepository gameStateRepository) {
        this.roomRepository = roomRepository;
        this.gameRepository = gameRepository;
        this.gameStateRepository = gameStateRepository;
    }

    @Transactional
    @PostMapping()
    public ResponseEntity<Game> startGame(@RequestParam int roomId) {
        Optional<Room> optionalRoom = roomRepository.findById(roomId);

        if (optionalRoom.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Room room = optionalRoom.get();
        room.setGameStarted(true);
        roomRepository.save(room);

        Game game = new Game();
        game.setRoom(room);
        game.setStartedAt(Instant.now());
        Set<User> users = new HashSet<>(room.getUsers());
        game.setUsers(users);

        game.setResources(null); // TODO :
        gameRepository.save(game);

        GameState gameState = new GameState();
        gameState.setGame(game);
        gameState.setTurnUser(room.getOwner()); // TODO : random | Add list of users for turn sequence
        gameStateRepository.save(gameState);

        return ResponseEntity.ok().body(game);
    }

}
