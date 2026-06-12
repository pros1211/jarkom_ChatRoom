package com.example.views;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

public class CreateRoomDialog extends Dialog<CreateRoomDialog.RoomResult> {

    public static class RoomResult {
        public final String name;
        public final int limit;

        public RoomResult(String name, int limit) {
            this.name = name;
            this.limit = limit;
        }
    }

    public CreateRoomDialog() {
        setTitle("Buat Room Baru");
        setHeaderText(null);
        setGraphic(new FontIcon(MaterialDesignP.PLUS_BOX));

        // Set Button Types (Create & Cancel)
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Style "OK" button
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("accent");
        okButton.setText("Buat Room");
        okButton.setDefaultButton(true);

        // Content
        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setPrefWidth(350);

        Label nameLabel = new Label("Nama Ruangan:");
        nameLabel.setStyle("-fx-font-weight: bold;");

        TextField roomNameField = new TextField();
        roomNameField.setPromptText("Misal: Belajar Bareng");
        roomNameField.setOnAction(e -> okButton.fire());
        Platform.runLater(roomNameField::requestFocus);

        Label limitLabel = new Label("Batas Peserta:");
        limitLabel.setStyle("-fx-font-weight: bold;");

        Spinner<Integer> limitSpinner = new Spinner<>(2, 100, 10);
        limitSpinner.setEditable(true);
        limitSpinner.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(nameLabel, roomNameField, limitLabel, limitSpinner);
        getDialogPane().setContent(content);

        // Convert result
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String name = roomNameField.getText().trim();
                if (!name.isEmpty()) {
                    return new RoomResult(name, limitSpinner.getValue());
                }
            }
            return null;
        });
    }
}
