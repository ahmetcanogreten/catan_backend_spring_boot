package com.ogreten.catan.room.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogreten.catan.room.domain.Resource;
import com.ogreten.catan.room.domain.Room;
import com.ogreten.catan.room.schema.RoomWithOnlyNameIn;
import com.ogreten.catan.room.service.RoomService;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RoomService roomService;

    @Test
    @WithMockUser()

    void getActiveRooms() throws Exception {
        Room activeRoom = Room.builder().name("active").isGameStarted(false).build();

        Page<Room> rooms = new PageImpl<Room>(List.of(
                activeRoom

        ));
        when(roomService.getActiveRooms(any())).thenReturn(rooms);

        MvcResult result = this.mvc.perform(get("/api/rooms/active"))
                .andExpect(status().isOk()).andReturn();

        ObjectMapper mapper = new ObjectMapper();

        JsonNode roomsJson = mapper.readTree(result.getResponse().getContentAsString()).get("content");

        assert roomsJson.size() == 1;
        assert roomsJson.get(0).get("name").asText().equals(activeRoom.getName());
        assert roomsJson.get(0).get("gameStarted").asBoolean() == activeRoom.isGameStarted();

    }

    @Test
    @WithMockUser()
    void createRoom() throws Exception {
        RoomWithOnlyNameIn roomInput = new RoomWithOnlyNameIn();
        roomInput.setName("Test Room");
        roomInput.setResources(List.of(new Resource(0, "Wood", 5)));

        Room createdRoom = Room.builder().name(roomInput.getName()).isGameStarted(false).build();
        when(roomService.createRoom(any(), any(), any())).thenReturn(createdRoom);

        MvcResult result = this.mvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(roomInput))).andReturn();
        // .andExpect(status().isOk()).andReturn();

        JsonNode roomJson = new ObjectMapper().readTree(result.getResponse().getContentAsString());

        assert roomJson.get("name").asText().equals(createdRoom.getName());
        assert roomJson.get("gameStarted").asBoolean() == createdRoom.isGameStarted();
    }

    @Test
    @WithMockUser()
    void joinRoom() throws Exception {
        String joinCode = "123456";
        Room joinedRoom = Room.builder().name("Joined Room").isGameStarted(false).build();
        when(roomService.joinRoom(any(), any())).thenReturn(joinedRoom);

        MvcResult result = this.mvc.perform(post("/api/rooms/join")
                .param("code", joinCode))
                .andExpect(status().isOk()).andReturn();

        JsonNode roomJson = new ObjectMapper().readTree(result.getResponse().getContentAsString());

        assert roomJson.get("name").asText().equals(joinedRoom.getName());
        assert roomJson.get("gameStarted").asBoolean() == joinedRoom.isGameStarted();
    }

    @Test
    @WithMockUser()
    void getRoom() throws Exception {
        int roomId = 1;
        Room room = Room.builder().name("Test Room").isGameStarted(false).build();
        when(roomService.getRoom(any())).thenReturn(room);

        MvcResult result = this.mvc.perform(get("/api/rooms/{roomId}", roomId))
                .andExpect(status().isOk()).andReturn();

        JsonNode roomJson = new ObjectMapper().readTree(result.getResponse().getContentAsString());

        assert roomJson.get("name").asText().equals(room.getName());
        assert roomJson.get("gameStarted").asBoolean() == room.isGameStarted();
    }

    @Test
    @WithMockUser()
    void updateRoom() throws Exception {
        int roomId = 1;
        RoomWithOnlyNameIn roomInput = new RoomWithOnlyNameIn();
        roomInput.setResources(List.of(new Resource(0, "Brick", 3)));

        Room updatedRoom = Room.builder().name("Updated Room").isGameStarted(false).build();
        when(roomService.updateRoom(any(), any())).thenReturn(updatedRoom);

        MvcResult result = this.mvc.perform(patch("/api/rooms/{roomId}", roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(roomInput)))
                .andExpect(status().isOk()).andReturn();

        JsonNode roomJson = new ObjectMapper().readTree(result.getResponse().getContentAsString());

        assert roomJson.get("name").asText().equals(updatedRoom.getName());
        assert roomJson.get("gameStarted").asBoolean() == updatedRoom.isGameStarted();
    }

    @Test
    @WithMockUser()
    void addBotToRoom() throws Exception {
        int roomId = 1;
        Room roomWithBot = Room.builder().name("Room with Bot").isGameStarted(false).build();
        when(roomService.addBotToRoom(any())).thenReturn(roomWithBot);

        MvcResult result = this.mvc.perform(post("/api/rooms/{roomId}/add-bot", roomId))
                .andExpect(status().isOk()).andReturn();

        JsonNode roomJson = new ObjectMapper().readTree(result.getResponse().getContentAsString());

        assert roomJson.get("name").asText().equals(roomWithBot.getName());
        assert roomJson.get("gameStarted").asBoolean() == roomWithBot.isGameStarted();
    }

}