package com.chatroom;

import com.chatroom.model.Room;

public class TestRoom {

    public static void main(String[] args) {

        Room room = new Room(
                "R001",
                "Jarkom",
                "Prospero");

        System.out.println(room.getRoomId());
        System.out.println(room.getRoomName());
        System.out.println(room.getOwner());
    }
}