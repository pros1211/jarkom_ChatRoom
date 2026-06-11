package com.chatroom;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.chatroom.protocol.MessageType;
import com.chatroom.protocol.Packet;
import com.google.gson.Gson;

public class CreateRoomTest {

    public static void main(String[] args)
            throws Exception {

        Socket socket = new Socket("localhost", 5000);

        Gson gson = new Gson();

        PrintWriter writer = new PrintWriter(
                socket.getOutputStream(),
                true);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream()));

        Packet login = new Packet(MessageType.LOGIN);

        login.setUsername("Prospero");

        writer.println(
                gson.toJson(login));

        System.out.println(
                reader.readLine());

        Packet room = new Packet(
                MessageType.CREATE_ROOM);

        room.setRoomName("Jarkom");

        writer.println(
                gson.toJson(room));

        System.out.println(
                reader.readLine());

        socket.close();
    }
}