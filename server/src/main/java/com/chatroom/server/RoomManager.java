package com.chatroom.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {

    private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();

    public ChatRoom createRoom(String roomName, String ownerName) {
        return createRoom(roomName, ownerName, 10);
    }

    public ChatRoom createRoom(String roomName, String ownerName, int maxMembers) {
        String roomId = UUID.randomUUID().toString().substring(0, 8);
        ChatRoom room = new ChatRoom(roomId, roomName, ownerName, maxMembers);
        rooms.put(roomId, room);
        return room;
    }

    public ChatRoom addRoom(String roomId, String roomName, String ownerName) {
        return addRoom(roomId, roomName, ownerName, 10);
    }

    public ChatRoom addRoom(String roomId, String roomName, String ownerName, int maxMembers) {
        return rooms.computeIfAbsent(roomId, id -> new ChatRoom(id, roomName, ownerName, maxMembers));
    }

    public void deleteRoom(String roomId) {
        rooms.remove(roomId);
    }

    public ChatRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public Map<String, ChatRoom> getRooms() {
        return rooms;
    }
}
