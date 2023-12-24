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
import com.ogreten.catan.game.schema.BuildInfo;
import com.ogreten.catan.game.schema.RollInfo;
import com.ogreten.catan.game.util.ResourceSettlementMapper;
import com.ogreten.catan.game.util.SettlementRoadMapper;
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

import java.util.stream.Stream;
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

                        userState.setRoads(List.of(
                                        randomUserRoadIndex));
                        userState.setSettlements(List.of(randomUserSettlementIndex));
                        userState.setCities(List.of());

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

                        final List<Integer> settlements = userState.getSettlements();
                        final List<Integer> cities = userState.getCities();
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

                List<Integer> othersSettlements = new ArrayList<>();
                List<Integer> othersCities = new ArrayList<>();
                List<Integer> othersRoads = new ArrayList<>();

                for (UserState userState : userStates) {
                        if (userState.getUser().getId().equals(gameState.getTurnUser().getId())) {
                                continue;
                        }

                        final List<Integer> settlements = userState.getSettlements();
                        final List<Integer> cities = userState.getCities();
                        final List<Integer> roads = userState.getRoads();

                        othersSettlements.addAll(settlements);
                        othersCities.addAll(cities);
                        othersRoads.addAll(roads);
                }

                final UserState turnUserState = userStateRepository
                                .findByGameIdAndUserId(gameId, gameState.getTurnUser().getId()).get();

                final List<Integer> turnUserSettlements = turnUserState.getSettlements();
                final List<Integer> turnUserCities = turnUserState.getCities();
                final List<Integer> turnUserRoads = turnUserState.getRoads();

                List<Integer> availableRoadsForTurnUser = new ArrayList<>();

                // for (Integer settlement : turnUserSettlements) {
                // final List<Integer> roadsOfSettlement = SettlementRoadMapper.getInstance()
                // .getRoadsOfVillage(settlement);

                // for (Integer road : roadsOfSettlement) {
                // if (!othersRoads.contains(road) && !turnUserRoads.contains(road)) {
                // availableRoadsForTurnUser.add(road);
                // }
                // }
                // }

                final List<Integer> allSettlementPlacesOfTurnUser = new ArrayList<>();
                for (Integer road : turnUserRoads) {
                        final List<Integer> settlementsOfRoad = SettlementRoadMapper.getInstance()
                                        .getVillageOfRoads(road);
                        allSettlementPlacesOfTurnUser.addAll(settlementsOfRoad);
                }

                for (Integer settlement : allSettlementPlacesOfTurnUser) {
                        final List<Integer> roadsOfSettlement = SettlementRoadMapper.getInstance()
                                        .getRoadsOfVillage(settlement);

                        for (Integer road : roadsOfSettlement) {
                                if (!othersRoads.contains(road) && !turnUserRoads.contains(road)) {
                                        availableRoadsForTurnUser.add(road);
                                }
                        }
                }

                List<Integer> availableSettlementsForTurnUser = new ArrayList<>();

                List<Integer> allSettlementsAndCities = new ArrayList<>();

                allSettlementsAndCities.addAll(othersSettlements);
                allSettlementsAndCities.addAll(othersCities);
                allSettlementsAndCities.addAll(turnUserSettlements);
                allSettlementsAndCities.addAll(turnUserCities);

                for (Integer road : turnUserRoads) {
                        final List<Integer> settlementsOfRoad = SettlementRoadMapper.getInstance()
                                        .getVillageOfRoads(road);

                        for (Integer settlement : settlementsOfRoad) {
                                if (!othersSettlements.contains(settlement) &&
                                                !othersCities.contains(settlement) &&
                                                !turnUserSettlements.contains(settlement)
                                                && !turnUserCities.contains(settlement)

                                                && SettlementRoadMapper.getInstance()
                                                                .isSettlementAtLeastTwoRoadAwayToOtherSettlements(
                                                                                settlement,
                                                                                allSettlementsAndCities)) {
                                        availableSettlementsForTurnUser.add(settlement);
                                }
                        }
                }

                List<Integer> availableCitiesForTurnUser = new ArrayList<>();
                availableCitiesForTurnUser.addAll(turnUserSettlements);

                gameState.setAvailableSettlementsForTurnUser(availableSettlementsForTurnUser);
                gameState.setAvailableRoadsForTurnUser(availableRoadsForTurnUser);
                gameState.setAvailableCitiesForTurnUser(availableCitiesForTurnUser);

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

        @PostMapping("/{gameId}/build-road")
        public ResponseEntity<GameState> buildRoad(

                        @Parameter(description = "Room id of the game to be started.", example = "1") @PathVariable int gameId,
                        @RequestBody BuildInfo buildInfo) {

                GameState gameState = gameStateRepository.findByGameId(gameId).get();

                if (gameState.getTurnState() != TurnState.BUILD) {
                        return ResponseEntity.badRequest().build();
                }

                final String userId = buildInfo.getUserId();

                if (!gameState.getTurnUser().getId().equals(UUID.fromString(userId))) {
                        return ResponseEntity.badRequest().build();
                }

                final UserState userState = userStateRepository.findByGameIdAndUserId(gameId, UUID.fromString(userId))
                                .get();

                final int numberOfLumber = userState.getNumberOfLumber();
                final int numberOfBrick = userState.getNumberOfBrick();

                if (numberOfLumber < 1 || numberOfBrick < 1) {
                        return ResponseEntity.badRequest().build();
                }

                final int roadIndex = buildInfo.getIndex();

                final List<Integer> availableRoadsForTurnUser = gameState.getAvailableRoadsForTurnUser();

                if (!availableRoadsForTurnUser.contains(roadIndex)) {
                        return ResponseEntity.badRequest().build();
                }

                final List<Integer> roads = userState.getRoads();
                roads.add(roadIndex);

                userState.setNumberOfLumber(numberOfLumber - 1);
                userState.setNumberOfBrick(numberOfBrick - 1);

                final List<Integer> updatedRoads = new ArrayList<>(roads);

                userState.setRoads(updatedRoads);

                userStateRepository.save(userState);

                final List<Integer> updatedAvailableRoadsForTurnUser = new ArrayList<>(
                                availableRoadsForTurnUser);
                updatedAvailableRoadsForTurnUser.remove(Integer.valueOf(roadIndex));

                gameState.setAvailableRoadsForTurnUser(updatedAvailableRoadsForTurnUser);

                gameStateRepository.save(gameState);

                return ResponseEntity.ok().body(gameState);
        }

        @PostMapping("/{gameId}/build-settlement")
        public ResponseEntity<GameState> buildSettlement(

                        @Parameter(description = "Room id of the game to be started.", example = "1") @PathVariable int gameId,
                        @RequestBody BuildInfo buildInfo) {

                GameState gameState = gameStateRepository.findByGameId(gameId).get();

                if (gameState.getTurnState() != TurnState.BUILD) {
                        return ResponseEntity.badRequest().build();
                }

                final String userId = buildInfo.getUserId();

                if (!gameState.getTurnUser().getId().equals(UUID.fromString(userId))) {
                        return ResponseEntity.badRequest().build();
                }

                final UserState userState = userStateRepository.findByGameIdAndUserId(gameId, UUID.fromString(userId))
                                .get();

                final int numberOfLumber = userState.getNumberOfLumber();
                final int numberOfBrick = userState.getNumberOfBrick();
                final int numberOfGrain = userState.getNumberOfGrain();
                final int numberOfWool = userState.getNumberOfWool();

                if (numberOfLumber < 1 || numberOfBrick < 1 || numberOfGrain < 1 || numberOfWool < 1) {
                        return ResponseEntity.badRequest().build();
                }

                final int settlementIndex = buildInfo.getIndex();

                final List<Integer> availableSettlementsForTurnUser = gameState.getAvailableSettlementsForTurnUser();

                if (!availableSettlementsForTurnUser.contains(settlementIndex)) {
                        return ResponseEntity.badRequest().build();
                }

                final List<Integer> settlements = userState.getSettlements();
                settlements.add(settlementIndex);

                userState.setNumberOfLumber(numberOfLumber - 1);
                userState.setNumberOfBrick(numberOfBrick - 1);
                userState.setNumberOfGrain(numberOfGrain - 1);
                userState.setNumberOfWool(numberOfWool - 1);

                final List<Integer> updatedSettlements = new ArrayList<>(settlements);

                userState.setSettlements(updatedSettlements);

                userStateRepository.save(userState);

                final List<Integer> updatedAvailableSettlementsForTurnUser = new ArrayList<>(
                                availableSettlementsForTurnUser);
                updatedAvailableSettlementsForTurnUser.remove(Integer.valueOf(settlementIndex));

                gameState.setAvailableRoadsForTurnUser(updatedAvailableSettlementsForTurnUser);

                gameStateRepository.save(gameState);

                return ResponseEntity.ok().body(gameState);
        }

        @PostMapping("/{gameId}/build-city")
        public ResponseEntity<GameState> buildCity(

                        @Parameter(description = "Room id of the game to be started.", example = "1") @PathVariable int gameId,
                        @RequestBody BuildInfo buildInfo) {

                GameState gameState = gameStateRepository.findByGameId(gameId).get();

                if (gameState.getTurnState() != TurnState.BUILD) {
                        return ResponseEntity.badRequest().build();
                }

                final String userId = buildInfo.getUserId();

                if (!gameState.getTurnUser().getId().equals(UUID.fromString(userId))) {
                        return ResponseEntity.badRequest().build();
                }

                final UserState userState = userStateRepository.findByGameIdAndUserId(gameId, UUID.fromString(userId))
                                .get();

                final int numberOfGrain = userState.getNumberOfGrain();
                final int numberOfOre = userState.getNumberOfOre();

                if (numberOfGrain < 2 || numberOfOre < 3) {
                        return ResponseEntity.badRequest().build();
                }

                final int cityIndex = buildInfo.getIndex();

                final List<Integer> availableCitiesForTurnUser = gameState.getAvailableCitiesForTurnUser();

                if (!availableCitiesForTurnUser.contains(cityIndex)) {
                        return ResponseEntity.badRequest().build();
                }

                final List<Integer> settlements = userState.getSettlements();
                settlements.remove(settlements.indexOf(cityIndex));

                final List<Integer> cities = userState.getCities();
                cities.add(cityIndex);

                userState.setNumberOfGrain(numberOfGrain - 2);
                userState.setNumberOfOre(numberOfOre - 3);

                final List<Integer> updatedSettlements = new ArrayList<>(settlements);
                userState.setSettlements(updatedSettlements);

                final List<Integer> updatedCities = new ArrayList<>(cities);
                userState.setCities(updatedCities);

                userStateRepository.save(userState);

                final List<Integer> updatedAvailableCitiesForTurnUser = new ArrayList<>(
                                availableCitiesForTurnUser);
                updatedAvailableCitiesForTurnUser.remove(updatedAvailableCitiesForTurnUser.indexOf(cityIndex));

                gameState.setAvailableCitiesForTurnUser(updatedAvailableCitiesForTurnUser);

                gameStateRepository.save(gameState);

                return ResponseEntity.ok().body(gameState);
        }

}
