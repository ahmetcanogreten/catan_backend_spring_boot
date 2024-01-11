package com.ogreten.catan.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.ogreten.catan.auth.domain.User;
import com.ogreten.catan.auth.repository.UserRepository;
import com.ogreten.catan.room.domain.Resource;
import com.ogreten.catan.room.domain.Room;
import com.ogreten.catan.room.exceptions.RoomNotFoundException;
import com.ogreten.catan.room.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @InjectMocks
    private RoomService roomService;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void GivenValidPageable_WhenGetActiveRooms_ThenReturnActiveRooms() {
        // Arrange
        Room room = Room.builder().id(1).build();
        Page<Room> page = new PageImpl<Room>(List.of(room));

        when(roomRepository.findAllByIsGameStartedFalse(any(Pageable.class))).thenReturn(page);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> result = roomService.getActiveRooms(pageable);

        // Assert
        assertThat(result).isEqualTo(page);
    }

    @Test
    void GivenValidRoomNameAndResourcesAndUser_WhenCreateRoom_ThenReturnCreatedRoom() {
        // Arrange
        String roomName = "roomName";
        List<Resource> resources = List.of(Resource.builder().index(0).type("type").number(0).build());
        User user = User.builder().id(
                UUID.fromString("00000000-0000-0000-0000-000000000000")).build();
        String roomCode = "roomCode";

        Room expectedRoom = Room.builder()
                .id(1)
                .name(roomName)
                .owner(user)
                .code(roomCode)
                .isGameStarted(false)
                .users(Set.of(user))
                .build();

        when(roomRepository.save(any(Room.class))).thenReturn(expectedRoom);

        // Act
        Room result = roomService.createRoom(roomName, resources, user);

        // Assert
        assertThat(result).isEqualTo(expectedRoom);

    }

    @Test
    void GivenValidRoomCodeAndUser_WhenJoinRoom_ThenReturnJoinedRoom() {
        // Arrange
        String roomCode = "roomCode";
        User user = User.builder().id(
                UUID.fromString("00000000-0000-0000-0000-000000000000")).build();

        Room roomWithoutUser = Room.builder()
                .id(1)
                .users(Set.of()).build();
        Room roomWithUser = Room.builder()
                .id(1).users(Set.of(user))
                .build();

        when(roomRepository.findByCode(roomCode)).thenReturn(Optional.of(roomWithoutUser));
        when(roomRepository.save(roomWithoutUser)).thenReturn(roomWithUser);

        // Act
        Room result = roomService.joinRoom(roomCode, user);

        // Assert
        assertThat(result).isEqualTo(roomWithUser);
        assertThat(result.getUsers()).contains(user);
    }

    @Test
    void GivenInvalidRoomCode_WhenJoinRoom_ThenThrowRoomNotFoundException() {
        // Arrange
        when(roomRepository.findByCode(anyString())).thenReturn(Optional.empty());

        // Act
        Executable executable = () -> roomService.joinRoom("invalidRoomCode", User.builder().build());

        // Assert
        assertThrows(RoomNotFoundException.class, executable);
    }

    @Test
    void GivenValidRoomId_WhenGetRoom_ThenReturnRoom() {
        // Arrange
        int roomId = 1;
        Room room = Room.builder().id(roomId).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        // Act
        Room result = roomService.getRoom(roomId);

        // Assert
        assertThat(result).isEqualTo(room);
    }

    @Test
    void GivenInvalidRoomId_WhenGetRoomThenThrowRoomNotFoundException() {
        // Arrange
        when(roomRepository.findById(anyInt())).thenReturn(Optional.empty());

        // Act
        Executable executable = () -> roomService.getRoom(1);

        // Assert
        assertThrows(RoomNotFoundException.class, executable);
    }

    @Test
    void GivenInvalidRoomId_WhenAddBotToRoom_ThenThrowRoomNotFoundException() {
        // Arrange
        when(roomRepository.findById(anyInt())).thenReturn(Optional.empty());

        // Act
        Executable executable = () -> roomService.addBotToRoom(1);

        // Assert
        assertThrows(RoomNotFoundException.class, executable);
    }

    @Test
    void GivenRoomIdOfRoomWithSingleUser_WhenAddBotToRoom_ThenReturnRoomWithTwoUsers() {
        // Arrange
        int roomId = 1;
        User user = User.builder().id(
                UUID.fromString("00000000-0000-0000-0000-000000000000")).build();
        User bot = User.builder().id(
                UUID.fromString("00000000-0000-0000-0000-000000000001")).build();

        Room roomWithSingleUser = Room.builder()
                .id(roomId)
                .users(Set.of(user))
                .build();
        Room roomWithTwoUsers = Room.builder()
                .id(roomId)
                .users(Set.of(user, bot))
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(roomWithSingleUser));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(bot);
        when(roomRepository.save(roomWithSingleUser)).thenReturn(roomWithTwoUsers);

        // Act
        Room result = roomService.addBotToRoom(roomId);

        // Assert
        assertThat(result).isEqualTo(roomWithTwoUsers);
        assertThat(result.getUsers()).contains(user, bot);
    }

}
