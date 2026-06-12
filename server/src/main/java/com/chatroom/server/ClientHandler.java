package com.chatroom.server;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.chatroom.database.DatabaseManager;
import com.chatroom.model.Message;
import com.chatroom.protocol.MessageType;
import com.chatroom.protocol.Packet;
import com.google.gson.Gson;

public class ClientHandler implements Runnable {

    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024L * 1024L;

    private static final int FILE_CHUNK_SIZE_BYTES = 16 * 1024;

    private final Socket socket;
    private String currentRoomId;
    private final ChatServer server;
    private final Gson gson = new Gson();
    private final DatabaseManager databaseManager = new DatabaseManager();
    private final Map<String, IncomingFileUpload> incomingFileUploads = new ConcurrentHashMap<>();
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
            socket.setTcpNoDelay(true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            String json;
            while ((json = reader.readLine()) != null) {
                try {
                    Packet packet = gson.fromJson(json, Packet.class);
                    handlePacket(packet);
                } catch (Exception packetError) {
                    System.out.println("Packet error from " + username + ": " + packetError.getMessage());
                    packetError.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("User " + username + " disconnected: " + e.getMessage());
            e.printStackTrace();
            handleLeaveRoom(); // Cleanup if disconnected
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void handlePacket(Packet packet) {
        if (packet == null || packet.getType() == null) {
            return;
        }

        switch (packet.getType()) {
            case LOGIN: handleLogin(packet); break;
            case CREATE_ROOM: handleCreateRoom(packet); break;
            case GET_ROOMS: handleGetRooms(); break;
            case JOIN_ROOM: handleJoinRoom(packet); break;
            case LEAVE_ROOM: handleLeaveRoom(); break;
            case CHAT_MESSAGE: handleChatMessage(packet); break;
            case FILE_MESSAGE: handleFileMessage(packet); break;
            case FILE_CHUNK: handleFileChunk(packet); break;
            case KICK_USER: handleKickUser(packet); break;
            case DELETE_ROOM: handleDeleteRoom(packet); break;
        }
    }

    private void handleLogin(Packet packet) {
        this.username = packet.getUsername();
        System.out.println("User login: " + username);
        saveUserToDatabase(username);

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
        room.addMember(this);
        currentRoomId = room.getRoomId();
        saveRoomToDatabase(room);

        Packet response = new Packet(MessageType.ROOM_CREATED);
        response.setRoomId(room.getRoomId());
        response.setRoomName(room.getRoomName());
        sendPacket(response);
        sendMessageHistory(room.getRoomId());
        sendFileHistory(room.getRoomId());
        
        // Broadcast ROOM_LIST ke SEMUA user yang sedang online agar Lobby mereka terupdate otomatis
        broadcastGlobalRoomList();

        System.out.println(username + " joined " + room.getRoomName());
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

        room.addMember(this);
        currentRoomId = room.getRoomId();
        sendMessageHistory(room.getRoomId());
        sendFileHistory(room.getRoomId());

        // Beritahu user lain di room
        Packet joinNotify = new Packet(MessageType.USER_JOINED);
        joinNotify.setUsername(username);
        joinNotify.setMessage(username + " bergabung ke ruangan.");
        broadcast(room, joinNotify);
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
        saveMessageToDatabase(currentRoomId, username, packet.getMessage());
        broadcast(room, outgoing);
    }

    private void handleFileMessage(Packet packet) {
        if (currentRoomId == null) return;
        ChatRoom room = server.getRoomManager().getRoom(currentRoomId);
        if (room == null) return;
        if (packet.getFileSize() > MAX_FILE_SIZE_BYTES) {
            sendError("Ukuran file maksimal 50 MB.");
            return;
        }

        Packet outgoing = new Packet(MessageType.FILE_MESSAGE);
        outgoing.setUsername(username);
        outgoing.setFileName(packet.getFileName());
        outgoing.setFileMimeType(packet.getFileMimeType());
        outgoing.setFileData(packet.getFileData());
        outgoing.setFileSize(packet.getFileSize());
        outgoing.setMessage(packet.getMessage());
        broadcast(room, outgoing);
    }

    private void handleFileChunk(Packet packet) {
        if (currentRoomId == null) return;
        ChatRoom room = server.getRoomManager().getRoom(currentRoomId);
        if (room == null) return;
        if (packet.getFileSize() > MAX_FILE_SIZE_BYTES) {
            sendError("Ukuran file maksimal 50 MB.");
            return;
        }

        Packet outgoing = new Packet(MessageType.FILE_CHUNK);
        outgoing.setUsername(username);
        outgoing.setFileId(packet.getFileId());
        outgoing.setFileName(packet.getFileName());
        outgoing.setFileMimeType(packet.getFileMimeType());
        outgoing.setFileData(packet.getFileData());
        outgoing.setFileSize(packet.getFileSize());
        outgoing.setChunkIndex(packet.getChunkIndex());
        outgoing.setTotalChunks(packet.getTotalChunks());
        outgoing.setMessage(packet.getMessage());
        broadcast(room, outgoing);
        storeFileChunk(packet);
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

    private void sendError(String message) {
        Packet errorPacket = new Packet(MessageType.ERROR);
        errorPacket.setMessage(message);
        sendPacket(errorPacket);
    }

    private void saveUserToDatabase(String username) {
        try {
            databaseManager.saveUser(username);
        } catch (Exception e) {
            System.out.println("Gagal menyimpan user ke database: " + e.getMessage());
        }
    }

    private void saveRoomToDatabase(ChatRoom room) {
        try {
            databaseManager.saveRoom(room.getRoomId(), room.getRoomName(), room.getOwnerName());
        } catch (Exception e) {
            System.out.println("Gagal menyimpan room ke database: " + e.getMessage());
        }
    }

    private void saveMessageToDatabase(String roomId, String sender, String content) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        try {
            databaseManager.saveMessage(roomId, sender, content);
        } catch (Exception e) {
            System.out.println("Gagal menyimpan pesan ke database: " + e.getMessage());
        }
    }

    private void saveFileMetadataToDatabase(IncomingFileUpload upload, Path savedPath) {
        try {
            databaseManager.saveFileMetadata(
                    upload.roomId,
                    upload.sender,
                    upload.fileName,
                    savedPath.toString(),
                    upload.mimeType,
                    upload.fileSize);
        } catch (Exception e) {
            System.out.println("Gagal menyimpan metadata file ke database: " + e.getMessage());
        }
    }

    private void storeFileChunk(Packet packet) {
        if (packet.getFileId() == null || packet.getTotalChunks() <= 0 || packet.getFileData() == null) {
            return;
        }

        String uploadKey = username + ":" + packet.getFileId();

        try {
            IncomingFileUpload upload = incomingFileUploads.computeIfAbsent(
                    uploadKey,
                    key -> new IncomingFileUpload(
                            currentRoomId,
                            username,
                            packet.getFileName(),
                            packet.getFileMimeType(),
                            packet.getFileSize(),
                            packet.getTotalChunks()));

            if (upload.addChunk(packet)) {
                incomingFileUploads.remove(uploadKey);
                Path savedPath = saveUploadToDisk(upload);
                saveFileMetadataToDatabase(upload, savedPath);
                saveMessageToDatabase(upload.roomId, upload.sender, "[FILE] " + upload.fileName);
            }
        } catch (Exception e) {
            incomingFileUploads.remove(uploadKey);
            System.out.println("Gagal menyimpan file upload: " + e.getMessage());
        }
    }

    private Path saveUploadToDisk(IncomingFileUpload upload)
            throws IOException {

        Path uploadDir = Paths.get("uploads", upload.roomId);
        Files.createDirectories(uploadDir);

        String storedName = System.currentTimeMillis() + "_" + sanitizeFileName(upload.fileName);
        Path savedPath = uploadDir.resolve(storedName);
        Files.write(savedPath, upload.toByteArray());

        return savedPath;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "file";
        }

        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void sendMessageHistory(String roomId) {
        try {
            List<Message> messages = databaseManager.getMessagesByRoom(roomId);
            Packet historyPacket = new Packet(MessageType.MESSAGE_HISTORY);
            historyPacket.setRoomId(roomId);
            historyPacket.setMessage(gson.toJson(messages));
            sendPacket(historyPacket);
        } catch (Exception e) {
            System.out.println("Gagal mengambil riwayat chat dari database: " + e.getMessage());
        }
    }

    private void sendFileHistory(String roomId) {
        try {
            List<DatabaseManager.StoredFile> storedFiles = databaseManager.getFilesByRoom(roomId);
            for (DatabaseManager.StoredFile storedFile : storedFiles) {
                sendStoredFile(storedFile);
            }
        } catch (Exception e) {
            System.out.println("Gagal mengambil riwayat file dari database: " + e.getMessage());
        }
    }

    private void sendStoredFile(DatabaseManager.StoredFile storedFile) {
        try {
            Path filePath = Paths.get(storedFile.getFilePath());
            if (!Files.exists(filePath) || storedFile.getFileSize() > MAX_FILE_SIZE_BYTES) {
                return;
            }

            byte[] bytes = Files.readAllBytes(filePath);
            String fileId = "history-" + storedFile.getId() + "-" + System.currentTimeMillis();
            int totalChunks = Math.max(1, (int) Math.ceil(bytes.length / (double) FILE_CHUNK_SIZE_BYTES));

            for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                int start = chunkIndex * FILE_CHUNK_SIZE_BYTES;
                int end = Math.min(bytes.length, start + FILE_CHUNK_SIZE_BYTES);
                byte[] chunk = new byte[end - start];
                System.arraycopy(bytes, start, chunk, 0, chunk.length);

                Packet filePacket = new Packet(MessageType.FILE_CHUNK);
                filePacket.setUsername(storedFile.getSender());
                filePacket.setFileId(fileId);
                filePacket.setFileName(storedFile.getFileName());
                filePacket.setFileMimeType(storedFile.getMimeType());
                filePacket.setFileSize(bytes.length);
                filePacket.setChunkIndex(chunkIndex);
                filePacket.setTotalChunks(totalChunks);
                filePacket.setFileData(Base64.getEncoder().encodeToString(chunk));
                filePacket.setMessage("Riwayat file: " + storedFile.getFileName());
                sendPacket(filePacket);
            }
        } catch (Exception e) {
            System.out.println("Gagal mengirim riwayat file " + storedFile.getFileName() + ": " + e.getMessage());
        }
    }

    public synchronized void sendPacket(Packet packet) {
        if (writer != null) {
            writer.println(gson.toJson(packet));
            if (writer.checkError()) {
                System.out.println("Gagal mengirim packet ke " + username);
            }
        }
    }

    private static class IncomingFileUpload {
        private final String roomId;
        private final String sender;
        private final String fileName;
        private final String mimeType;
        private final long fileSize;
        private final byte[][] chunks;
        private int receivedChunks;

        private IncomingFileUpload(
                String roomId,
                String sender,
                String fileName,
                String mimeType,
                long fileSize,
                int totalChunks) {

            this.roomId = roomId;
            this.sender = sender;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
            this.chunks = new byte[totalChunks][];
        }

        private boolean addChunk(Packet packet) {
            int chunkIndex = packet.getChunkIndex();
            if (chunkIndex < 0 || chunkIndex >= chunks.length) {
                return false;
            }

            if (chunks[chunkIndex] == null) {
                chunks[chunkIndex] = Base64.getDecoder().decode(packet.getFileData());
                receivedChunks++;
            }

            return receivedChunks == chunks.length;
        }

        private byte[] toByteArray() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) fileSize);
            for (byte[] chunk : chunks) {
                if (chunk != null) {
                    outputStream.write(chunk, 0, chunk.length);
                }
            }
            return outputStream.toByteArray();
        }
    }
}
