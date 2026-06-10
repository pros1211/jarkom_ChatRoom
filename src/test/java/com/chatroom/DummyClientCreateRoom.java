package com.chatroom;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.chatroom.protocol.MessageType;
import com.chatroom.protocol.Packet;
import com.google.gson.Gson;

public class DummyClientCreateRoom {

    public static void main(String[] args)
            throws Exception {

        Socket socket = new Socket(
                "localhost",
                5000);

        Gson gson = new Gson();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream()));

        PrintWriter writer = new PrintWriter(
                socket.getOutputStream(),
                true);

        Packet login = new Packet(
                MessageType.LOGIN);

        login.setUsername(
                "Prospero");

        writer.println(
                gson.toJson(login));

        System.out.println(
                reader.readLine());

        Packet createRoom = new Packet(
                MessageType.CREATE_ROOM);

        createRoom.setRoomName(
                "Jarkom");

        writer.println(
                gson.toJson(createRoom));

        System.out.println(
                reader.readLine());

        socket.close();
    }
}