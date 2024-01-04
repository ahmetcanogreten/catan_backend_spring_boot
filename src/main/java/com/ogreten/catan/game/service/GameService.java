package com.ogreten.catan.game.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ogreten.catan.auth.domain.User;
import com.ogreten.catan.auth.repository.UserRepository;
import com.ogreten.catan.game.domain.Game;
import com.ogreten.catan.game.domain.GameLog;
import com.ogreten.catan.game.domain.GameState;
import com.ogreten.catan.game.domain.TurnState;
import com.ogreten.catan.game.domain.UserState;
import com.ogreten.catan.game.repository.GameLogRepository;
import com.ogreten.catan.game.repository.GameRepository;
import com.ogreten.catan.game.repository.GameStateRepository;
import com.ogreten.catan.game.repository.UserStateRepository;
import com.ogreten.catan.game.schema.UserOptions;
import com.ogreten.catan.game.schema.UserWithInGamePoints;
import com.ogreten.catan.game.util.ResourceSettlementMapper;
import com.ogreten.catan.game.util.SettlementRoadMapper;
import com.ogreten.catan.leaderboard.domain.UserEarnedPoints;
import com.ogreten.catan.leaderboard.repository.LeaderboardRepository;
import com.ogreten.catan.room.domain.Resource;
import com.ogreten.catan.room.domain.Room;
import com.ogreten.catan.room.repository.RoomRepository;

@Service
public class GameService {

    RoomRepository roomRepository;
    GameRepository gameRepository;
    GameStateRepository gameStateRepository;
    UserStateRepository userStateRepository;
    UserRepository userRepository;
    GameLogRepository gameLogRepository;
    LeaderboardRepository leaderboardRepository;

    public GameService(RoomRepository roomRepository, GameRepository gameRepository,
            GameStateRepository gameStateRepository, UserStateRepository userStateRepository,
            UserRepository userRepository,
            GameLogRepository gameLogRepository,
            LeaderboardRepository leaderboardRepository) {
        this.roomRepository = roomRepository;
        this.gameRepository = gameRepository;
        this.gameStateRepository = gameStateRepository;
        this.userStateRepository = userStateRepository;
        this.userRepository = userRepository;
        this.gameLogRepository = gameLogRepository;
        this.leaderboardRepository = leaderboardRepository;
    }

    public Optional<Game> createGame(int roomId) {
        Optional<Room> optionalRoom = roomRepository.findById(roomId);

        if (optionalRoom.isEmpty()) {
            return Optional.empty();
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

            int numberOfBrick = 0;
            int numberOfLumber = 0;
            int numberOfOre = 0;
            int numberOfGrain = 0;
            int numberOfWool = 0;

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

            userState.setRoads(List.of());
            userState.setSettlements(List.of());
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
                TurnState.CHOOSE_1);
        gameStateRepository.save(gameState);

        GameLog gameLog = new GameLog();
        gameLog.setGame(game);
        gameLog.setLog(room.getOwner().getFirstName() + " started the game.");
        gameLogRepository.save(gameLog);

        return Optional.of(game);
    }

    public Optional<Game> getGame(
            int gameId) {
        Optional<Game> optionalGame = gameRepository.findById(gameId);

        return optionalGame;
    }

    public Optional<Game> getGameByRoomId(
            int roomId) {
        return gameRepository.findByRoomId(roomId);

    }

    public Optional<GameState> getGameState(
            int gameId) {
        Optional<GameState> optionalGameState = gameStateRepository.findByGameId(gameId);

        return optionalGameState;
    }

    public List<UserState> getUserStatesInAGame(
            int gameId) {
        List<UserState> userStates = userStateRepository.findByGameId(gameId);

        return userStates;
    }

    public UserOptions getUserOptions(
            int gameId, UUID userId) {

        List<Integer> othersSettlements = new ArrayList<>();
        List<Integer> othersCities = new ArrayList<>();
        List<Integer> othersRoads = new ArrayList<>();

        final List<UserState> userStates = userStateRepository.findByGameId(gameId);

        for (UserState userState : userStates) {
            if (userState.getUser().getId().equals(userId)) {
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
                .findByGameIdAndUserId(gameId, userId).get();

        final List<Integer> turnUserSettlements = turnUserState.getSettlements();
        final List<Integer> turnUserCities = turnUserState.getCities();
        final List<Integer> turnUserRoads = turnUserState.getRoads();

        // Get available roads for turn user
        List<Integer> availableRoadsForTurnUser = new ArrayList<>();

        List<Integer> allSettlementPlacesOfTurnUser = new ArrayList<>();
        for (Integer road : turnUserRoads) {
            final List<Integer> settlementsOfRoad = SettlementRoadMapper.getInstance()
                    .getVillageOfRoads(road);
            allSettlementPlacesOfTurnUser.addAll(settlementsOfRoad);
        }

        allSettlementPlacesOfTurnUser.addAll(turnUserSettlements);
        for (Integer settlement : allSettlementPlacesOfTurnUser) {
            final List<Integer> roadsOfSettlement = SettlementRoadMapper.getInstance()
                    .getRoadsOfVillage(settlement);

            for (Integer road : roadsOfSettlement) {
                if (!othersRoads.contains(road) && !turnUserRoads.contains(road)) {
                    availableRoadsForTurnUser.add(road);
                }
            }
        }

        // Get available settlements for turn user
        List<Integer> allSettlementsAndCities = new ArrayList<>();

        allSettlementsAndCities.addAll(othersSettlements);
        allSettlementsAndCities.addAll(othersCities);
        allSettlementsAndCities.addAll(turnUserSettlements);
        allSettlementsAndCities.addAll(turnUserCities);

        List<Integer> mayBeAvailableSettlementsForTurnUser = SettlementRoadMapper.getInstance()
                .getAllVillagesWhileOtherVillages(
                        allSettlementsAndCities);

        List<Integer> availableSettlementsForTurnUser = new ArrayList<>();

        for (Integer eachRoad : turnUserRoads) {
            final List<Integer> settlementsOfRoad = SettlementRoadMapper.getInstance()
                    .getVillageOfRoads(eachRoad);

            for (Integer eachSettlement : settlementsOfRoad) {
                if (mayBeAvailableSettlementsForTurnUser.contains(eachSettlement)) {
                    availableSettlementsForTurnUser.add(eachSettlement);
                }
            }
        }

        // Get available cities for turn user
        List<Integer> availableCitiesForTurnUser = new ArrayList<>(turnUserSettlements);

        UserOptions userOptions = new UserOptions();

        userOptions.setAvailableSettlements(availableSettlementsForTurnUser);
        userOptions.setAvailableRoads(availableRoadsForTurnUser);
        userOptions.setAvailableCities(availableCitiesForTurnUser);

        return userOptions;
    }

    public void rollDice(
            int gameId, int dice1, int dice2, UUID userId) {
        if (dice1 < 1 || dice1 > 6 || dice2 < 1 || dice2 > 6) {
            // TODO: Throw exception
            return;
        }

        Optional<GameState> optionalGameState = gameStateRepository.findByGameId(gameId);

        if (optionalGameState.isEmpty()) {
            // TODO: Throw exception
            return;
        }

        GameState gameState = optionalGameState.get();

        if (gameState.getTurnState() != TurnState.ROLL) {
            // TODO: Throw exception
            return;
        }

        if (!gameState.getTurnUser().getId().equals(userId)) {
            // TODO: Throw exception
            return;
        }

        GameLog gameLogDice = new GameLog();
        Game game = gameState.getGame();
        gameLogDice.setGame(game);
        User turnUser = gameState.getTurnUser();
        gameLogDice.setLog(turnUser.getFirstName() + " rolled the dice and got " + dice1 + " and " + dice2 + ".");
        gameLogRepository.save(gameLogDice);

        gameState.setDice1(dice1);
        gameState.setDice2(dice2);

        final int diceSum = dice1 + dice2;

        final List<UserState> userStates = userStateRepository.findByGameId(gameId);

        final List<Resource> resources = game.getResources().stream()
                .filter(resource -> resource.getNumber() == diceSum).toList();

        for (UserState userState : userStates) {

            final List<Integer> settlements = userState.getSettlements();
            final List<Integer> cities = userState.getCities();

            int numberOfBrick = 0;
            int numberOfLumber = 0;
            int numberOfOre = 0;
            int numberOfGrain = 0;
            int numberOfWool = 0;

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
                            numberOfBrick += 2;
                            break;
                        case "forest":
                            numberOfLumber += 2;
                            break;
                        case "mountains":
                            numberOfOre += 2;
                            break;
                        case "fields":
                            numberOfGrain += 2;
                            break;
                        case "pasture":
                            numberOfWool += 2;
                            break;
                        default:
                            break;
                    }
                }
            }

            GameLog gameLog = new GameLog();
            gameLog.setGame(game);
            User user = userState.getUser();
            gameLog.setLog(user.getFirstName() + " got " + numberOfBrick + " brick, " + numberOfLumber + " lumber, "
                    + numberOfOre + " ore, " + numberOfGrain + " grain, " + numberOfWool + " wool.");
            gameLogRepository.save(gameLog);

            userState.setNumberOfBrick(numberOfBrick + userState.getNumberOfBrick());
            userState.setNumberOfLumber(numberOfLumber + userState.getNumberOfLumber());
            userState.setNumberOfOre(numberOfOre + userState.getNumberOfOre());
            userState.setNumberOfGrain(numberOfGrain + userState.getNumberOfGrain());
            userState.setNumberOfWool(numberOfWool + userState.getNumberOfWool());

            userStateRepository.save(userState);
        }

        gameState.setTurnState(TurnState.BUILD);

        gameStateRepository.save(gameState);

    }

    public void endTurn(
            int gameId, UUID userId) {

        Optional<GameState> optionalGameState = gameStateRepository.findByGameId(gameId);

        if (optionalGameState.isEmpty()) {
            // TODO: Throw exception
            return;
        }

        GameState gameState = optionalGameState.get();
        Game game = gameState.getGame();

        // Check if game is finished
        List<UserWithInGamePoints> userWithInGamePointsList = getUsersPoints(gameId);

        for (UserWithInGamePoints userWithInGamePoints : userWithInGamePointsList) {
            if (userWithInGamePoints.getId().equals(userId) &&
                    userWithInGamePoints.getPoints() >= 8) {
                game.setFinishedAt(Instant.now());
                gameRepository.save(game);

                for (UserWithInGamePoints userWithInGamePoints1 : userWithInGamePointsList) {
                    User user = userRepository.findById(userWithInGamePoints1.getId()).get();
                    UserEarnedPoints userEarnedPoints = new UserEarnedPoints();
                    userEarnedPoints.setUser(user);
                    userEarnedPoints.setPoints(userWithInGamePoints1.getPoints());
                    userEarnedPoints.setAt(Instant.now());
                    leaderboardRepository.save(userEarnedPoints);
                }
                return;
            }
        }

        if (gameState.getTurnState() != TurnState.BUILD) {
            // TODO: Throw exception
            return;
        }

        if (!gameState.getTurnUser().getId().equals(userId)) {
            // TODO: Throw exception
            return;
        }

        int indexOfCurrentTurnUser = game.getUsersCycle().indexOf(userId.toString());
        int indexOfNextTurnUser = (indexOfCurrentTurnUser + 1) % game.getUsersCycle().size();

        String idOfNextTurnUser = game.getUsersCycle().get(indexOfNextTurnUser);
        User nextTurnUser = userRepository.findById(UUID.fromString(idOfNextTurnUser)).get();

        GameLog gameLog = new GameLog();
        gameLog.setGame(game);
        User turnUser = gameState.getTurnUser();
        gameLog.setLog(turnUser.getFirstName() + " ended the turn.");
        gameLogRepository.save(gameLog);

        gameState.setTurnUser(nextTurnUser);
        gameState.setTurnState(TurnState.ROLL);
        gameStateRepository.save(gameState);

    }

    public void buildRoad(
            int gameId, int roadIndex, UUID userId) {
        GameState gameState = gameStateRepository.findByGameId(gameId).get();

        if (gameState.getTurnState() != TurnState.BUILD) {
            // TODO: Throw exception
            return;
        }

        if (!gameState.getTurnUser().getId().equals(userId)) {
            // TODO: Throw exception
            return;
        }

        final UserState userState = userStateRepository.findByGameIdAndUserId(gameId, userId)
                .get();

        final int numberOfLumber = userState.getNumberOfLumber();
        final int numberOfBrick = userState.getNumberOfBrick();

        if (numberOfLumber < 1 || numberOfBrick < 1) {
            // TODO: Throw exception
            return;
        }

        final UserOptions userOptions = getUserOptions(gameId, userId);
        final List<Integer> availableRoadsForTurnUser = userOptions.getAvailableRoads();

        if (!availableRoadsForTurnUser.contains(roadIndex)) {
            // TODO: Throw exception
            return;
        }

        final List<Integer> roads = userState.getRoads();
        roads.add(roadIndex);

        userState.setNumberOfLumber(numberOfLumber - 1);
        userState.setNumberOfBrick(numberOfBrick - 1);

        final List<Integer> updatedRoads = new ArrayList<>(roads);

        userState.setRoads(updatedRoads);

        userStateRepository.save(userState);

        GameLog gameLog = new GameLog();
        Game game = gameState.getGame();
        gameLog.setGame(game);
        User turnUser = gameState.getTurnUser();
        gameLog.setLog(turnUser.getFirstName() + " built a road.");
        gameLogRepository.save(gameLog);
    }

    public void buildSettlement(
            int gameId, int settlementIndex, UUID userId) {
        GameState gameState = gameStateRepository.findByGameId(gameId).get();

        if (gameState.getTurnState() != TurnState.BUILD) {
            // TODO: Throw exception
            return;
        }

        if (!gameState.getTurnUser().getId().equals(userId)) {
            // TODO: Throw exception
            return;
        }

        final UserState userState = userStateRepository.findByGameIdAndUserId(gameId, userId)
                .get();

        final int numberOfLumber = userState.getNumberOfLumber();
        final int numberOfBrick = userState.getNumberOfBrick();
        final int numberOfGrain = userState.getNumberOfGrain();
        final int numberOfWool = userState.getNumberOfWool();

        if (numberOfLumber < 1 || numberOfBrick < 1 || numberOfGrain < 1 || numberOfWool < 1) {
            // TODO: Throw exception
            return;
        }

        final UserOptions userOptions = getUserOptions(gameId, userId);
        final List<Integer> availableSettlementsForTurnUser = userOptions.getAvailableSettlements();

        if (!availableSettlementsForTurnUser.contains(settlementIndex)) {
            // TODO: Throw exception
            return;
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

        GameLog gameLog = new GameLog();
        Game game = gameState.getGame();
        gameLog.setGame(game);
        User turnUser = gameState.getTurnUser();
        gameLog.setLog(turnUser.getFirstName() + " built a settlement.");
        gameLogRepository.save(gameLog);

    }

    public void buildCity(
            int gameId, int cityIndex, UUID userId) {

        GameState gameState = gameStateRepository.findByGameId(gameId).get();

        if (gameState.getTurnState() != TurnState.BUILD) {
            // TODO: Throw exception
            return;
        }

        if (!gameState.getTurnUser().getId().equals(userId)) {
            // TODO: Throw exception
            return;
        }

        final UserState userState = userStateRepository.findByGameIdAndUserId(gameId, userId)
                .get();

        final int numberOfGrain = userState.getNumberOfGrain();
        final int numberOfOre = userState.getNumberOfOre();

        if (numberOfGrain < 2 || numberOfOre < 3) {
            // TODO: Throw exception
            return;
        }

        final UserOptions userOptions = getUserOptions(gameId, userId);

        final List<Integer> availableCitiesForTurnUser = userOptions.getAvailableCities();

        if (!availableCitiesForTurnUser.contains(cityIndex)) {
            // TODO: Throw exception
            return;
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

        GameLog gameLog = new GameLog();
        Game game = gameState.getGame();
        gameLog.setGame(game);
        User turnUser = gameState.getTurnUser();
        gameLog.setLog(turnUser.getFirstName() + " built a city.");
        gameLogRepository.save(gameLog);
    }

    public void chooseSettlementAndRoadForBot(
            int gameId, UUID userId) {

        GameState gameState = gameStateRepository.findByGameId(gameId).get();

        if (gameState.getTurnState() != TurnState.CHOOSE_1 && gameState.getTurnState() != TurnState.CHOOSE_2) {
            // TODO : Throw exception
            return;
        }

        if (!gameState.getTurnUser().getId().equals(userId)) {
            // TODO : Throw exception
            return;
        }

        GameLog gameLog = new GameLog();
        Game game = gameState.getGame();
        gameLog.setGame(game);
        User turnUser = gameState.getTurnUser();
        gameLog.setLog(turnUser.getFirstName() + " chose a settlement and a road.");
        gameLogRepository.save(gameLog);

        List<Integer> allSettlements = new ArrayList<>();

        final List<UserState> userStates = userStateRepository.findByGameId(gameId);

        for (UserState userState : userStates) {
            final List<Integer> settlements = userState.getSettlements();
            allSettlements.addAll(settlements);
        }

        List<Integer> availableSettlementsForTurnUser = SettlementRoadMapper.getInstance()
                .getAllVillagesWhileOtherVillages(
                        allSettlements);

        final int randomSettlementIndex = availableSettlementsForTurnUser.get(
                new Random().nextInt(availableSettlementsForTurnUser.size()));

        final List<Integer> availableRoadsForTurnUser = SettlementRoadMapper.getInstance()
                .getRoadsOfVillage(randomSettlementIndex);

        final int randomRoadIndex = availableRoadsForTurnUser.get(
                new Random().nextInt(availableRoadsForTurnUser.size()));

        final UserState userState = userStateRepository.findByGameIdAndUserId(gameId, userId)
                .get();

        final List<Integer> settlements = userState.getSettlements();
        settlements.add(randomSettlementIndex);

        final List<Integer> roads = userState.getRoads();
        roads.add(randomRoadIndex);

        userState.setSettlements(settlements);
        userState.setRoads(roads);

        final int whichIndexAmI = gameState.getGame().getUsersCycle().indexOf(userId.toString());

        final int indexOfNextTurnUser = (whichIndexAmI + 1) % gameState.getGame().getUsersCycle().size();

        final String idOfNextTurnUser = gameState.getGame().getUsersCycle().get(indexOfNextTurnUser);

        final User nextTurnUser = userRepository.findById(UUID.fromString(idOfNextTurnUser)).get();

        gameState.setTurnUser(nextTurnUser);

        if (gameState.getTurnState() == TurnState.CHOOSE_2) {
            int numberOfBrick = 0;
            int numberOfLumber = 0;
            int numberOfOre = 0;
            int numberOfGrain = 0;
            int numberOfWool = 0;

            final List<Integer> resourceIndexesNearSettlement = ResourceSettlementMapper.getInstance()
                    .getResourceIndexesNearSettlement(randomSettlementIndex);

            for (Integer resourceIndex : resourceIndexesNearSettlement) {
                Resource resource = game.getResources().stream()
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

            userState.setNumberOfBrick(numberOfBrick);
            userState.setNumberOfLumber(
                    numberOfLumber);
            userState.setNumberOfOre(
                    numberOfOre);
            userState.setNumberOfGrain(
                    numberOfGrain);
            userState.setNumberOfWool(
                    numberOfWool);

            userStateRepository.save(userState);
        }

        if (whichIndexAmI == gameState.getGame().getUsersCycle().size() - 1) {
            if (gameState.getTurnState() == TurnState.CHOOSE_1) {
                gameState.setTurnState(TurnState.CHOOSE_2);
            } else {
                gameState.setTurnState(TurnState.ROLL);

            }
        }

        gameStateRepository.save(gameState);
        userStateRepository.save(userState);

    }

    public Map<Integer, List<Integer>> getAvailableSettlementsAndRoadsToChoose(
            int gameId, UUID userId) {

        GameState gameState = gameStateRepository.findByGameId(gameId).get();

        if (gameState.getTurnState() != TurnState.CHOOSE_1 && gameState.getTurnState() != TurnState.CHOOSE_2) {
            // TODO : Throw exception
            return Map.of();
        }

        if (!gameState.getTurnUser().getId().equals(userId)) {
            // TODO : Throw exception
            return Map.of();
        }

        List<Integer> allBuiltSettlements = new ArrayList<>();

        final List<UserState> userStates = userStateRepository.findByGameId(gameId);

        for (UserState userState : userStates) {
            final List<Integer> settlements = userState.getSettlements();
            allBuiltSettlements.addAll(settlements);
        }

        List<Integer> availableSettlementsForTurnUser = SettlementRoadMapper.getInstance()
                .getAllVillagesWhileOtherVillages(
                        allBuiltSettlements);

        Map<Integer, List<Integer>> availableSettlementsForTurnUserWithRoads = new HashMap<>();

        for (Integer settlement : availableSettlementsForTurnUser) {
            final List<Integer> roadsOfSettlement = SettlementRoadMapper.getInstance()
                    .getRoadsOfVillage(settlement);

            availableSettlementsForTurnUserWithRoads.put(settlement, roadsOfSettlement);
        }

        return availableSettlementsForTurnUserWithRoads;
    }

    public void chooseSettlement(
            int gameId, UUID userId, int settlementIndex) {

        GameState gameState = gameStateRepository.findByGameId(gameId).get();

        if (gameState.getTurnState() != TurnState.CHOOSE_1 && gameState.getTurnState() != TurnState.CHOOSE_2) {
            // TODO : Throw exception
            return;
        }

        if (!gameState.getTurnUser().getId().equals(userId)) {
            // TODO : Throw exception
            return;
        }

        List<Integer> allSettlements = new ArrayList<>();

        final List<UserState> userStates = userStateRepository.findByGameId(gameId);

        for (UserState userState : userStates) {
            final List<Integer> settlements = userState.getSettlements();
            allSettlements.addAll(settlements);
        }

        List<Integer> availableSettlementsForTurnUser = SettlementRoadMapper.getInstance()
                .getAllVillagesWhileOtherVillages(
                        allSettlements);

        if (!availableSettlementsForTurnUser.contains(settlementIndex)) {
            // TODO : Throw exception
            return;
        }

        final UserState userState = userStateRepository.findByGameIdAndUserId(gameId, userId)
                .get();

        final List<Integer> settlements = userState.getSettlements();
        settlements.add(settlementIndex);

        userState.setSettlements(settlements);

        if (gameState.getTurnState() == TurnState.CHOOSE_2) {

            int numberOfBrick = 0;
            int numberOfLumber = 0;
            int numberOfOre = 0;
            int numberOfGrain = 0;
            int numberOfWool = 0;

            final List<Integer> resourceIndexesNearSettlement = ResourceSettlementMapper.getInstance()
                    .getResourceIndexesNearSettlement(settlementIndex);

            final Game game = gameState.getGame();

            for (Integer resourceIndex : resourceIndexesNearSettlement) {
                Resource resource = game.getResources().stream()
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

            userState.setNumberOfBrick(numberOfBrick);
            userState.setNumberOfLumber(
                    numberOfLumber);
            userState.setNumberOfOre(
                    numberOfOre);
            userState.setNumberOfGrain(
                    numberOfGrain);
            userState.setNumberOfWool(
                    numberOfWool);

            GameLog gameLog = new GameLog();
            gameLog.setGame(game);
            User turnUser = gameState.getTurnUser();
            gameLog.setLog(turnUser.getFirstName() + " chose a settlement. Got " + numberOfBrick + " brick, "
                    + numberOfLumber + " lumber, " + numberOfOre + " ore, " + numberOfGrain + " grain, " + numberOfWool
                    + " wool.");
            gameLogRepository.save(gameLog);
            userStateRepository.save(userState);
            return;
        }

        userStateRepository.save(userState);
        GameLog gameLog = new GameLog();
        Game game = gameState.getGame();
        gameLog.setGame(game);
        User turnUser = gameState.getTurnUser();
        gameLog.setLog(turnUser.getFirstName() + " chose a settlement.");
        gameLogRepository.save(gameLog);
    }

    public void chooseRoad(
            int gameId, UUID userId, int roadIndex) {

        GameState gameState = gameStateRepository.findByGameId(gameId).get();

        if (gameState.getTurnState() != TurnState.CHOOSE_1 && gameState.getTurnState() != TurnState.CHOOSE_2) {
            // TODO : Throw exception
            return;
        }

        if (!gameState.getTurnUser().getId().equals(userId)) {
            // TODO : Throw exception
            return;
        }

        GameLog gameLog = new GameLog();
        Game game = gameState.getGame();
        gameLog.setGame(game);
        User turnUser = gameState.getTurnUser();
        gameLog.setLog(turnUser.getFirstName() + " chose a road.");
        gameLogRepository.save(gameLog);

        UserOptions userOptions = getUserOptions(gameId, userId);

        final List<Integer> availableRoadsForTurnUser = userOptions.getAvailableRoads();

        if (!availableRoadsForTurnUser.contains(roadIndex)) {
            // TODO : Throw exception
            return;
        }

        final UserState userState = userStateRepository.findByGameIdAndUserId(gameId, userId)
                .get();

        final List<Integer> roads = userState.getRoads();
        roads.add(roadIndex);

        userState.setRoads(roads);

        final int whichIndexAmI = gameState.getGame().getUsersCycle().indexOf(userId.toString());

        final int indexOfNextTurnUser = (whichIndexAmI + 1) %
                gameState.getGame().getUsersCycle().size();

        final String idOfNextTurnUser = gameState.getGame().getUsersCycle().get(indexOfNextTurnUser);

        final User nextTurnUser = userRepository.findById(UUID.fromString(idOfNextTurnUser)).get();

        gameState.setTurnUser(nextTurnUser);

        if (whichIndexAmI == gameState.getGame().getUsersCycle().size() - 1) {
            if (gameState.getTurnState() == TurnState.CHOOSE_1) {
                gameState.setTurnState(TurnState.CHOOSE_2);
            } else {
                gameState.setTurnState(TurnState.ROLL);
            }
        }

        gameStateRepository.save(gameState);

    }

    public List<GameLog> getGameLogs(int gameId) {
        return gameLogRepository.findByGameId(gameId);
    }

    private Integer calculateLongestRoad(
            List<Integer> walkedRoads,
            List<Integer> allRoads) {

        List<Integer> neighbours = new ArrayList<>();

        if (walkedRoads.isEmpty()) {
            neighbours.addAll(allRoads);
        } else {
            int latestWalkedRoad = walkedRoads.get(walkedRoads.size() - 1);

            List<Integer> mayBeNeighbours = SettlementRoadMapper.getInstance().getRoadsOfRoad(latestWalkedRoad);

            List<Integer> mayBeNeighboursOfPrevious = List.of();

            if (walkedRoads.size() > 1) {
                int previousWalkedRoad = walkedRoads.get(walkedRoads.size() - 2);
                mayBeNeighboursOfPrevious = SettlementRoadMapper.getInstance().getRoadsOfRoad(previousWalkedRoad);
            }

            for (Integer mayBeNeighbour : mayBeNeighbours) {
                if (allRoads.contains(mayBeNeighbour) && !walkedRoads.contains(mayBeNeighbour) &&
                        !mayBeNeighboursOfPrevious.contains(mayBeNeighbour)) {
                    neighbours.add(mayBeNeighbour);
                }
            }
        }

        int longestRoad = 0;

        for (Integer neighbour : neighbours) {
            List<Integer> newWalkedRoads = new ArrayList<>(walkedRoads);
            newWalkedRoads.add(neighbour);

            List<Integer> newAllRoads = new ArrayList<>(allRoads);
            newAllRoads.removeAll(newWalkedRoads);

            int newLongestRoad = calculateLongestRoad(newWalkedRoads, newAllRoads);

            if (newLongestRoad > longestRoad) {
                longestRoad = newLongestRoad;
            }
        }

        return longestRoad + 1;
    }

    public List<UserWithInGamePoints> getUsersPoints(int gameId) {

        final List<UserState> userStates = userStateRepository.findByGameId(gameId);

        List<UserWithInGamePoints> usersWithPoints = new ArrayList<>();

        Map<UUID, Integer> longestRoads = new HashMap<>();

        for (UserState userState : userStates) {
            UserWithInGamePoints userWithPoints = new UserWithInGamePoints();
            userWithPoints.setId(userState.getUser().getId());

            int points = 0;

            final List<Integer> settlements = userState.getSettlements();
            final List<Integer> cities = userState.getCities();
            final List<Integer> roads = userState.getRoads();

            points += settlements.size();
            points += 2 * cities.size();

            // Calculate longest road
            int longestRoad = calculateLongestRoad(
                    List.of(),
                    roads) - 1;
            longestRoads.put(userState.getUser().getId(), longestRoad);

            userWithPoints.setPoints(points);
            usersWithPoints.add(userWithPoints);
        }

        // Calculate longest road points
        int maxLongestRoad = 0;
        UUID maxLongestRoadUserId = null;

        for (Map.Entry<UUID, Integer> entry : longestRoads.entrySet()) {
            if (entry.getValue() > maxLongestRoad) {
                maxLongestRoad = entry.getValue();
                maxLongestRoadUserId = entry.getKey();
            }
        }

        if (maxLongestRoad >= 5) {
            for (UserWithInGamePoints userWithInGamePoints : usersWithPoints) {
                if (userWithInGamePoints.getId().equals(maxLongestRoadUserId)) {
                    userWithInGamePoints.setPoints(userWithInGamePoints.getPoints() + 2);
                }
            }
        }

        return usersWithPoints;
    }
}
