package com.chatroom.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.chatroom.protocol.MessageType;
import com.chatroom.protocol.Packet;
import com.google.gson.Gson;

public class ClientHandler implements Runnable {

    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024L * 1024L;

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
                            socket.getInputStream(),
                            StandardCharsets.UTF_8));

            socket.setTcpNoDelay(true);
            writer = new PrintWriter(
                    new OutputStreamWriter(
                            socket.getOutputStream(),
                            StandardCharsets.UTF_8),
                    true);

            String json;

            while ((json = reader.readLine()) != null) {

                try {
                    Packet packet = gson.fromJson(
                            json,
                            Packet.class);

                    handlePacket(packet);
                } catch (Exception packetError) {
                    System.out.println(
                            "Packet error from "
                                    + username
                                    + ": "
                                    + packetError.getMessage());
                    packetError.printStackTrace();
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Client disconnected: "
                            + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePacket(
            Packet packet) {

        if (packet == null || packet.getType() == null)
            return;

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

            case FILE_MESSAGE:
                handleFileMessage(packet);
                break;

            case FILE_CHUNK:
                handleFileChunk(packet);
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
        room.addMember(this);
        currentRoomId = room.getRoomId();

        Packet response = new Packet(
                MessageType.ROOM_CREATED);

        response.setRoomId(
                room.getRoomId());

        response.setRoomName(
                room.getRoomName());

        sendPacket(response);

        System.out.println(
                username
                        + " joined "
                        + room.getRoomName());
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

    private void handleFileMessage(
            Packet packet) {

        if (currentRoomId == null)
            return;

        ChatRoom room = server
                .getRoomManager()
                .getRoom(
                        currentRoomId);

        if (room == null)
            return;

        if (packet.getFileSize() > MAX_FILE_SIZE_BYTES) {
            sendError("Ukuran file maksimal 50 MB.");
            return;
        }

        Packet outgoing = new Packet(
                MessageType.FILE_MESSAGE);

        outgoing.setUsername(
                username);
        outgoing.setFileName(
                packet.getFileName());
        outgoing.setFileMimeType(
                packet.getFileMimeType());
        outgoing.setFileData(
                packet.getFileData());
        outgoing.setFileSize(
                packet.getFileSize());
        outgoing.setMessage(
                packet.getMessage());

        broadcast(
                room,
                outgoing);
    }

    private void handleFileChunk(
            Packet packet) {

        if (currentRoomId == null)
            return;

        ChatRoom room = server
                .getRoomManager()
                .getRoom(
                        currentRoomId);

        if (room == null)
            return;

        if (packet.getFileSize() > MAX_FILE_SIZE_BYTES) {
            sendError("Ukuran file maksimal 50 MB.");
            return;
        }

        Packet outgoing = new Packet(
                MessageType.FILE_CHUNK);

        outgoing.setUsername(
                username);
        outgoing.setFileId(
                packet.getFileId());
        outgoing.setFileName(
                packet.getFileName());
        outgoing.setFileMimeType(
                packet.getFileMimeType());
        outgoing.setFileData(
                packet.getFileData());
        outgoing.setFileSize(
                packet.getFileSize());
        outgoing.setChunkIndex(
                packet.getChunkIndex());
        outgoing.setTotalChunks(
                packet.getTotalChunks());
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

    private void sendError(
            String message) {

        Packet errorPacket = new Packet(
                MessageType.ERROR);

        errorPacket.setMessage(
                message);

        sendPacket(errorPacket);
    }

    private synchronized void sendPacket(
            Packet packet) {

        writer.println(
                gson.toJson(packet));

        if (writer.checkError()) {
            System.out.println(
                    "Gagal mengirim packet ke "
                            + username);
        }
    }
}
