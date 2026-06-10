package com.chatroom.model;

import java.time.LocalDateTime;

public class Room {

    private String roomId;

    private String roomName;

    private String owner;

    private LocalDateTime createdAt;

    public Room() {
    }

    public Room(
            String roomId,
            String roomName,
            String owner) {

        this.roomId = roomId;
        this.roomName = roomName;
        this.owner = owner;
        this.createdAt = LocalDateTime.now();
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getOwner() {
        return owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}