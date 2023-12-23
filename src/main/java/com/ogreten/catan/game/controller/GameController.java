package com.ogreten.catan.game.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ogreten.catan.auth.domain.User;
import com.ogreten.catan.auth.repository.UserRepository;
import com.ogreten.catan.game.domain.Game;
import com.ogreten.catan.game.domain.GameState;
import com.ogreten.catan.game.domain.TurnState;
import com.ogreten.catan.game.domain.UserState;
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
import java.util.UUID;

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

        UserRepository userRepository;

        public GameController(RoomRepository roomRepository, GameRepository gameRepository,
                        GameStateRepository gameStateRepository,
                        UserStateRepository userStateRepository,
                        UserRepository userRepository) {
                this.roomRepository = roomRepository;
                this.gameRepository = gameRepository;
                this.gameStateRepository = gameStateRepository;
                this.userStateRepository = userStateRepository;
                this.userRepository = userRepository;

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

                List<Integer> usersSettlementIndexes = new ArrayList<>();

                // Create user states
                for (User user : users) {
                        int randomUserSettlementIndex = (new Random().nextInt(54));
                        while (!SettlementRoadMapper.getInstance().isSettlementAtLeastTwoRoadAwayToOtherSettlements(
                                        randomUserSettlementIndex, usersSettlementIndexes)) {
                                randomUserSettlementIndex = (new Random().nextInt(54));
                        }
                        usersSettlementIndexes.add(randomUserSettlementIndex);

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

                final int diceSum = rollInfo.getDice1() + rollInfo.getDice2();

                final List<UserState> userStates = userStateRepository.findByGameId(gameId);

                Game game = gameState.getGame();
                final List<Resource> resources = game.getResources().stream()
                                .filter(resource -> resource.getNumber() == diceSum).toList();

                for (UserState userState : userStates) {

                        final Map<String, Object> userBuildings = userState.getBuildings();
                        final List<Integer> settlements = (List<Integer>) userBuildings.get("settlement");
                        final List<Integer> cities = (List<Integer>) userBuildings.get("city");

                        for (Integer settlement : settlements) {
                                final List<Integer> resourceIndexesNearSettlement = ResourceSettlementMapper
                                                .getInstance().getResourceIndexesNearSettlement(settlement);

                                for (Integer resourceIndex : resourceIndexesNearSettlement) {
                                        Optional<Resource> resource = resources.stream()
                                                        .filter(eachResource -> eachResource
                                                                        .getIndex() == resourceIndex)
                                                        .findFirst();

                                        if (resource.isEmpty()) {
                                                continue;
                                        }

                                        switch (resource.get().getType()) {
                                                case "hills":
                                                        userState.setNumberOfBrick(userState.getNumberOfBrick() + 1);
                                                        break;
                                                case "forest":
                                                        userState.setNumberOfLumber(
                                                                        userState.getNumberOfLumber() + 1);
                                                        break;
                                                case "mountains":
                                                        userState.setNumberOfOre(
                                                                        userState.getNumberOfOre() + 1);
                                                        break;
                                                case "fields":
                                                        userState.setNumberOfGrain(
                                                                        userState.getNumberOfGrain() + 1);
                                                        break;
                                                case "pasture":
                                                        userState.setNumberOfWool(
                                                                        userState.getNumberOfWool() + 1);
                                                        break;
                                                default:
                                                        break;
                                        }
                                }
                        }

                        for (Integer city : cities) {
                                final List<Integer> resourceIndexesNearCity = ResourceSettlementMapper
                                                .getInstance().getResourceIndexesNearSettlement(city);

                                for (Integer resourceIndex : resourceIndexesNearCity) {
                                        Optional<Resource> resource = resources.stream()
                                                        .filter(eachResource -> eachResource
                                                                        .getIndex() == resourceIndex)
                                                        .findFirst();

                                        if (resource.isEmpty()) {
                                                continue;
                                        }

                                        switch (resource.get().getType()) {
                                                case "hills":
                                                        userState.setNumberOfBrick(userState.getNumberOfBrick() + 2);
                                                        break;
                                                case "forest":
                                                        userState.setNumberOfLumber(
                                                                        userState.getNumberOfLumber() + 2);
                                                        break;
                                                case "mountains":
                                                        userState.setNumberOfOre(
                                                                        userState.getNumberOfOre() + 2);
                                                        break;
                                                case "fields":
                                                        userState.setNumberOfGrain(
                                                                        userState.getNumberOfGrain() + 2);
                                                        break;
                                                case "pasture":
                                                        userState.setNumberOfWool(
                                                                        userState.getNumberOfWool() + 2);
                                                        break;
                                                default:
                                                        break;
                                        }
                                }
                        }

                        int numberOfBrick = userState.getNumberOfBrick();
                        int numberOfLumber = userState.getNumberOfLumber();
                        int numberOfOre = userState.getNumberOfOre();
                        int numberOfGrain = userState.getNumberOfGrain();
                        int numberOfWool = userState.getNumberOfWool();

                        userState.setNumberOfBrick(numberOfBrick);
                        userState.setNumberOfLumber(numberOfLumber);
                        userState.setNumberOfOre(numberOfOre);
                        userState.setNumberOfGrain(numberOfGrain);
                        userState.setNumberOfWool(numberOfWool);

                        userStateRepository.save(userState);
                }

                gameState.setTurnState(TurnState.BUILD);

                gameStateRepository.save(gameState);

                return ResponseEntity.ok().body(gameState);
        }

        @PostMapping("/{gameId}/end-turn")
        public ResponseEntity<GameState> endTurnInAGame(

                        @Parameter(description = "Room id of the game to be started.", example = "1") @PathVariable int gameId,
                        @RequestParam String userId) {

                GameState gameState = gameStateRepository.findByGameId(gameId).get();
                Game game = gameState.getGame();

                if (gameState.getTurnState() != TurnState.BUILD) {
                        return ResponseEntity.badRequest().build();
                }

                if (!gameState.getTurnUser().getId().equals(UUID.fromString(userId))) {
                        return ResponseEntity.badRequest().build();
                }

                int indexOfCurrentTurnUser = game.getUsersCycle().indexOf(userId);
                int indexOfNextTurnUser = (indexOfCurrentTurnUser + 1) % game.getUsersCycle().size();

                String idOfNextTurnUser = game.getUsersCycle().get(indexOfNextTurnUser);
                User nextTurnUser = userRepository.findById(UUID.fromString(idOfNextTurnUser)).get();

                gameState.setTurnUser(nextTurnUser);
                gameState.setTurnState(TurnState.ROLL);
                gameStateRepository.save(gameState);

                return ResponseEntity.ok().body(gameState);
        }
}
