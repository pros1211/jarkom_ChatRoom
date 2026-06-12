package com.example.views;

import com.example.models.Room;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.function.Consumer;

public class ChatView extends BorderPane {
    private final VBox messageContainer;
    private final TextField messageField;
    private final String currentUser;
    private final Room room;

    private final VBox userListContainer;
    private Consumer<String> onKickUser;
    private Consumer<File> onSendFile;

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
        messageContainer.heightProperty().addListener((obs, oldHeight, newHeight) -> scrollPane.setVvalue(1.0));

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

        Button fileBtn = new Button("File");
        fileBtn.getStyleClass().add("flat");
        fileBtn.setStyle("-fx-text-fill: #007AFF; -fx-padding: 4 8 4 0;");
        fileBtn.setOnAction(e -> handleChooseFile());

        Button sendBtn = new Button();
        FontIcon sendIcon = new FontIcon(MaterialDesignA.ARROW_UP_BOLD_CIRCLE);
        sendIcon.setIconSize(28);
        sendBtn.setGraphic(sendIcon);
        sendBtn.getStyleClass().add("flat");
        sendBtn.setStyle("-fx-text-fill: #007AFF; -fx-padding: 0;");
        
        sendBtn.setOnAction(e -> handleSendMessage());
        messageField.setOnAction(e -> handleSendMessage());

        pill.getChildren().addAll(fileBtn, messageField, sendBtn);
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

    public void setOnSendFile(Consumer<File> onSendFile) {
        this.onSendFile = onSendFile;
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

    private void handleChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih file");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Semua File", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(getScene().getWindow());
        if (selectedFile != null && onSendFile != null) {
            onSendFile.accept(selectedFile);
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

    public void addFileMessage(String sender, String fileName, String fileMimeType, long fileSize, String fileData, boolean isSelf) {
        if (fileData == null) {
            addMessage("Sistem", "File dari " + sender + " kosong atau rusak.", false);
            return;
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(fileData);
        } catch (IllegalArgumentException e) {
            addMessage("Sistem", "File dari " + sender + " gagal dibaca.", false);
            return;
        }

        addFileMessage(sender, fileName, fileMimeType, fileSize, bytes, isSelf);
    }

    public void addFileMessage(String sender, String fileName, String fileMimeType, long fileSize, byte[] bytes, boolean isSelf) {
        String safeFileName = fileName == null ? "file" : fileName;
        String safeMimeType = fileMimeType == null ? "application/octet-stream" : fileMimeType;

        VBox bubbleWrapper = new VBox(2);
        VBox fileCard = new VBox(8);
        fileCard.setMaxWidth(360);
        fileCard.setPadding(new Insets(10, 12, 10, 12));

        Label fileTitle = new Label(safeFileName);
        fileTitle.setWrapText(true);
        fileTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        Label fileMeta = new Label(formatFileSize(fileSize) + " - " + safeMimeType);
        fileMeta.setStyle("-fx-font-size: 11px; -fx-text-fill: #6e6e73;");

        fileCard.getChildren().addAll(fileTitle, fileMeta);

        Node preview = createFilePreview(safeFileName, safeMimeType, bytes);
        if (preview != null) {
            fileCard.getChildren().add(preview);
        }

        Button downloadBtn = new Button("Download");
        downloadBtn.setMaxWidth(Double.MAX_VALUE);
        downloadBtn.setOnAction(e -> saveFile(safeFileName, bytes));
        fileCard.getChildren().add(downloadBtn);

        HBox wrapper = new HBox(fileCard);
        if (isSelf) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            fileCard.setStyle(
                "-fx-background-color: #007AFF;" +
                "-fx-background-radius: 18 18 2 18;"
            );
            fileTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");
            fileMeta.setStyle("-fx-font-size: 11px; -fx-text-fill: #d8ecff;");
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);
            Label nameLabel = new Label(sender);
            nameLabel.setStyle("-fx-text-fill: #8e8e93; -fx-font-size: 10px; -fx-padding: 0 0 0 5;");
            bubbleWrapper.getChildren().add(nameLabel);
            fileCard.setStyle(
                "-fx-background-color: #E9E9EB;" +
                "-fx-background-radius: 18 18 18 2;"
            );
        }

        bubbleWrapper.getChildren().add(wrapper);
        messageContainer.getChildren().add(bubbleWrapper);
    }

    private Node createFilePreview(String fileName, String fileMimeType, byte[] bytes) {
        if (fileMimeType == null) {
            return new Label("Preview tidak tersedia.");
        }

        if (fileMimeType.startsWith("image/")) {
            Image image = new Image(new ByteArrayInputStream(bytes), 320, 220, true, true);
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(320);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            return imageView;
        }

        if (fileMimeType.startsWith("video/") || fileMimeType.startsWith("audio/")) {
            return createMediaPreview(fileName, bytes, fileMimeType.startsWith("video/"));
        }

        Label placeholder = new Label("Preview tidak tersedia untuk tipe file ini.");
        placeholder.setWrapText(true);
        placeholder.setStyle("-fx-font-size: 12px; -fx-text-fill: #6e6e73;");
        return placeholder;
    }

    private Node createMediaPreview(String fileName, byte[] bytes, boolean video) {
        VBox mediaBox = new VBox(8);
        try {
            File tempFile = Files.createTempFile("chatapp-", getFileExtension(fileName)).toFile();
            Files.write(tempFile.toPath(), bytes);
            tempFile.deleteOnExit();

            Media media = new Media(tempFile.toURI().toString());
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            Button playBtn = new Button("Play");
            playBtn.setOnAction(e -> {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    playBtn.setText("Play");
                } else {
                    mediaPlayer.play();
                    playBtn.setText("Pause");
                }
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.stop();
                playBtn.setText("Play");
            });

            if (video) {
                MediaView mediaView = new MediaView(mediaPlayer);
                mediaView.setFitWidth(320);
                mediaView.setFitHeight(180);
                mediaView.setPreserveRatio(true);
                mediaBox.getChildren().add(mediaView);
            }

            mediaBox.getChildren().add(playBtn);
            return mediaBox;
        } catch (IOException | RuntimeException e) {
            Label errorLabel = new Label("Preview media tidak bisa dibuka.");
            errorLabel.setWrapText(true);
            errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6e6e73;");
            return errorLabel;
        }
    }

    private void saveFile(String fileName, byte[] bytes) {
        String safeFileName = sanitizeFileName(fileName);
        String extension = getFileExtension(safeFileName);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan file");
        fileChooser.setInitialFileName(safeFileName);
        if (!extension.equals(".tmp")) {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(extension.substring(1).toUpperCase() + " File", "*" + extension)
            );
        }
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Semua File", "*.*")
        );

        File destination = fileChooser.showSaveDialog(getScene().getWindow());
        if (destination == null) {
            return;
        }

        File targetFile = ensureOriginalExtension(destination, extension);
        try {
            Files.write(targetFile.toPath(), bytes);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Gagal menyimpan file: " + e.getMessage());
            alert.show();
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }

        double mb = kb / 1024.0;
        return String.format("%.1f MB", mb);
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex);
        }
        return ".tmp";
    }

    private File ensureOriginalExtension(File destination, String extension) {
        if (extension.equals(".tmp")) {
            return destination;
        }

        String fileName = destination.getName();
        if (fileName.toLowerCase().endsWith(extension.toLowerCase())) {
            return destination;
        }

        return new File(destination.getParentFile(), fileName + extension);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "downloaded-file";
        }

        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
