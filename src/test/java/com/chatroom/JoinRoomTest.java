package com.chatroom;

import java.io.PrintWriter;
import java.net.Socket;

import com.chatroom.protocol.MessageType;
import com.chatroom.protocol.Packet;
import com.google.gson.Gson;

public class JoinRoomTest {

    public static void main(String[] args)
            throws Exception {

        Socket socket = new Socket(
                "localhost",
                5000);

        Gson gson = new Gson();

        PrintWriter writer = new PrintWriter(
                socket.getOutputStream(),
                true);

        Packet login = new Packet(
                MessageType.LOGIN);

        login.setUsername(
                "Prospero");

        writer.println(
                gson.toJson(login));

        Packet join = new Packet(
                MessageType.JOIN_ROOM);

        join.setRoomId(
                "ROOM001");

        writer.println(
                gson.toJson(join));
        while (true) {
            Thread.sleep(1000);
        }
    }
}