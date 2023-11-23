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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = "Game", description = "Game Management API")
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

    @Operation(summary = "Start a game", description = "Start a game from existing room.", tags = { "game", "post" })
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = Game.class), mediaType = "application/json") })
    @ApiResponse(responseCode = "404", content = {
            @Content(schema = @Schema()) })
    @Transactional
    @PostMapping()
    public ResponseEntity<Game> startGame(
            @Parameter(description = "Room id of the game to be started.", example = "1") @RequestParam int roomId) {
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
