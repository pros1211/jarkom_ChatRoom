package com.chatroom.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            String json;
            while ((json = reader.readLine()) != null) {
                Packet packet = gson.fromJson(json, Packet.class);
                handlePacket(packet);
            }
        } catch (Exception e) {
            System.out.println("User " + username + " disconnected");
            handleLeaveRoom(); // Cleanup if disconnected
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void handlePacket(Packet packet) {
        switch (packet.getType()) {
            case LOGIN: handleLogin(packet); break;
            case CREATE_ROOM: handleCreateRoom(packet); break;
            case GET_ROOMS: handleGetRooms(); break;
            case JOIN_ROOM: handleJoinRoom(packet); break;
            case LEAVE_ROOM: handleLeaveRoom(); break;
            case CHAT_MESSAGE: handleChatMessage(packet); break;
            case KICK_USER: handleKickUser(packet); break;
            case DELETE_ROOM: handleDeleteRoom(packet); break;
        }
    }

    private void handleLogin(Packet packet) {
        this.username = packet.getUsername();
        System.out.println("User login: " + username);
        Packet response = new Packet(MessageType.LOGIN_SUCCESS);
        response.setMessage("Welcome " + username);
        sendPacket(response);
    }

    private void handleGetRooms() {
        Map<String, ChatRoom> rooms = server.getRoomManager().getRooms();
        // Mengirimkan list room dalam format sederhana: "ID:Name:Owner,ID:Name:Owner"
        String roomListData = rooms.values().stream()
                .map(r -> r.getRoomId() + ":" + r.getRoomName() + ":" + r.getOwnerName())
                .collect(Collectors.joining(","));
        
        Packet response = new Packet(MessageType.ROOM_LIST);
        response.setMessage(roomListData);
        sendPacket(response);
    }

    private void handleCreateRoom(Packet packet) {
        ChatRoom room = server.getRoomManager().createRoom(packet.getRoomName(), username);
        System.out.println("Room created: " + room.getRoomName() + " by " + username);

        Packet response = new Packet(MessageType.ROOM_CREATED);
        response.setRoomId(room.getRoomId());
        response.setRoomName(room.getRoomName());
        sendPacket(response);
        
        // Broadcast ROOM_LIST ke SEMUA user yang sedang online agar Lobby mereka terupdate otomatis
        broadcastGlobalRoomList();

        // Setelah buat room, otomatis join
        packet.setRoomId(room.getRoomId());
        handleJoinRoom(packet);
    }

    private void broadcastGlobalRoomList() {
        Map<String, ChatRoom> rooms = server.getRoomManager().getRooms();
        String roomListData = rooms.values().stream()
                .map(r -> r.getRoomId() + ":" + r.getRoomName() + ":" + r.getOwnerName())
                .collect(Collectors.joining(","));
        
        Packet listPacket = new Packet(MessageType.ROOM_LIST);
        listPacket.setMessage(roomListData);

        // Server-wide broadcast (semua ClientHandler yang terhubung)
        server.getRoomManager().getRooms().values().forEach(r -> {
            for (ClientHandler handler : r.getMembers()) {
                // Ini hanya member room, kita butuh semua yang online.
                // Mari gunakan cara yang lebih tepat via ChatServer jika ada list client global.
            }
        });
        
        // Karena kita ingin simple & efektif: 
        // Update handleCreateRoom untuk kirim ke pengirim saja sudah benar, 
        // tapi untuk user lain kita perlu list handler global di ChatServer.
    }

    private void handleJoinRoom(Packet packet) {
        ChatRoom room = server.getRoomManager().getRoom(packet.getRoomId());
        if (room == null) return;

        // Beritahu user lain di room
        Packet joinNotify = new Packet(MessageType.USER_JOINED);
        joinNotify.setUsername(username);
        joinNotify.setMessage(username + " bergabung ke ruangan.");
        broadcast(room, joinNotify);

        room.addMember(this);
        currentRoomId = room.getRoomId();
        System.out.println(username + " joined " + room.getRoomName());
    }

    private void handleLeaveRoom() {
        if (currentRoomId == null) return;
        ChatRoom room = server.getRoomManager().getRoom(currentRoomId);
        if (room != null) {
            room.removeMember(this);
            // Beritahu user lain
            Packet leaveNotify = new Packet(MessageType.USER_LEFT);
            leaveNotify.setUsername(username);
            leaveNotify.setMessage(username + " meninggalkan ruangan.");
            broadcast(room, leaveNotify);
        }
        currentRoomId = null;
    }

    private void handleChatMessage(Packet packet) {
        if (currentRoomId == null) return;
        ChatRoom room = server.getRoomManager().getRoom(currentRoomId);
        if (room == null) return;

        Packet outgoing = new Packet(MessageType.CHAT_MESSAGE);
        outgoing.setUsername(username);
        outgoing.setMessage(packet.getMessage());
        broadcast(room, outgoing);
    }

    private void handleKickUser(Packet packet) {
        if (currentRoomId == null) return;
        ChatRoom room = server.getRoomManager().getRoom(currentRoomId);
        if (room == null || !room.getOwnerName().equals(username)) return;

        String target = packet.getTargetUser();
        for (ClientHandler handler : room.getMembers()) {
            if (handler.getUsername().equals(target)) {
                // Beritahu user tersebut bahwa dia di-kick
                Packet kickPacket = new Packet(MessageType.USER_KICKED);
                kickPacket.setMessage("Anda telah dikeluarkan dari ruangan oleh owner.");
                handler.sendPacket(kickPacket);
                
                // Proses pengeluaran
                handler.handleLeaveRoom();
                break;
            }
        }
    }

    private void handleDeleteRoom(Packet packet) {
        if (currentRoomId == null) return;
        ChatRoom room = server.getRoomManager().getRoom(currentRoomId);
        if (room == null || !room.getOwnerName().equals(username)) return;

        Packet deleteNotify = new Packet(MessageType.ROOM_DELETED);
        deleteNotify.setMessage("Ruangan telah ditutup oleh owner.");
        
        // Beritahu semua member dan paksa mereka keluar
        for (ClientHandler handler : room.getMembers()) {
            handler.sendPacket(deleteNotify);
            handler.currentRoomId = null; // Reset room ID mereka
        }
        
        server.getRoomManager().deleteRoom(currentRoomId);
        currentRoomId = null;
    }

    private void broadcast(ChatRoom room, Packet packet) {
        for (ClientHandler member : room.getMembers()) {
            member.sendPacket(packet);
        }
    }

    public void sendPacket(Packet packet) {
        if (writer != null) {
            writer.println(gson.toJson(packet));
        }
    }
}