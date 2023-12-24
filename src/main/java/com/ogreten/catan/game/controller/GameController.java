package com.ogreten.catan.game.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ogreten.catan.game.domain.Game;
import com.ogreten.catan.game.domain.GameState;
import com.ogreten.catan.game.domain.UserState;
import com.ogreten.catan.game.schema.BuildInfo;
import com.ogreten.catan.game.schema.RollInfo;
import com.ogreten.catan.game.schema.UserOptions;
import com.ogreten.catan.game.service.GameService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        GameService gameService;

        public GameController(GameService gameService) {
                this.gameService = gameService;
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

                Optional<Game> optionalGame = gameService.createGame(roomId);

                if (optionalGame.isEmpty()) {
                        return ResponseEntity.notFound().build();
                }
                return ResponseEntity.ok().body(optionalGame.get());
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
                Optional<Game> optionalGame = gameService.getGame(gameId);

                if (optionalGame.isEmpty()) {
                        return ResponseEntity.notFound().build();
                }

                return ResponseEntity.ok().body(optionalGame.get());

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

                Optional<GameState> optionalGameState = gameService.getGameState(gameId);

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
                List<UserState> userStates = gameService.getUserStatesInAGame(gameId);

                return ResponseEntity.ok().body(userStates);
        }

        @Operation(summary = "Start a game", description = "Start a game from existing room.", tags = { "game",
                        "post" })
        @ApiResponse(responseCode = "200", content = {
                        @Content(schema = @Schema(implementation = Game.class), mediaType = "application/json") })
        @ApiResponse(responseCode = "404", content = {
                        @Content(schema = @Schema()) })
        @PostMapping("/{gameId}/roll")
        public ResponseEntity<String> rollInAGame(

                        @Parameter(description = "Room id of the game to be started.", example = "1") @PathVariable int gameId,
                        @RequestBody RollInfo rollInfo) {

                final int dice1 = rollInfo.getDice1();
                final int dice2 = rollInfo.getDice2();

                final UUID userId = rollInfo.getUserId();

                gameService.rollDice(gameId, dice1, dice2, userId);

                return ResponseEntity.ok().body("");

        }

        @PostMapping("/{gameId}/end-turn")
        public ResponseEntity<String> endTurnInAGame(

                        @Parameter(description = "Room id of the game to be started.", example = "1") @PathVariable int gameId,
                        @RequestParam String userId) {

                gameService.endTurn(gameId, UUID.fromString(userId));

                return ResponseEntity.ok().body("");

        }

        @PostMapping("/{gameId}/build-road")
        public ResponseEntity<String> buildRoad(

                        @Parameter(description = "Room id of the game to be started.", example = "1") @PathVariable int gameId,
                        @RequestBody BuildInfo buildInfo) {

                final int roadIndex = buildInfo.getIndex();
                final String userId = buildInfo.getUserId();

                gameService.buildRoad(gameId, roadIndex, UUID.fromString(userId));

                return ResponseEntity.ok().body("");

        }

        @PostMapping("/{gameId}/build-settlement")
        public ResponseEntity<String> buildSettlement(

                        @Parameter(description = "Room id of the game to be started.", example = "1") @PathVariable int gameId,
                        @RequestBody BuildInfo buildInfo) {

                final int settlementIndex = buildInfo.getIndex();
                final String userId = buildInfo.getUserId();
                gameService.buildSettlement(gameId, settlementIndex,
                                UUID.fromString(userId));

                return ResponseEntity.ok().body("");

        }

        @PostMapping("/{gameId}/build-city")
        public ResponseEntity<String> buildCity(

                        @Parameter(description = "Room id of the game to be started.", example = "1") @PathVariable int gameId,
                        @RequestBody BuildInfo buildInfo) {

                final int cityIndex = buildInfo.getIndex();
                final String userId = buildInfo.getUserId();

                gameService.buildCity(gameId, cityIndex, UUID.fromString(userId));

                return ResponseEntity.ok().body("");

        }

        @GetMapping("/{gameId}/user-options")
        public ResponseEntity<UserOptions> buildCity(

                        @Parameter(description = "Room id of the game to be started.", example = "1") @PathVariable int gameId,
                        @RequestParam String userId) {

                UserOptions userOptions = gameService.getUserOptions(gameId, UUID.fromString(userId));

                return ResponseEntity.ok().body(userOptions);

        }

        @PostMapping("/{gameId}/choose-settlement-and-road-for-bot")
        public ResponseEntity<String> chooseSettlementAndRoadForBot(
                        @PathVariable int gameId,
                        @RequestParam UUID userId) {

                gameService.chooseSettlementAndRoadForBot(gameId, userId);

                return ResponseEntity.ok().body("");
        }

        @GetMapping("/{gameId}/available-settlements-and-roads-to-choose")
        public ResponseEntity<Map<Integer, List<Integer>>> getAvailableSettlementsAndRoadsToChoose(
                        @PathVariable int gameId,
                        @RequestParam UUID userId) {

                Map<Integer, List<Integer>> availableSettlementsAndRoadsToChoose = gameService
                                .getAvailableSettlementsAndRoadsToChoose(gameId,
                                                userId);

                return ResponseEntity.ok().body(availableSettlementsAndRoadsToChoose);
        }

        @PostMapping("/{gameId}/choose-settlement")
        public ResponseEntity<String> chooseSettlement(
                        @PathVariable int gameId,
                        @RequestParam UUID userId,
                        @RequestParam int settlementIndex) {

                gameService.chooseSettlement(gameId, userId, settlementIndex);

                return ResponseEntity.ok().body("");
        }

        @PostMapping("/{gameId}/choose-road")
        public ResponseEntity<String> chooseRoad(
                        @PathVariable int gameId,
                        @RequestParam UUID userId,
                        @RequestParam int roadIndex) {

                gameService.chooseRoad(gameId, userId, roadIndex);

                return ResponseEntity.ok().body("");
        }

}
