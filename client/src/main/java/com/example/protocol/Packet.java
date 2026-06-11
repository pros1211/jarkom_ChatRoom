package com.example.protocol;

public class Packet {
    private MessageType type;
    private String username;
    private String roomId;
    private String roomName;
    private String targetUser;
    private String message;
    private long timestamp;

    public Packet() {}

    public Packet(MessageType type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getTargetUser() { return targetUser; }
    public void setTargetUser(String targetUser) { this.targetUser = targetUser; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}