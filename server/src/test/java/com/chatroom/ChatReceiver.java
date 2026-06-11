package com.chatroom;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.chatroom.protocol.MessageType;
import com.chatroom.protocol.Packet;
import com.google.gson.Gson;

public class ChatReceiver {

    public static void main(String[] args)
            throws Exception {

        Socket socket = new Socket(
                "localhost",
                5000);

        Gson gson = new Gson();

        PrintWriter writer = new PrintWriter(
                socket.getOutputStream(),
                true);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream()));

        Packet login = new Packet(
                MessageType.LOGIN);

        login.setUsername(
                "Budi");

        writer.println(
                gson.toJson(login));

        Packet join = new Packet(
                MessageType.JOIN_ROOM);

        join.setRoomId(
                "ROOM001");

        writer.println(
                gson.toJson(join));

        System.out.println(
                "Waiting message...");

        while (true) {

            String response = reader.readLine();

            System.out.println(
                    "RECEIVED: "
                            + response);
        }
    }
}