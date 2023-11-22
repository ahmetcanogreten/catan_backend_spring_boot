package com.ogreten.catan.room.controller;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ogreten.catan.auth.domain.CustomUserDetails;
import com.ogreten.catan.auth.domain.User;
import com.ogreten.catan.room.domain.Room;
import com.ogreten.catan.room.repository.RoomRepository;
import com.ogreten.catan.room.schema.RoomWithOnlyName;
import com.ogreten.catan.utils.RandomStringGenerator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Room", description = "Room Management API")
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Operation(summary = "Get active rooms", description = "Get active rooms. That is rooms whose games are not started yet.", tags = {
            "room",
            "get" })
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {
                    @Content(schema = @Schema(implementation = Page.class), mediaType = "application/json") }),
    })
    @GetMapping("/active")
    public Page<Room> getActiveRooms(
            @Parameter(description = "Page of the active rooms.") @RequestParam(defaultValue = "0") int pageNo,
            @Parameter(description = "Page size of the active rooms.") @RequestParam(defaultValue = "10") int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return roomRepository.findAllByIsGameStartedFalse(pageable);
    }

    @Operation(summary = "Create a room", description = "Create a room with a name", tags = {
            "room",
            "post" })
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {
                    @Content(schema = @Schema(implementation = Room.class), mediaType = "application/json") }),
    })
    @PostMapping("")
    public Room createRoom(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "Name of the room") @RequestBody RoomWithOnlyName roomNameDTO) {
        User user = customUserDetails.getUser();

        Room room = new Room();
        room.setOwner(user);
        room.setName(roomNameDTO.getName());
        room.setCode(RandomStringGenerator.generate(6));
        room.setGameStarted(false);
        return roomRepository.save(room);
    }

    @Operation(summary = "Join a room", description = "Join a room using the join code", tags = {
            "room",
            "post" })
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {
                    @Content(schema = @Schema(implementation = Page.class), mediaType = "application/json") }),
            @ApiResponse(responseCode = "404", content = {
                    @Content(schema = @Schema()) }),
    })
    @PostMapping("/join")
    public ResponseEntity<Room> joinRoom(@AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "The join code of the room to be joined.") @RequestParam String code) {
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

}
