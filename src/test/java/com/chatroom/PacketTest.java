package com.chatroom;

import com.chatroom.protocol.Packet;
import com.chatroom.protocol.MessageType;

public class PacketTest {

    public static void main(String[] args) {

        Packet packet = new Packet(MessageType.LOGIN);

        packet.setUsername("Prospero");

        System.out.println(packet.getType());
        System.out.println(packet.getUsername());
        System.out.println(packet.getTimestamp());
    }
}