package com.example.views;

import com.example.models.Room;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;

import java.util.function.Consumer;

public class LobbyView extends BorderPane {

    private final ListView<Room> roomList;

    public LobbyView(String username, Consumer<Room> onJoinRoom, Runnable onCreateRoom, Runnable onRefreshRooms) {
        // --- 1. Global Styling ---
        setStyle("-fx-background-color: #f0f2f5;");

        // --- 2. Header (Rounded & Floating) ---
        HBox header = new HBox(15);
        header.setPadding(new Insets(15, 25, 15, 25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: white; -fx-background-radius: 0 0 20 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        Label logo = new Label("ChatApp");
        logo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #007AFF;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Refresh Button
        Button refreshBtn = new Button();
        refreshBtn.setGraphic(new FontIcon(MaterialDesignA.AUTORENEW));
        refreshBtn.getStyleClass().add("flat");
        refreshBtn.setTooltip(new Tooltip("Refresh Daftar Room"));
        refreshBtn.setOnAction(e -> {
            javafx.animation.RotateTransition rt = new javafx.animation.RotateTransition(
                    javafx.util.Duration.millis(500), refreshBtn.getGraphic());
            rt.setByAngle(360);
            rt.play();
            onRefreshRooms.run();
        });

        HBox userBadge = new HBox(10);
        userBadge.setAlignment(Pos.CENTER);
        userBadge.setPadding(new Insets(5, 15, 5, 5));
        userBadge.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 20;");

        Label avatar = new Label(username.substring(0, 1).toUpperCase());
        avatar.setAlignment(Pos.CENTER);
        avatar.setPrefSize(30, 30);
        avatar.setStyle(
                "-fx-background-color: #007AFF; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-weight: bold;");

        Label userLabel = new Label(username);
        userLabel.setStyle("-fx-font-weight: bold;");

        userBadge.getChildren().addAll(avatar, userLabel);
        header.getChildren().addAll(logo, spacer, refreshBtn, userBadge);

        // --- 3. Sidebar (Clean & Spaced) ---
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(280);

        Label sidebarTitle = new Label("ROOMS");
        sidebarTitle.setStyle(
                "-fx-font-weight: bold; -fx-text-fill: #8e8e93; -fx-font-size: 11px; -fx-letter-spacing: 1px;");

        roomList = new ListView<>();
        roomList.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");

        roomList.setCellFactory(param -> new ListCell<Room>() {
            @Override
            protected void updateItem(Room item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox card = new HBox(12);
                    card.setAlignment(Pos.CENTER_LEFT);
                    card.setPadding(new Insets(12));
                    card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-margin: 5 0 5 0;");

                    StackPane iconContainer = new StackPane(new FontIcon(MaterialDesignC.CHAT_OUTLINE));
                    iconContainer.setPrefSize(36, 36);
                    iconContainer.setStyle(
                            "-fx-background-color: #e8f2ff; -fx-background-radius: 10; -fx-text-fill: #007AFF;");

                    VBox info = new VBox(2);
                    Label name = new Label(item.getName());
                    name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                    Label owner = new Label("oleh " + item.getOwnerName());
                    owner.setStyle("-fx-font-size: 11px; -fx-text-fill: #8e8e93;");

                    info.getChildren().addAll(name, owner);
                    if (item.getMaxMembers() > 0) {
                        Label limit = new Label("maks. " + item.getMaxMembers() + " peserta");
                        limit.setStyle("-fx-font-size: 11px; -fx-text-fill: #6e6e73;");
                        info.getChildren().add(limit);
                    }
                    card.getChildren().addAll(iconContainer, info);

                    setGraphic(card);

                    card.setOnMouseEntered(e -> card
                            .setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 12; -fx-cursor: hand;"));
                    card.setOnMouseExited(
                            e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 12;"));
                }
            }
        });

        VBox.setVgrow(roomList, Priority.ALWAYS);

        roomList.setOnMouseClicked(event -> {
            Room selectedRoom = roomList.getSelectionModel().getSelectedItem();
            if (selectedRoom != null) {
                onJoinRoom.accept(selectedRoom);
            }
        });

        Button createRoomBtn = new Button("Buat Room Baru");
        createRoomBtn.setGraphic(new FontIcon(MaterialDesignP.PLUS));
        createRoomBtn.setStyle(
                "-fx-background-color: #007AFF; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-weight: bold; -fx-padding: 10 20 10 20;");
        createRoomBtn.setMaxWidth(Double.MAX_VALUE);
        createRoomBtn.setOnAction(e -> onCreateRoom.run());

        sidebar.getChildren().addAll(sidebarTitle, roomList, createRoomBtn);

        // --- 4. Main Area ---
        VBox mainArea = new VBox(25);
        mainArea.setAlignment(Pos.CENTER);
        mainArea.setPadding(new Insets(40));

        VBox welcomeCard = new VBox(20);
        welcomeCard.setAlignment(Pos.CENTER);
        welcomeCard.setPadding(new Insets(60));
        welcomeCard.setStyle(
                "-fx-background-color: white; -fx-background-radius: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 20, 0, 0, 10);");
        welcomeCard.setMaxWidth(500);

        FontIcon roomIcon = new FontIcon(MaterialDesignC.CHAT_OUTLINE);
        roomIcon.setIconSize(80);
        roomIcon.setStyle("-fx-text-fill: #e1e1e1;");

        Label welcomeLabel = new Label("Siap untuk Chatting?");
        welcomeLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1d1d1f;");

        Label instructionLabel = new Label(
                "Pilih ruangan dari daftar di samping\natau buat ruangan baru untuk mulai mengobrol.");
        instructionLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        instructionLabel.setStyle("-fx-text-fill: #8e8e93; -fx-font-size: 14px; -fx-line-spacing: 5;");

        welcomeCard.getChildren().addAll(roomIcon, welcomeLabel, instructionLabel);
        mainArea.getChildren().add(welcomeCard);

        setTop(header);
        setLeft(sidebar);
        setCenter(mainArea);
    }

    public void addRoom(Room room) {
        roomList.getItems().add(room);
    }

    public void clearRooms() {
        roomList.getItems().clear();
    }

    public void removeRoom(Room room) {
        roomList.getItems().remove(room);
    }
}
