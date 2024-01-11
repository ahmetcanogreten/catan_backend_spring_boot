package com.ogreten.catan.room.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ogreten.catan.auth.domain.CustomUserDetails;
import com.ogreten.catan.auth.domain.User;
import com.ogreten.catan.room.domain.Resource;
import com.ogreten.catan.room.domain.Room;
import com.ogreten.catan.room.exceptions.RoomNotFoundException;
import com.ogreten.catan.room.schema.RoomWithOnlyNameIn;
import com.ogreten.catan.room.service.RoomService;

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

        RoomService roomService;

        public RoomController(RoomService roomService) {
                this.roomService = roomService;
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
                return roomService.getActiveRooms(pageable);

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
                String roomName = roomWithOnlyNameIn.getName();
                List<Resource> resources = roomWithOnlyNameIn.getResources();

                return roomService.createRoom(
                                roomName,
                                resources,
                                user);
        }

        @Operation(summary = "Join a room", description = "Join a room using the join code", tags = {
                        "room",
                        "post" })
        @ApiResponse(responseCode = "200", content = {
                        @Content(schema = @Schema(implementation = Page.class), mediaType = "application/json") })
        @ApiResponse(responseCode = "404")
        @PostMapping("/join")
        public ResponseEntity<Room> joinRoom(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                        @Parameter(description = "The join code of the room to be joined.", example = "123456") @RequestParam String code) {
                try {

                        User user = customUserDetails.getUser();
                        Room room = roomService.joinRoom(code, user);

                        return ResponseEntity.ok(room);
                } catch (RoomNotFoundException e) {
                        return ResponseEntity.notFound().build();
                }
        }

        @Operation(summary = "Get a room", description = "Get a room using room id", tags = {
                        "room",
                        "get" })
        @ApiResponse(responseCode = "200", content = {
                        @Content(schema = @Schema(implementation = Page.class), mediaType = "application/json") })
        @ApiResponse(responseCode = "404")
        @GetMapping("/{roomId}")
        public ResponseEntity<Room> getRoom(
                        @Parameter(description = "roomId", example = "7") @PathVariable int roomId) {
                try {
                        Room room = roomService.getRoom(roomId);
                        return ResponseEntity.ok(room);
                } catch (RoomNotFoundException e) {
                        return ResponseEntity.notFound().build();
                }
        }

        @Operation(summary = "Add a bot to the room", description = "Add a bot to the room using room id")
        @ApiResponse(responseCode = "200", content = {
                        @Content(schema = @Schema(implementation = Room.class), mediaType = "application/json") })
        @ApiResponse(responseCode = "404")
        @PostMapping("/{roomId}/add-bot")
        public ResponseEntity<Room> addBot(
                        @Parameter(description = "roomId", example = "7") @PathVariable int roomId) {
                try {
                        Room room = roomService.addBotToRoom(roomId);
                        return ResponseEntity.ok(room);
                } catch (RoomNotFoundException e) {
                        return ResponseEntity.notFound().build();
                }

        }

}
