package com.chatroom.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom {

    private final String roomId;

    private final String roomName;

    private final String ownerName;

    private final int maxMembers;

    private final Set<ClientHandler> members = ConcurrentHashMap.newKeySet();

    public ChatRoom(
            String roomId,
            String roomName,
            String ownerName) {
        this(roomId, roomName, ownerName, 10);
    }

    public ChatRoom(
            String roomId,
            String roomName,
            String ownerName,
            int maxMembers) {

        this.roomId = roomId;
        this.roomName = roomName;
        this.ownerName = ownerName;
        this.maxMembers = Math.max(2, maxMembers);
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

    public int getMaxMembers() {
        return maxMembers;
    }

    public boolean isFull() {
        return members.size() >= maxMembers;
    }

    public Set<ClientHandler> getMembers() {
        return members;
    }

    public void addMember(
            ClientHandler client) {

        members.add(client);
    }

    public void removeMember(
            ClientHandler client) {

        members.remove(client);
    }
}
