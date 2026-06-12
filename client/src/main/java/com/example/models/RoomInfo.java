package com.example.models;

public class RoomInfo {
    private String roomId;
    private String roomName;
    private String owner;
    private int memberCount;

    public RoomInfo() {}

    public RoomInfo(String roomId, String roomName, String owner, int memberCount) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.owner = owner;
        this.memberCount = memberCount;
    }

    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getOwner() { return owner; }
    public int getMemberCount() { return memberCount; }
}