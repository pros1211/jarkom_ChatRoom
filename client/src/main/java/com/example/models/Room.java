package com.example.models;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String roomId;
    private String roomName;
    private String ownerName;
    private int maxMembers;
    private List<String> participants;

    public Room(String roomId, String roomName, String ownerName) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.ownerName = ownerName;
        this.participants = new ArrayList<>();
        this.participants.add(ownerName);
    }

    public Room(String roomId, String roomName, String ownerName, int maxMembers) {
        this(roomId, roomName, ownerName);
        this.maxMembers = maxMembers;
    }

    public String getRoomId() { return roomId; }
    public String getName() { return roomName; }
    public String getOwnerName() { return ownerName; }
    public int getMaxMembers() { return maxMembers; }
    public void setMaxMembers(int maxMembers) { this.maxMembers = maxMembers; }
    public List<String> getParticipants() { return participants; }
    
    public boolean isOwner(String username) {
        return ownerName != null && ownerName.equals(username);
    }
}
