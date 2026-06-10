package com.chatroom;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.chatroom.protocol.MessageType;
import com.chatroom.protocol.Packet;
import com.google.gson.Gson;

public class ClientTest1 {

    public static void main(
            String[] args)
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
                "Prospero");

        writer.println(
                gson.toJson(login));

        System.out.println(
                reader.readLine());

        Thread.sleep(2000);

        Packet join = new Packet(
                MessageType.JOIN_ROOM);

        join.setRoomId(
                "ROOM001");

        writer.println(
                gson.toJson(join));

        Thread.sleep(2000);

        Packet msg = new Packet(
                MessageType.CHAT_MESSAGE);

        msg.setMessage(
                "Halo semua");

        writer.println(
                gson.toJson(msg));
    }
}