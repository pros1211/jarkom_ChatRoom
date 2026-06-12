package com.chatroom.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {

    private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();

    public ChatRoom createRoom(
            String roomName,
            String ownerName) {

        String roomId = UUID.randomUUID()
                .toString()
                .substring(0, 8);

        ChatRoom room = new ChatRoom(
                roomId,
                roomName,
                ownerName);

        rooms.put(
                room.getRoomId(),
                room);

        return room;
    }

    public ChatRoom getRoom(
            String roomId) {

        return rooms.get(roomId);
    }

    public Map<String, ChatRoom> getRooms() {
        return rooms;
    }
}