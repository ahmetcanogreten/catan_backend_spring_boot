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
import com.ogreten.catan.room.dto.RoomNameDTO;
import com.ogreten.catan.room.repository.RoomRepository;
import com.ogreten.catan.utils.RandomStringGenerator;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping("/active")
    public Page<Room> getActiveRooms(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return roomRepository.findAllByIsGameStartedFalse(pageable);
    }

    @PostMapping("")
    public Room createRoom(@RequestBody RoomNameDTO roomNameDTO) {
        Room room = new Room();
        room.setName(roomNameDTO.getName());
        room.setCode(RandomStringGenerator.generate(6));
        room.setGameStarted(false);
        return roomRepository.save(room);
    }

    @PostMapping("/join")
    public ResponseEntity<Room> joinRoom(@AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam String code) {
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
