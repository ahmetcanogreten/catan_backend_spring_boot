package com.ogreten.catan.room.controller;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ogreten.catan.auth.domain.CustomUserDetails;
import com.ogreten.catan.auth.domain.User;
import com.ogreten.catan.auth.repository.UserRepository;
import com.ogreten.catan.room.domain.Room;
import com.ogreten.catan.room.repository.RoomRepository;
import com.ogreten.catan.room.schema.RoomWithOnlyNameIn;
import com.ogreten.catan.utils.RandomStringGenerator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;

@Transactional
@Tag(name = "Room", description = "Room Management API")
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

        RoomRepository roomRepository;
        UserRepository userRepository;

        public RoomController(RoomRepository roomRepository, UserRepository userRepository) {
                this.roomRepository = roomRepository;
                this.userRepository = userRepository;
        }

        @Operation(summary = "Get active rooms", description = "Get active rooms. That is rooms whose games are not started yet.", tags = {
                        "room",
                        "get" })
        @ApiResponse(responseCode = "200", content = {
                        @Content(schema = @Schema(implementation = Page.class), mediaType = "application/json") })
        @GetMapping("/active")
        public Page<Room> getActiveRooms(
                        @Parameter(description = "Page of the active rooms.", example = "0") @RequestParam(defaultValue = "0") int pageNo,
                        @Parameter(description = "Page size of the active rooms.", example = "10") @RequestParam(defaultValue = "10") int pageSize) {
                Pageable pageable = PageRequest.of(pageNo, pageSize);
                return roomRepository.findAllByIsGameStartedFalse(pageable);
        }

        @Operation(summary = "Create a room", description = "Create a room with a name", tags = {
                        "room",
                        "post" })
        @ApiResponse(responseCode = "200", content = {
                        @Content(schema = @Schema(implementation = Room.class), mediaType = "application/json") })
        @PostMapping("")
        public Room createRoom(
                        @AuthenticationPrincipal CustomUserDetails customUserDetails,
                        @Parameter(description = "Name of the room", example = "My Room") @RequestBody RoomWithOnlyNameIn roomWithOnlyNameIn) {
                User user = customUserDetails.getUser();

                Room room = new Room();
                room.setResources(roomWithOnlyNameIn.getResources());
                room.setOwner(user);
                room.setName(roomWithOnlyNameIn.getName());
                room.setCode(RandomStringGenerator.generate(Room.CODE_LENGTH));
                room.setGameStarted(false);
                room.setUsers(Set.of(user));
                return roomRepository.save(room);
        }

        @Operation(summary = "Join a room", description = "Join a room using the join code", tags = {
                        "room",
                        "post" })
        @ApiResponse(responseCode = "200", content = {
                        @Content(schema = @Schema(implementation = Page.class), mediaType = "application/json") })
        @ApiResponse(responseCode = "404", content = {
        })
        @PostMapping("/join")
        public ResponseEntity<Room> joinRoom(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                        @Parameter(description = "The join code of the room to be joined.", example = "123456") @RequestParam String code) {
                User user = customUserDetails.getUser();

                Optional<Room> optionalRoom = roomRepository.findByCode(code);
                if (optionalRoom.isEmpty()) {
                        return ResponseEntity.notFound().build();
                }

                Room room = optionalRoom.get();
                room.getUsers().add(user);
                roomRepository.save(room);
                return ResponseEntity.ok(room);
        }

        @Operation(summary = "Get a room", description = "Get a room using room id", tags = {
                        "room",
                        "get" })
        @ApiResponse(responseCode = "200", content = {
                        @Content(schema = @Schema(implementation = Page.class), mediaType = "application/json") })
        @ApiResponse(responseCode = "404", content = {
        })
        @GetMapping("/{roomId}")
        public ResponseEntity<Room> getRoom(
                        @Parameter(description = "roomId", example = "7") @PathVariable int roomId) {

                Optional<Room> optionalRoom = roomRepository.findById(roomId);
                if (optionalRoom.isEmpty()) {
                        return ResponseEntity.notFound().build();
                }

                Room room = optionalRoom.get();
                return ResponseEntity.ok(room);
        }

        @Operation(summary = "Update the room", description = "Update the room using room id", tags = {
                        "room",
                        "patch" })
        @ApiResponse(responseCode = "200", content = {
                        @Content(schema = @Schema(implementation = Page.class), mediaType = "application/json") })
        @ApiResponse(responseCode = "404", content = {
        })
        @PatchMapping("/{roomId}")
        public ResponseEntity<Room> updateRoom(
                        @Parameter(description = "roomId", example = "7") @PathVariable int roomId,
                        @Parameter(description = "Room with name and resources") @RequestBody RoomWithOnlyNameIn roomWithOnlyNameIn

        ) {

                Optional<Room> optionalRoom = roomRepository.findById(roomId);
                if (optionalRoom.isEmpty()) {
                        return ResponseEntity.notFound().build();
                }

                Room room = optionalRoom.get();
                room.setResources(roomWithOnlyNameIn.getResources());

                roomRepository.save(room);
                return ResponseEntity.ok(room);
        }

        @Operation(summary = "Update the room", description = "Update the room using room id", tags = {
                        "room",
                        "patch" })
        @ApiResponse(responseCode = "200", content = {
                        @Content(schema = @Schema(implementation = Page.class), mediaType = "application/json") })
        @ApiResponse(responseCode = "404", content = {
        })
        @PostMapping("/{roomId}/add-bot")
        public ResponseEntity<Room> addBot(
                        @Parameter(description = "roomId", example = "7") @PathVariable int roomId

        ) {

                Optional<Room> optionalRoom = roomRepository.findById(roomId);
                if (optionalRoom.isEmpty()) {
                        return ResponseEntity.notFound().build();
                }

                Room room = optionalRoom.get();

                int size = room.getUsers().size();

                User bot;
                switch (size) {
                        case 1:
                                bot = userRepository.findByEmail("bot1@bot.com").orElseGet(() -> {
                                        User user = new User();
                                        user.setEmail("bot1@bot.com");
                                        user.setFirstName("Barbara");
                                        user.setLastName("Liskov");
                                        user.setBot(true);
                                        userRepository.save(user);
                                        return user;

                                });
                                break;
                        case 2:
                                bot = userRepository.findByEmail("bot2@bot.com").orElseGet(() -> {
                                        User user = new User();
                                        user.setEmail("bot2@bot.com");
                                        user.setFirstName("Alan");
                                        user.setLastName("Turing");
                                        user.setBot(true);
                                        userRepository.save(user);
                                        return user;

                                });
                                break;
                        case 3:
                                bot = userRepository.findByEmail("bot3@bot.com").orElseGet(() -> {
                                        User user = new User();
                                        user.setEmail("bot3@bot.com");
                                        user.setFirstName("Ada");
                                        user.setLastName("Lovelace");
                                        user.setBot(true);
                                        userRepository.save(user);
                                        return user;

                                });
                                break;
                        default:
                                return ResponseEntity.badRequest().build();
                }

                room.getUsers().add(bot);

                roomRepository.save(room);
                return ResponseEntity.ok(room);
        }

}
