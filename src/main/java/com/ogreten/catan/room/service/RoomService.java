package com.ogreten.catan.room.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ogreten.catan.auth.domain.User;
import com.ogreten.catan.auth.repository.UserRepository;
import com.ogreten.catan.room.domain.Resource;
import com.ogreten.catan.room.domain.Room;
import com.ogreten.catan.room.exceptions.RoomFullException;
import com.ogreten.catan.room.exceptions.RoomNotFoundException;
import com.ogreten.catan.room.repository.RoomRepository;
import com.ogreten.catan.utils.RandomStringGenerator;

@Service
public class RoomService {
    final RoomRepository roomRepository;
    final UserRepository userRepository;

    public RoomService(RoomRepository roomRepository,
            UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public Page<Room> getActiveRooms(
            Pageable pageable) {
        return roomRepository.findAllByIsGameStartedFalse(pageable);
    }

    public Room createRoom(
            String roomName,
            List<Resource> resources,
            User user) {
        Room room = Room.builder()
                .name(roomName)
                .resources(resources)
                .owner(user)
                .code(RandomStringGenerator.generate(Room.CODE_LENGTH))
                .isGameStarted(false)
                .users(Set.of(user))
                .build();

        return roomRepository.save(room);
    }

    public Room joinRoom(
            String roomCode,
            User user) {
        Optional<Room> optionalRoom = roomRepository.findByCode(roomCode);

        if (optionalRoom.isEmpty()) {
            throw new RoomNotFoundException();
        }

        Room room = optionalRoom.get();

        Set<User> updatedUsers = new HashSet<>(room.getUsers());
        updatedUsers.add(user);

        room.setUsers(updatedUsers);

        return roomRepository.save(room);
    }

    public Room getRoom(
            int roomId) {
        Optional<Room> optionalRoom = roomRepository.findById(roomId);

        if (optionalRoom.isEmpty()) {
            throw new RoomNotFoundException();
        }

        return optionalRoom.get();
    }

    public Room updateRoom(
            int roomId,
            List<Resource> resources) {

        Optional<Room> optionalRoom = roomRepository.findById(roomId);
        if (optionalRoom.isEmpty()) {
            throw new RoomNotFoundException();
        }

        Room room = optionalRoom.get();
        room.setResources(resources);

        return roomRepository.save(room);
    }

    public Room addBotToRoom(
            int roomId) {

        Optional<Room> optionalRoom = roomRepository.findById(roomId);
        if (optionalRoom.isEmpty()) {
            throw new RoomNotFoundException();
        }

        Room room = optionalRoom.get();

        int size = room.getUsers().size();

        User bot;
        switch (size) {
            case 1:
                bot = userRepository.findByEmail("bot1@bot.com").orElseGet(() -> {
                    User user = User.builder()
                            .email("bot1@bot.com")
                            .firstName("Barbara")
                            .lastName("Liskov")
                            .bot(true)
                            .build();

                    return userRepository.save(user);
                });
                break;
            case 2:
                bot = userRepository.findByEmail("bot2@bot.com").orElseGet(() -> {

                    User user = User.builder()
                            .email("bot2@bot.com")
                            .firstName("Alan")
                            .lastName("Turing")
                            .bot(true)
                            .build();

                    return userRepository.save(user);

                });
                break;
            case 3:
                bot = userRepository.findByEmail("bot3@bot.com").orElseGet(() -> {
                    User user = User.builder()
                            .email("bot3@bot.com")
                            .firstName("Ada")
                            .lastName("Lovelace")
                            .bot(true)
                            .build();

                    return userRepository.save(user);
                });
                break;
            default:
                throw new RoomFullException();
        }

        Set<User> updatedUsers = new HashSet<>(room.getUsers());
        updatedUsers.add(bot);

        room.setUsers(updatedUsers);

        return roomRepository.save(room);
    }

}
