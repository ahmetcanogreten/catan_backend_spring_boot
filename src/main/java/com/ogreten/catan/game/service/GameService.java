package com.ogreten.catan.game.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ogreten.catan.auth.domain.User;
import com.ogreten.catan.auth.repository.UserRepository;
import com.ogreten.catan.game.domain.Game;
import com.ogreten.catan.game.domain.GameState;
import com.ogreten.catan.game.domain.TurnState;
import com.ogreten.catan.game.domain.UserState;
import com.ogreten.catan.game.repository.GameRepository;
import com.ogreten.catan.game.repository.GameStateRepository;
import com.ogreten.catan.game.repository.UserStateRepository;
import com.ogreten.catan.game.schema.UserOptions;
import com.ogreten.catan.game.util.ResourceSettlementMapper;
import com.ogreten.catan.game.util.SettlementRoadMapper;
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

    public GameService(RoomRepository roomRepository, GameRepository gameRepository,
            GameStateRepository gameStateRepository, UserStateRepository userStateRepository,
            UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.gameRepository = gameRepository;
        this.gameStateRepository = gameStateRepository;
        this.userStateRepository = userStateRepository;
        this.userRepository = userRepository;
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

        return Optional.of(game);
    }

    public Optional<Game> getGame(
            int gameId) {
        Optional<Game> optionalGame = gameRepository.findById(gameId);

        return optionalGame;
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

        List<Integer> availableRoadsForTurnUser = new ArrayList<>();

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

        UserOptions userOptions = new UserOptions();

        userOptions.setAvailableRoads(availableRoadsForTurnUser);
        userOptions.setAvailableSettlements(availableSettlementsForTurnUser);
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

        gameState.setDice1(dice1);
        gameState.setDice2(dice2);

        final int diceSum = dice1 + dice2;

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
    }

}
