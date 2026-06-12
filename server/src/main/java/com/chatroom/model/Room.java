package com.chatroom.model;

import java.time.LocalDateTime;

public class Room {

    private String roomId;

    private String roomName;

    private String owner;

    private int maxMembers;

    private LocalDateTime createdAt;

    public Room() {
    }

    public Room(
            String roomId,
            String roomName,
            String owner) {
        this(roomId, roomName, owner, 10);
    }

    public Room(
            String roomId,
            String roomName,
            String owner,
            int maxMembers) {

        this.roomId = roomId;
        this.roomName = roomName;
        this.owner = owner;
        this.maxMembers = maxMembers;
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

    public int getMaxMembers() {
        return maxMembers;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
