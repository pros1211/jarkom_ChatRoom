package com.example.views;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

import java.util.Optional;

public class CreateRoomDialog extends Dialog<String> {

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
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(350);

        Label label = new Label("Nama Ruangan:");
        label.setStyle("-fx-font-weight: bold;");

        TextField roomNameField = new TextField();
        roomNameField.setPromptText("Misal: Belajar Bareng");
        roomNameField.setOnAction(e -> okButton.fire());
        Platform.runLater(roomNameField::requestFocus);

        content.getChildren().addAll(label, roomNameField);
        getDialogPane().setContent(content);

        // Convert result
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return roomNameField.getText().trim();
            }
            return null;
        });
    }
}
