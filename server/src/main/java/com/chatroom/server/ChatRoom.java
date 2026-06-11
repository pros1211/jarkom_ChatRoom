package com.chatroom.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom {

    private final String roomId;
    private final String roomName;
    private final String ownerName;
    private final Set<ClientHandler> members = ConcurrentHashMap.newKeySet();

    public ChatRoom(String roomId, String roomName, String ownerName) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.ownerName = ownerName;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Set<ClientHandler> getMembers() {
        return members;
    }

    public void addMember(ClientHandler client) {
        members.add(client);
    }

    public void removeMember(ClientHandler client) {
        members.remove(client);
    }
}