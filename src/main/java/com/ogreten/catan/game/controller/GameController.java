package com.ogreten.catan.game.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ogreten.catan.auth.domain.User;
import com.ogreten.catan.game.domain.Game;
import com.ogreten.catan.game.domain.GameState;
import com.ogreten.catan.game.domain.TurnState;
import com.ogreten.catan.game.domain.UserState;
import com.ogreten.catan.game.domain.SettlementRoadMapping;
import com.ogreten.catan.game.repository.GameRepository;
import com.ogreten.catan.game.repository.GameStateRepository;
import com.ogreten.catan.game.repository.UserStateRepository;
import com.ogreten.catan.game.schema.RollInfo;
import com.ogreten.catan.game.service.ResourceSettlementMapper;
import com.ogreten.catan.game.service.SettlementRoadMapper;
import com.ogreten.catan.room.domain.Resource;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Transactional
@Tag(name = "Game", description = "Game Management API")
@RestController
@RequestMapping("/api/games")
public class GameController {

        RoomRepository roomRepository;
        GameRepository gameRepository;
        GameStateRepository gameStateRepository;
        UserStateRepository userStateRepository;

        public GameController(RoomRepository roomRepository, GameRepository gameRepository,
                        GameStateRepository gameStateRepository,

                        UserStateRepository userStateRepository) {
                this.roomRepository = roomRepository;
                this.gameRepository = gameRepository;
                this.gameStateRepository = gameStateRepository;
                this.userStateRepository = userStateRepository;

        }

        @Operation(summary = "Start a game", description = "Start a game from existing room.", tags = { "game",
                        "post" })
        @ApiResponse(responseCode = "200", content = {
                        @Content(schema = @Schema(implementation = Game.class), mediaType = "application/json") })
        @ApiResponse(responseCode = "404", content = {
                        @Content(schema = @Schema()) })
        @PostMapping()
        public ResponseEntity<Game> startGame(
                        @Parameter(description = "Room id of the game to be started.", example = "1") @RequestParam int roomId) {
                Optional<Room> optionalRoom = roomRepository.findById(roomId);

                if (optionalRoom.isEmpty()) {
                        return ResponseEntity.notFound().build();
                }

                // Close the room
                Room room = optionalRoom.get();
                room.setGameStarted(true);
                roomRepository.save(room);

                // Create a game
                Game game = new Game();
                game.setRoom(room);
                game.setStartedAt(Instant.now());
                Set<User> users = new HashSet<>(room.getUsers());
                game.setUsers(users);

                ArrayList<String> usersCycle = new ArrayList<>();
                for (User user : users) {
                        usersCycle.add(user.getId().toString());
                }
                Collections.shuffle(usersCycle);
                game.setUsersCycle(usersCycle);

                final List<Resource> roomResources = room.getResources();

                game.setResources(roomResources);
                gameRepository.save(game);

                // Create user states
                for (User user : users) {
                        // TODO : Check if there is a settlement two roads away from another
                        final int randomUserSettlementIndex = (new Random().nextInt(54));
                        final List<Integer> roadsOfRandomUserSettlementIndex = SettlementRoadMapper.getInstance()
                                        .getRoadsOfVillage(randomUserSettlementIndex);

                        final int randomUserRoadIndex = roadsOfRandomUserSettlementIndex
                                        .get(new Random().nextInt(roadsOfRandomUserSettlementIndex.size()));

                        int numberOfBrick = 0;
                        int numberOfLumber = 0;
                        int numberOfOre = 0;
                        int numberOfGrain = 0;
                        int numberOfWool = 0;

                        final List<Integer> resourceIndexesNearSettlement = ResourceSettlementMapper.getInstance()
                                        .getResourceIndexesNearSettlement(randomUserSettlementIndex);

                        for (Integer resourceIndex : resourceIndexesNearSettlement) {
                                Resource resource = roomResources.stream()
                                                .filter(eachResource -> eachResource.getIndex() == resourceIndex)
                                                .findFirst().get();

                                switch (resource.getType()) {
                                        case "hills":
                                                numberOfBrick += 1;
                                                break;
                                        case "forest":
                                                numberOfLumber += 1;
                                                break;
                                        case "mountains":
                                                numberOfOre += 1;
                                                break;
                                        case "fields":
                                                numberOfGrain += 1;
                                                break;
                                        case "pasture":
                                                numberOfWool += 1;
                                                break;
                                        default:
                                                break;
                                }
                        }

                        UserState userState = new UserState();
                        userState.setGame(game);
                        userState.setUser(user);
                        userState.setNumberOfBrick(numberOfBrick);
                        userState.setNumberOfLumber(
                                        numberOfLumber);
                        userState.setNumberOfOre(
                                        numberOfOre);
                        userState.setNumberOfGrain(
                                        numberOfGrain);
                        userState.setNumberOfWool(
                                        numberOfWool);

                        userState.setBuildings(Map.of(
                                        "road", List.of(
                                                        randomUserRoadIndex),
                                        "settlement", List.of(
                                                        randomUserSettlementIndex),
                                        "city", List.of()));
                        userStateRepository.save(userState);
                }

                // Game state
                User turnUser = users.stream().filter(user -> user.getId().toString().equals(usersCycle.get(0)))
                                .findFirst()
                                .get();

                GameState gameState = new GameState();
                gameState.setGame(game);
                gameState.setTurnUser(turnUser);
                gameState.setTurnState(
                                TurnState.ROLL);
                gameStateRepository.save(gameState);

                return ResponseEntity.ok().body(game);
        }

        @Operation(summary = "Start a game", description = "Start a game from existing room.", tags = { "game",
                        "post" })
        @ApiResponse(responseCode = "200", content = {
                        @Content(schema = @Schema(implementation = Game.class), mediaType = "application/json") })
        @ApiResponse(responseCode = "404", content = {
                        @Content(schema = @Schema()) })
        @GetMapping("/{gameId}")
        public ResponseEntity<Game> getGame(
                        @Parameter(description = "Room id of the game to be started.", example = "1") @PathVariable int gameId) {
                Optional<Game> optionalGame = gameRepository.findById(gameId);

                if (optionalGame.isEmpty()) {
                        return ResponseEntity.notFound().build();
                }

                Game game = optionalGame.get();
                return ResponseEntity.ok().body(game);
        }

        @Operation(summary = "Start a game", description = "Start a game from existing room.", tags = { "game",
                        "post" })
        @ApiResponse(responseCode = "200", content = {
                        @Content(schema = @Schema(implementation = Game.class), mediaType = "application/json") })
        @ApiResponse(responseCode = "404", content = {
                        @Content(schema = @Schema()) })
        @GetMapping("/{gameId}/state")
        public ResponseEntity<GameState> getGameState(
                        @Parameter(description = "Room id of the game to be started.", example = "1") @PathVariable int gameId) {
                Optional<GameState> optionalGameState = gameStateRepository.findByGameId(gameId);

                if (optionalGameState.isEmpty()) {
                        return ResponseEntity.notFound().build();
                }

                GameState gameState = optionalGameState.get();
                return ResponseEntity.ok().body(gameState);
        }

        @Operation(summary = "Start a game", description = "Start a game from existing room.", tags = { "game",
                        "post" })
        @ApiResponse(responseCode = "200", content = {
                        @Content(schema = @Schema(implementation = Game.class), mediaType = "application/json") })
        @ApiResponse(responseCode = "404", content = {
                        @Content(schema = @Schema()) })
        @GetMapping("/{gameId}/user-states")
        public ResponseEntity<List<UserState>> getUserStatesInAGame(
                        @Parameter(description = "Room id of the game to be started.", example = "1") @PathVariable int gameId) {
                List<UserState> userStates = userStateRepository.findByGameId(gameId);

                return ResponseEntity.ok().body(userStates);
        }

        // @Operation(summary = "Start a game", description = "Start a game from
        // existing room.", tags = { "game",
        // "post" })
        // @ApiResponse(responseCode = "200", content = {
        // @Content(schema = @Schema(implementation = Game.class), mediaType =
        // "application/json") })
        // @ApiResponse(responseCode = "404", content = {
        // @Content(schema = @Schema()) })
        @PostMapping("/{gameId}/roll")
        public ResponseEntity<GameState> rollInAGame(

                        @Parameter(description = "Room id of the game to be started.", example = "1") @PathVariable int gameId,
                        @RequestBody RollInfo rollInfo) {

                GameState gameState = gameStateRepository.findByGameId(gameId).get();

                if (gameState.getTurnState() != TurnState.ROLL) {
                        return ResponseEntity.badRequest().build();
                }

                if (!gameState.getTurnUser().getId().equals(rollInfo.getUserId())) {
                        return ResponseEntity.badRequest().build();
                }

                gameState.setDice1(rollInfo.getDice1());
                gameState.setDice2(rollInfo.getDice2());
                gameState.setTurnState(TurnState.BUILD);

                gameStateRepository.save(gameState);

                return ResponseEntity.ok().body(gameState);
        }

        @GetMapping("/village-roads")
        public List<SettlementRoadMapping> getVillageRoadMappings() {
                SettlementRoadMapper villageRoadChecker = SettlementRoadMapper.getInstance();
                return villageRoadChecker.getSettlementRoadMappings();
        }
}
