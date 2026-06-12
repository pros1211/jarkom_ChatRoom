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
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class App extends Application {

    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024L * 1024L;
    private static final int FILE_CHUNK_SIZE_BYTES = 16 * 1024;

    private Stage primaryStage;
    private String currentUser = "";
    private ChatClient chatClient;
    private LobbyView lobbyView;
    private ChatView chatView;
    private final Map<String, IncomingFile> incomingFiles = new HashMap<>();

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.chatClient = new ChatClient();
        
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        
        // Handle incoming packets globally
        chatClient.setOnPacketReceived(this::handleIncomingPacket);
        
        showLogin();
    }

    private void showLogin() {
        LoginView loginView = new LoginView(username -> {
            this.currentUser = username;
            // Hubungkan ke localhost port 5000 (sesuai ServerLauncher)
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
        
        // Minta daftar room terbaru
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
        
        // Hubungkan input teks ke Socket
        chatView.setOnSendMessage(text -> {
            Packet chatPacket = new Packet(MessageType.CHAT_MESSAGE);
            chatPacket.setMessage(text);
            chatClient.sendPacket(chatPacket);
        });

        // Hubungkan aksi Kick ke Socket
        chatView.setOnKickUser(targetUser -> {
            Packet kickPacket = new Packet(MessageType.KICK_USER);
            kickPacket.setTargetUser(targetUser);
            chatClient.sendPacket(kickPacket);
        });

        chatView.setOnSendFile(this::handleSendFile);
        
        primaryStage.setScene(new Scene(chatView, 900, 600));
        primaryStage.setTitle("ChatApp - " + room.getName());
    }

    private void handleSendFile(File file) {
        if (file == null || !file.isFile()) {
            return;
        }

        if (file.length() > MAX_FILE_SIZE_BYTES) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Ukuran file maksimal 50 MB.");
            alert.show();
            return;
        }

        Thread uploadThread = new Thread(() -> sendFileChunks(file), "file-upload-" + file.getName());
        uploadThread.setDaemon(true);
        uploadThread.start();
    }

    private void sendFileChunks(File file) {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            String fileId = currentUser + "-" + System.currentTimeMillis() + "-" + Math.abs(file.getName().hashCode());
            int totalChunks = Math.max(1, (int) Math.ceil(file.length() / (double) FILE_CHUNK_SIZE_BYTES));
            byte[] buffer = new byte[FILE_CHUNK_SIZE_BYTES];
            int chunkIndex = 0;
            int bytesRead;

            if (file.length() == 0) {
                sendFileChunk(file, mimeType, fileId, 0, totalChunks, new byte[0]);
                return;
            }

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] chunk = Arrays.copyOf(buffer, bytesRead);
                if (!sendFileChunk(file, mimeType, fileId, chunkIndex, totalChunks, chunk)) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Koneksi ke server terputus saat mengirim file.");
                        alert.show();
                    });
                    return;
                }
                chunkIndex++;
            }
        } catch (IOException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Gagal membaca file: " + e.getMessage());
                alert.show();
            });
        }
    }

    private boolean sendFileChunk(File file, String mimeType, String fileId, int chunkIndex, int totalChunks, byte[] chunk) {
        Packet filePacket = new Packet(MessageType.FILE_CHUNK);
        filePacket.setFileId(fileId);
        filePacket.setFileName(file.getName());
        filePacket.setFileMimeType(mimeType);
        filePacket.setFileSize(file.length());
        filePacket.setChunkIndex(chunkIndex);
        filePacket.setTotalChunks(totalChunks);
        filePacket.setFileData(Base64.getEncoder().encodeToString(chunk));
        filePacket.setMessage("Mengirim file: " + file.getName());
        return chatClient.sendPacketNow(filePacket);
    }

    private void handleIncomingPacket(Packet packet) {
        switch (packet.getType()) {
            case LOGIN_SUCCESS:
                showLobby();
                break;
                
            case ROOM_LIST:
                if (lobbyView != null && packet.getMessage() != null) {
                    Platform.runLater(() -> {
                        lobbyView.clearRooms();
                        String data = packet.getMessage();
                        if (!data.isEmpty()) {
                            for (String roomStr : data.split(",")) {
                                String[] parts = roomStr.split(":");
                                if (parts.length >= 3) {
                                    lobbyView.addRoom(new Room(parts[0], parts[1], parts[2]));
                                }
                            }
                        }
                    });
                }
                break;

            case ROOM_CREATED:
                Room newRoom = new Room(packet.getRoomId(), packet.getRoomName(), currentUser);
                showChat(newRoom);
                break;

            case CHAT_MESSAGE:
                if (chatView != null) {
                    chatView.addMessage(packet.getUsername(), packet.getMessage(), packet.getUsername().equals(currentUser));
                }
                break;

            case FILE_MESSAGE:
                if (chatView != null) {
                    chatView.addFileMessage(
                        packet.getUsername(),
                        packet.getFileName(),
                        packet.getFileMimeType(),
                        packet.getFileSize(),
                        packet.getFileData(),
                        packet.getUsername().equals(currentUser)
                    );
                }
                break;

            case FILE_CHUNK:
                handleFileChunk(packet);
                break;

            case USER_JOINED:
                if (currentRoom != null && chatView != null) {
                    if (!currentRoom.getParticipants().contains(packet.getUsername())) {
                        currentRoom.getParticipants().add(packet.getUsername());
                        Platform.runLater(() -> chatView.refreshParticipants());
                    }
                    chatView.addMessage("Sistem", packet.getMessage(), false);
                }
                break;

            case USER_LEFT:
                if (currentRoom != null && chatView != null) {
                    currentRoom.getParticipants().remove(packet.getUsername());
                    Platform.runLater(() -> chatView.refreshParticipants());
                    chatView.addMessage("Sistem", packet.getMessage(), false);
                }
                break;

            case SYSTEM_NOTIFICATION:
                if (chatView != null) {
                    chatView.addMessage("Sistem", packet.getMessage(), false);
                }
                break;

            case ERROR:
                Alert error = new Alert(Alert.AlertType.ERROR, packet.getMessage());
                error.show();
                break;

            case USER_KICKED:
            case ROOM_DELETED:
                Platform.runLater(() -> {
                    Alert info = new Alert(Alert.AlertType.INFORMATION, packet.getMessage());
                    info.show();
                    showLobby();
                });
                break;
        }
    }

    private void handleFileChunk(Packet packet) {
        if (packet.getFileId() == null || packet.getTotalChunks() <= 0) {
            return;
        }

        int maxAllowedChunks = (int) Math.ceil(MAX_FILE_SIZE_BYTES / (double) FILE_CHUNK_SIZE_BYTES) + 1;
        if (packet.getFileSize() > MAX_FILE_SIZE_BYTES || packet.getTotalChunks() > maxAllowedChunks) {
            if (chatView != null) {
                Platform.runLater(() -> chatView.addMessage("Sistem", "File dari " + packet.getUsername() + " melebihi batas ukuran.", false));
            }
            return;
        }

        try {
            IncomingFile incomingFile = incomingFiles.computeIfAbsent(
                packet.getFileId(),
                id -> new IncomingFile(packet)
            );

            if (incomingFile.addChunk(packet)) {
                incomingFiles.remove(packet.getFileId());
                if (chatView != null) {
                    byte[] fileBytes = incomingFile.toByteArray();
                    Platform.runLater(() -> chatView.addFileMessage(
                            incomingFile.sender,
                            incomingFile.fileName,
                            incomingFile.fileMimeType,
                            incomingFile.fileSize,
                            fileBytes,
                            incomingFile.sender.equals(currentUser)
                    ));
                }
            }
        } catch (IllegalArgumentException e) {
            incomingFiles.remove(packet.getFileId());
            if (chatView != null) {
                Platform.runLater(() -> chatView.addMessage("Sistem", "File dari " + packet.getUsername() + " gagal diterima.", false));
            }
        }
    }

    private static class IncomingFile {
        private final String sender;
        private final String fileName;
        private final String fileMimeType;
        private final long fileSize;
        private final byte[][] chunks;
        private int receivedChunks;

        private IncomingFile(Packet firstPacket) {
            this.sender = firstPacket.getUsername();
            this.fileName = firstPacket.getFileName();
            this.fileMimeType = firstPacket.getFileMimeType();
            this.fileSize = firstPacket.getFileSize();
            this.chunks = new byte[firstPacket.getTotalChunks()][];
        }

        private boolean addChunk(Packet packet) {
            int chunkIndex = packet.getChunkIndex();
            if (chunkIndex < 0 || chunkIndex >= chunks.length || packet.getFileData() == null) {
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

    @Override
    public void stop() {
        chatClient.disconnect();
    }

    public static void main(String[] args) {
        launch();
    }
}
