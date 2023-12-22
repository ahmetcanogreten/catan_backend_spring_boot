package com.ogreten.catan.room.schema;

import java.util.List;

import com.ogreten.catan.room.domain.Resource;

import lombok.Data;

@Data
public class RoomWithOnlyNameIn {
    private String name;
    private List<Resource> resources;
}
