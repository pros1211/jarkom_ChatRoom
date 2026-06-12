package com.example;

import atlantafx.base.theme.PrimerLight;
import com.example.models.Room;
import com.example.network.ChatClient;
import com.example.protocol.MessageType;
import com.example.protocol.Packet;
import com.example.views.ChatView;
import com.example.views.LobbyView;
import com.example.views.LoginView;
import com.example.views.CreateRoomDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App extends Application {

    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024L * 1024L;
    private static final int FILE_CHUNK_SIZE_BYTES = 16 * 1024;

    private Stage primaryStage;
    private String currentUser = "";
    private ChatClient chatClient;
    private LobbyView lobbyView;
    private ChatView chatView;
    private final Gson gson = new Gson();
    private final Map<String, IncomingFile> incomingFiles = new HashMap<>();

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.chatClient = new ChatClient();
        
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        
        chatClient.setOnPacketReceived(this::handleIncomingPacket);
        showLogin();
    }

    private void showLogin() {
        LoginView loginView = new LoginView(username -> {
            this.currentUser = username;
            chatClient.connect("localhost", 5000, () -> {
                Packet loginPacket = new Packet(MessageType.LOGIN);
                loginPacket.setUsername(username);
                chatClient.sendPacket(loginPacket);
            }, error -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, error);
                alert.show();
            });
        });
        primaryStage.setScene(new Scene(loginView, 450, 450));
        primaryStage.setTitle("ChatApp - Login");
        primaryStage.show();
    }

    private void showLobby() {
        this.lobbyView = new LobbyView(
            currentUser, 
            room -> {
                Packet joinPacket = new Packet(MessageType.JOIN_ROOM);
                joinPacket.setRoomId(room.getRoomId());
                chatClient.sendPacket(joinPacket);
                showChat(room);
            }, 
            this::handleCreateRoom,
            () -> chatClient.sendPacket(new Packet(MessageType.GET_ROOMS))
        );
        primaryStage.setScene(new Scene(lobbyView, 900, 600));
        primaryStage.setTitle("ChatApp - Lobby");
        chatClient.sendPacket(new Packet(MessageType.GET_ROOMS));
    }

    private void handleCreateRoom() {
        CreateRoomDialog dialog = new CreateRoomDialog();
        dialog.initOwner(primaryStage);
        dialog.showAndWait().ifPresent(roomName -> {
            if (!roomName.isEmpty()) {
                Packet createPacket = new Packet(MessageType.CREATE_ROOM);
                createPacket.setRoomName(roomName);
                chatClient.sendPacket(createPacket);
            }
        });
    }

    private Room currentRoom;

    private void showChat(Room room) {
        this.currentRoom = room;
        this.chatView = new ChatView(
            room, 
            currentUser, 
            () -> {
                chatClient.sendPacket(new Packet(MessageType.LEAVE_ROOM));
                showLobby();
            },
            () -> {
                Packet deletePacket = new Packet(MessageType.DELETE_ROOM);
                deletePacket.setRoomId(room.getRoomId());
                chatClient.sendPacket(deletePacket);
            }
        );
        
        chatView.setOnSendMessage(text -> {
            Packet chatPacket = new Packet(MessageType.CHAT_MESSAGE);
            chatPacket.setMessage(text);
            chatClient.sendPacket(chatPacket);
        });

        chatView.setOnKickUser(targetUser -> {
            Packet kickPacket = new Packet(MessageType.KICK_USER);
            kickPacket.setTargetUser(targetUser);
            chatClient.sendPacket(kickPacket);
        });

        chatView.setOnTypingStatus(isTyping -> {
            Packet typingPacket = new Packet(MessageType.TYPING_STATUS);
            typingPacket.setTyping(isTyping);
            chatClient.sendPacket(typingPacket);
        });

        chatView.setOnSendFile(this::handleSendFile);
        
        primaryStage.setScene(new Scene(chatView, 900, 600));
        primaryStage.setTitle("ChatApp - " + room.getName());
    }

    private void handleSendFile(File file) {
        if (file == null || !file.isFile()) return;
        if (file.length() > MAX_FILE_SIZE_BYTES) {
            new Alert(Alert.AlertType.WARNING, "Max 50 MB.").show();
            return;
        }
        new Thread(() -> sendFileChunks(file)).start();
    }

    private void sendFileChunks(File file) {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) mimeType = "application/octet-stream";
            String fileId = currentUser + "-" + System.currentTimeMillis();
            int totalChunks = (int) Math.ceil(file.length() / (double) FILE_CHUNK_SIZE_BYTES);
            byte[] buffer = new byte[FILE_CHUNK_SIZE_BYTES];
            int chunkIndex = 0;
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                Packet filePacket = new Packet(MessageType.FILE_CHUNK);
                filePacket.setFileId(fileId);
                filePacket.setFileName(file.getName());
                filePacket.setFileMimeType(mimeType);
                filePacket.setFileSize(file.length());
                filePacket.setChunkIndex(chunkIndex++);
                filePacket.setTotalChunks(totalChunks);
                filePacket.setFileData(Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, bytesRead)));
                chatClient.sendPacketNow(filePacket);
            }
        } catch (IOException ignored) {}
    }

    private void handleIncomingPacket(Packet packet) {
        Platform.runLater(() -> {
            switch (packet.getType()) {
                case LOGIN_SUCCESS: showLobby(); break;
                case ROOM_LIST:
                    if (lobbyView != null && packet.getMessage() != null) {
                        lobbyView.clearRooms();
                        for (String roomStr : packet.getMessage().split(",")) {
                            String[] parts = roomStr.split(":");
                            if (parts.length >= 3) lobbyView.addRoom(new Room(parts[0], parts[1], parts[2]));
                        }
                    }
                    break;
                case ROOM_CREATED:
                    showChat(new Room(packet.getRoomId(), packet.getRoomName(), currentUser));
                    break;
                case MESSAGE_HISTORY: handleMessageHistory(packet); break;
                case CHAT_MESSAGE:
                    if (chatView != null) chatView.addMessage(packet.getUsername(), packet.getMessage(), packet.getUsername().equals(currentUser));
                    break;
                case FILE_MESSAGE:
                    if (chatView != null) chatView.addFileMessage(packet.getUsername(), packet.getFileName(), packet.getFileMimeType(), packet.getFileSize(), packet.getFileData(), packet.getUsername().equals(currentUser));
                    break;
                case FILE_CHUNK: handleFileChunk(packet); break;
                case USER_JOINED:
                    if (currentRoom != null && chatView != null) {
                        if (!currentRoom.getParticipants().contains(packet.getUsername())) {
                            currentRoom.getParticipants().add(packet.getUsername());
                            chatView.refreshParticipants();
                        }
                        chatView.addMessage("Sistem", packet.getMessage(), false);
                    }
                    break;
                case USER_LEFT:
                    if (currentRoom != null && chatView != null) {
                        currentRoom.getParticipants().remove(packet.getUsername());
                        chatView.refreshParticipants();
                        chatView.addMessage("Sistem", packet.getMessage(), false);
                    }
                    break;
                case ERROR: new Alert(Alert.AlertType.ERROR, packet.getMessage()).show(); break;
                case USER_KICKED:
                case ROOM_DELETED:
                    new Alert(Alert.AlertType.INFORMATION, packet.getMessage()).show();
                    showLobby();
                    break;
                case TYPING_STATUS:
                    if (chatView != null) chatView.setPlayerTyping(packet.getUsername(), packet.isTyping());
                    break;
                case USER_PRESENCE:
                    if (chatView != null) chatView.refreshParticipants();
                    break;
            }
        });
    }

    private void handleMessageHistory(Packet packet) {
        if (chatView == null || packet.getMessage() == null) return;
        Type historyType = new TypeToken<List<HistoryMessage>>() {}.getType();
        List<HistoryMessage> messages = gson.fromJson(packet.getMessage(), historyType);
        if (messages == null) return;
        for (HistoryMessage message : messages) {
            if (message.content != null && message.content.startsWith("[FILE] ")) continue;
            chatView.addMessage(message.sender, message.content, message.sender.equals(currentUser));
        }
    }

    private void handleFileChunk(Packet packet) {
        IncomingFile incomingFile = incomingFiles.computeIfAbsent(packet.getFileId(), id -> new IncomingFile(packet));
        if (incomingFile.addChunk(packet)) {
            incomingFiles.remove(packet.getFileId());
            if (chatView != null) {
                chatView.addFileMessage(incomingFile.sender, incomingFile.fileName, incomingFile.fileMimeType, incomingFile.fileSize, incomingFile.toByteArray(), incomingFile.sender.equals(currentUser));
            }
        }
    }

    private static class IncomingFile {
        private final String sender, fileName, fileMimeType;
        private final long fileSize;
        private final byte[][] chunks;
        private int receivedChunks;
        private IncomingFile(Packet p) {
            this.sender = p.getUsername(); this.fileName = p.getFileName();
            this.fileMimeType = p.getFileMimeType(); this.fileSize = p.getFileSize();
            this.chunks = new byte[p.getTotalChunks()][];
        }
        private boolean addChunk(Packet p) {
            if (chunks[p.getChunkIndex()] == null) {
                chunks[p.getChunkIndex()] = Base64.getDecoder().decode(p.getFileData());
                receivedChunks++;
            }
            return receivedChunks == chunks.length;
        }
        private byte[] toByteArray() {
            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) fileSize);
            for (byte[] c : chunks) if (c != null) bos.write(c, 0, c.length);
            return bos.toByteArray();
        }
    }

    private static class HistoryMessage { String sender, content; }

    @Override public void stop() { chatClient.disconnect(); }
    public static void main(String[] args) { launch(); }
}