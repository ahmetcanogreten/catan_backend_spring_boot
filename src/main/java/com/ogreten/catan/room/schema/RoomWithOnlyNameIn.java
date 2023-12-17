package com.ogreten.catan.room.schema;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class RoomWithOnlyNameIn {
    private String name;
    private List<Map<String, Object>> resources;
}
