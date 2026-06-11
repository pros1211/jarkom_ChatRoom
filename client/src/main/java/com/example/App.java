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

public class App extends Application {

    private Stage primaryStage;
    private String currentUser = "";
    private ChatClient chatClient;
    private LobbyView lobbyView;
    private ChatView chatView;

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
        
        primaryStage.setScene(new Scene(chatView, 900, 600));
        primaryStage.setTitle("ChatApp - " + room.getName());
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

    @Override
    public void stop() {
        chatClient.disconnect();
    }

    public static void main(String[] args) {
        launch();
    }
}