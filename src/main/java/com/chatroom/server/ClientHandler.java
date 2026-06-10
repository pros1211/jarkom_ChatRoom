package com.chatroom.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.chatroom.protocol.MessageType;
import com.chatroom.protocol.Packet;
import com.google.gson.Gson;

public class ClientHandler implements Runnable {

    private final Socket socket;

    private String currentRoomId;
    private final ChatServer server;

    private final Gson gson = new Gson();

    private BufferedReader reader;

    private PrintWriter writer;

    private String username;

    public ClientHandler(
            Socket socket,
            ChatServer server) {

        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {

        try {

            reader = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));

            writer = new PrintWriter(
                    socket.getOutputStream(),
                    true);

            String json;

            while ((json = reader.readLine()) != null) {

                Packet packet = gson.fromJson(
                        json,
                        Packet.class);

                handlePacket(packet);
            }

        } catch (Exception e) {

            System.out.println(
                    "Client disconnected");
        }
    }

    private void handlePacket(
            Packet packet) {

        switch (packet.getType()) {

            case LOGIN:
                handleLogin(packet);
                break;

            case CREATE_ROOM:
                handleCreateRoom(packet);
                break;

            case JOIN_ROOM:
                handleJoinRoom(packet);
                break;

            case LEAVE_ROOM:
                handleLeaveRoom();
                break;

            case CHAT_MESSAGE:
                handleChatMessage(packet);
                break;
        }
    }

    private void handleLogin(
            Packet packet) {

        username = packet.getUsername();

        System.out.println(
                "User login: "
                        + username);

        Packet response = new Packet(
                MessageType.LOGIN_SUCCESS);

        response.setMessage(
                "Welcome "
                        + username);

        sendPacket(response);
    }

    private void handleCreateRoom(
            Packet packet) {

        ChatRoom room = server.getRoomManager()
                .createRoom(
                        packet.getRoomName());
        System.out.println(
                server.getRoomManager()
                        .getRooms()
                        .keySet());
        System.out.println(
                "Room created: "
                        + room.getRoomName());

        Packet response = new Packet(
                MessageType.ROOM_CREATED);

        response.setRoomId(
                room.getRoomId());

        response.setRoomName(
                room.getRoomName());

        sendPacket(response);
    }

    private void handleJoinRoom(
            Packet packet) {

        ChatRoom room = server
                .getRoomManager()
                .getRoom(
                        packet.getRoomId());

        if (room == null)
            return;

        room.addMember(this);

        currentRoomId = room.getRoomId();

        System.out.println(
                username
                        + " joined "
                        + room.getRoomName());
    }

    private void handleLeaveRoom() {

        if (currentRoomId == null)
            return;

        ChatRoom room = server
                .getRoomManager()
                .getRoom(
                        currentRoomId);

        if (room != null) {

            room.removeMember(this);
        }

        currentRoomId = null;
    }

    private void handleChatMessage(
            Packet packet) {

        if (currentRoomId == null)
            return;

        ChatRoom room = server
                .getRoomManager()
                .getRoom(
                        currentRoomId);

        if (room == null)
            return;

        Packet outgoing = new Packet(
                MessageType.CHAT_MESSAGE);

        outgoing.setUsername(
                username);

        outgoing.setMessage(
                packet.getMessage());

        broadcast(
                room,
                outgoing);
    }

    private void broadcast(
            ChatRoom room,
            Packet packet) {

        for (ClientHandler member : room.getMembers()) {

            member.sendPacket(
                    packet);
        }
    }

    private void sendPacket(
            Packet packet) {

        writer.println(
                gson.toJson(packet));
    }
}