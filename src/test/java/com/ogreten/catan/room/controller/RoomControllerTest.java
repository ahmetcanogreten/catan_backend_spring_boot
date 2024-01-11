package com.ogreten.catan.room.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogreten.catan.room.domain.Resource;
import com.ogreten.catan.room.domain.Room;
import com.ogreten.catan.room.exceptions.RoomNotFoundException;
import com.ogreten.catan.room.schema.RoomWithOnlyNameIn;
import com.ogreten.catan.room.service.RoomService;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    private MockMvc mvc;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private RoomController roomController;

    private static final String ROOM_PATH = "/api/rooms";
    private static final String ROOM_ACTIVE_PATH = "/api/rooms/active";
    private static final String ROOM_JOIN_PATH = "/api/rooms/join";
    private static final String ROOM_GET_PATH = "/api/rooms/{roomId}";
    private static final String ROOM_ADD_BOT_PATH = "/api/rooms/{roomId}/add-bot";

    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders.standaloneSetup(roomController).build();
    }

    @Test
    void GivenValidPageParameters_WhenGetActiveRooms_ThenReturnActiveRooms() throws Exception {
        // Arrange
        Room activeRoom1 = Room.builder().id(1).build();
        Room activeRoom2 = Room.builder().id(2).build();

        Page<Room> rooms = new PageImpl<Room>(List.of(
                activeRoom1, activeRoom2));
        when(roomService.getActiveRooms(any())).thenReturn(rooms);

        // Act
        MvcResult result = this.mvc.perform(get(ROOM_ACTIVE_PATH)).andReturn();

        // Assert
        ObjectMapper mapper = new ObjectMapper();
        JsonNode roomsJson = mapper.readTree(result.getResponse().getContentAsString()).get("content");

        // .andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(roomsJson.size()).isEqualTo(2);
        assertThat(roomsJson.get(0).get("id").asInt()).isEqualTo(activeRoom1.getId());
        assertThat(roomsJson.get(1).get("id").asInt()).isEqualTo(activeRoom2.getId());
    }

    @Test
    void GivenValidRoomParemeters_WhenCreateRoom_ThenReturnCreatedRoom() throws Exception {
        // Arrange
        RoomWithOnlyNameIn roomInput = new RoomWithOnlyNameIn();
        roomInput.setName("Test Room");
        roomInput.setResources(List.of(new Resource(0, "Wood", 5)));

        Room createdRoom = Room.builder().name(roomInput.getName()).isGameStarted(false).build();
        when(roomService.createRoom(anyString(), any(), any())).thenReturn(createdRoom);

        // Act
        MvcResult result = this.mvc.perform(post(ROOM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(roomInput))).andReturn();

        // Assert
        JsonNode roomJson = new ObjectMapper().readTree(result.getResponse().getContentAsString());

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(roomJson.get("name").asText()).isEqualTo(createdRoom.getName());
        assertThat(roomJson.get("gameStarted").asBoolean()).isEqualTo(createdRoom.isGameStarted());
    }

    @Test
    void GivenCorrectJoinCode_WhenJoinRoom_ThenReturnRoom() throws Exception {
        // Arrange
        String joinCode = "123456";
        Room joinedRoom = Room.builder().name("Joined Room").isGameStarted(false).build();
        when(roomService.joinRoom(anyString(), any())).thenReturn(joinedRoom);

        // Act
        MvcResult result = this.mvc.perform(post(ROOM_JOIN_PATH)
                .param("code", joinCode)).andReturn();

        // Assert
        JsonNode roomJson = new ObjectMapper().readTree(result.getResponse().getContentAsString());

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(roomJson.get("name").asText()).isEqualTo(joinedRoom.getName());
        assertThat(roomJson.get("gameStarted").asBoolean()).isEqualTo(joinedRoom.isGameStarted());
    }

    @Test
    void GivenIncorrectJoinCode_WhenJoinRoom_ThenReturnNotFound() throws Exception {
        // Arrange
        String joinCode = "123456";
        when(roomService.joinRoom(anyString(), any())).thenThrow(RoomNotFoundException.class);

        // Act
        MvcResult result = this.mvc.perform(post(ROOM_JOIN_PATH)
                .param("code", joinCode)).andReturn();

        // Assert
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void GivenExistingRoomId_WhenGetRoom_ThenReturnRoom() throws Exception {
        // Arrange
        int roomId = 1;
        Room room = Room.builder().id(roomId).build();
        when(roomService.getRoom(anyInt())).thenReturn(room);

        // Act
        MvcResult result = this.mvc.perform(
                get(ROOM_GET_PATH, roomId)

        )
                .andReturn();

        // Assert
        JsonNode roomJson = new ObjectMapper().readTree(result.getResponse().getContentAsString());

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(roomJson.get("id").asInt()).isEqualTo(room.getId());
    }

    @Test
    void GivenNonExistingRoomId_WhenGetRoom_ThenReturnNotFound() throws Exception {
        // Arrange
        int roomId = 1;
        when(roomService.getRoom(anyInt())).thenThrow(RoomNotFoundException.class);

        // Act
        MvcResult result = this.mvc.perform(get(ROOM_GET_PATH, roomId))
                .andReturn();

        // Assert

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void GivenExistingRoomId_WhenAddBot_ThenReturnRoom() throws Exception {
        // Arrange
        int roomId = 1;
        Room roomWithBot = Room.builder().id(roomId).build();
        when(roomService.addBotToRoom(anyInt())).thenReturn(roomWithBot);

        // Act
        MvcResult result = this.mvc.perform(post(ROOM_ADD_BOT_PATH,
                roomId)).andReturn();

        // Assert
        JsonNode roomJson = new ObjectMapper().readTree(result.getResponse().getContentAsString());

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(roomJson.get("id").asInt()).isEqualTo(roomWithBot.getId());
    }

    @Test
    void GivenNonExistingRoomId_WhenAddBot_ThenReturnNotFound() throws Exception {
        // Arrange
        int roomId = 1;
        when(roomService.addBotToRoom(anyInt())).thenThrow(RoomNotFoundException.class);

        // Act
        MvcResult result = this.mvc.perform(post(ROOM_ADD_BOT_PATH,
                roomId)).andReturn();

        // Assert
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

}