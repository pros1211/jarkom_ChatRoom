package com.example.views;

import com.example.models.Room;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;

import java.util.function.Consumer;

public class ChatView extends BorderPane {
    private final VBox messageContainer;
    private final TextField messageField;
    private final String currentUser;
    private final Room room;

    private final VBox userListContainer;
    private Consumer<String> onKickUser;

    public ChatView(Room room, String username, Runnable onBack, Runnable onCloseRoom) {
        this.room = room;
        this.currentUser = username;

        // ... (Header and other code)
        HBox header = new HBox(15);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-border-color: #d1d1d1; -fx-border-width: 0 0 0.5 0;");

        Button backBtn = new Button();
        backBtn.setGraphic(new FontIcon(MaterialDesignA.ARROW_LEFT));
        backBtn.getStyleClass().add("flat");
        backBtn.setStyle("-fx-text-fill: #007AFF;"); // iOS Blue
        backBtn.setOnAction(e -> onBack.run());

        // Center Info
        VBox titleInfo = new VBox(0);
        titleInfo.setAlignment(Pos.CENTER);
        Label roomTitle = new Label(room.getName());
        roomTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label ownerLabel = new Label(room.getParticipants().size() + " Orang >");
        ownerLabel.setStyle("-fx-text-fill: #8e8e93; -fx-font-size: 11px;");
        titleInfo.getChildren().addAll(roomTitle, ownerLabel);

        Region spacerL = new Region();
        HBox.setHgrow(spacerL, Priority.ALWAYS);
        Region spacerR = new Region();
        HBox.setHgrow(spacerR, Priority.ALWAYS);

        header.getChildren().addAll(backBtn, spacerL, titleInfo, spacerR);

        // Owner/Leave Action
        if (room.isOwner(currentUser)) {
            Button closeBtn = new Button();
            closeBtn.setGraphic(new FontIcon(MaterialDesignD.DELETE));
            closeBtn.getStyleClass().addAll("flat", "danger");
            closeBtn.setOnAction(e -> onCloseRoom.run());
            header.getChildren().add(closeBtn);
        } else {
            Button leaveBtn = new Button("Keluar");
            leaveBtn.getStyleClass().add("flat");
            leaveBtn.setStyle("-fx-text-fill: #FF3B30;"); // iOS Red
            leaveBtn.setOnAction(e -> onBack.run());
            header.getChildren().add(leaveBtn);
        }

        // --- 2. Chat Area ---
        messageContainer = new VBox(8); // Tighter spacing like iMessage
        messageContainer.setPadding(new Insets(20));
        messageContainer.setStyle("-fx-background-color: #ffffff;");
        
        ScrollPane scrollPane = new ScrollPane(messageContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVvalue(1.0);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #ffffff;");

        // --- 3. Input Area (iMessage Pill Style) ---
        HBox inputWrapper = new HBox(10);
        inputWrapper.setPadding(new Insets(10, 20, 20, 20));
        inputWrapper.setAlignment(Pos.CENTER);
        inputWrapper.setStyle("-fx-background-color: #ffffff;");

        HBox pill = new HBox(5);
        pill.setAlignment(Pos.CENTER);
        pill.setPadding(new Insets(2, 5, 2, 15));
        pill.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d1d1; -fx-border-radius: 20; -fx-background-radius: 20;");
        HBox.setHgrow(pill, Priority.ALWAYS);

        messageField = new TextField();
        messageField.setPromptText("iMessage");
        messageField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 5;");
        HBox.setHgrow(messageField, Priority.ALWAYS);

        Button sendBtn = new Button();
        FontIcon sendIcon = new FontIcon(MaterialDesignA.ARROW_UP_BOLD_CIRCLE);
        sendIcon.setIconSize(28);
        sendBtn.setGraphic(sendIcon);
        sendBtn.getStyleClass().add("flat");
        sendBtn.setStyle("-fx-text-fill: #007AFF; -fx-padding: 0;");
        
        sendBtn.setOnAction(e -> handleSendMessage());
        messageField.setOnAction(e -> handleSendMessage());

        pill.getChildren().addAll(messageField, sendBtn);
        inputWrapper.getChildren().add(pill);

        // --- 4. Sidebar Peserta ---
        VBox userSidebar = new VBox(15);
        userSidebar.setPadding(new Insets(20, 15, 15, 15));
        userSidebar.setPrefWidth(220);
        userSidebar.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #d1d1d1; -fx-border-width: 0 0 0 0.5;");

        Label userSidebarTitle = new Label("PESERTA");
        userSidebarTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #8e8e93; -fx-font-size: 10px;");
        
        userListContainer = new VBox(12);
        refreshParticipants();
        
        userSidebar.getChildren().addAll(userSidebarTitle, userListContainer);

        setTop(header);
        setCenter(scrollPane);
        setBottom(inputWrapper);
        setRight(userSidebar);

        addMessage("Sistem", "Anda telah bergabung di " + room.getName(), false);
    }

    public void setOnKickUser(Consumer<String> onKickUser) {
        this.onKickUser = onKickUser;
    }

    public void refreshParticipants() {
        userListContainer.getChildren().clear();
        for (String participant : room.getParticipants()) {
            HBox userRow = new HBox(10);
            userRow.setAlignment(Pos.CENTER_LEFT);
            
            Label avatar = new Label(participant.substring(0, 1).toUpperCase());
            avatar.setAlignment(Pos.CENTER);
            avatar.setPrefSize(30, 30);
            avatar.setStyle("-fx-background-color: #e1e1e1; -fx-background-radius: 15; -fx-font-size: 12px; -fx-font-weight: bold;");
            
            Label pLabel = new Label(participant + (participant.equals(currentUser) ? " (Anda)" : ""));
            pLabel.setStyle("-fx-font-size: 13px;");
            
            Region pSpacer = new Region();
            HBox.setHgrow(pSpacer, Priority.ALWAYS);
            
            userRow.getChildren().addAll(avatar, pLabel, pSpacer);

            if (room.isOwner(currentUser) && !participant.equals(currentUser)) {
                Button kickBtn = new Button();
                kickBtn.setGraphic(new FontIcon(MaterialDesignA.ACCOUNT_REMOVE));
                kickBtn.getStyleClass().addAll("flat", "danger");
                kickBtn.setOnAction(e -> {
                    if (onKickUser != null) onKickUser.accept(participant);
                });
                userRow.getChildren().add(kickBtn);
            }
            userListContainer.getChildren().add(userRow);
        }
    }

    private Consumer<String> onSendMessage;

    public void setOnSendMessage(Consumer<String> onSendMessage) {
        this.onSendMessage = onSendMessage;
    }

    private void handleSendMessage() {
        String text = messageField.getText();
        if (!text.isEmpty()) {
            if (onSendMessage != null) {
                onSendMessage.accept(text);
            } else {
                addMessage(currentUser, text, true);
            }
            messageField.clear();
        }
    }

    public void addMessage(String sender, String text, boolean isSelf) {
        if (sender.equals("Sistem")) {
            Label sysMsg = new Label(text.toUpperCase());
            sysMsg.setStyle("-fx-text-fill: #8e8e93; -fx-font-size: 10px; -fx-font-weight: bold;");
            HBox center = new HBox(sysMsg);
            center.setAlignment(Pos.CENTER);
            center.setPadding(new Insets(10, 0, 10, 0));
            messageContainer.getChildren().add(center);
            return;
        }

        VBox bubbleWrapper = new VBox(2);
        Label contentLabel = new Label(text);
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(350);
        contentLabel.setPadding(new Insets(8, 14, 8, 14));

        HBox wrapper = new HBox(contentLabel);
        
        if (isSelf) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            contentLabel.setStyle(
                "-fx-background-color: #007AFF;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 18 18 2 18;" +
                "-fx-font-size: 14px;"
            );
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);
            // Tambahkan nama pengirim di atas bubble untuk grup chat
            Label nameLabel = new Label(sender);
            nameLabel.setStyle("-fx-text-fill: #8e8e93; -fx-font-size: 10px; -fx-padding: 0 0 0 5;");
            bubbleWrapper.getChildren().add(nameLabel);
            
            contentLabel.setStyle(
                "-fx-background-color: #E9E9EB;" +
                "-fx-text-fill: black;" +
                "-fx-background-radius: 18 18 18 2;" +
                "-fx-font-size: 14px;"
            );
        }

        bubbleWrapper.getChildren().add(wrapper);
        messageContainer.getChildren().add(bubbleWrapper);
    }
}