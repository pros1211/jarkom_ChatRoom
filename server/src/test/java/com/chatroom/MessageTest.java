package com.chatroom;

import com.chatroom.model.Message;

public class MessageTest {

    public static void main(String[] args) {

        Message msg = new Message(
                "Prospero",
                "R001",
                "Halo semua", System.currentTimeMillis());

        System.out.println(msg.getSender());
        System.out.println(msg.getRoomId());
        System.out.println(msg.getContent());
    }
}