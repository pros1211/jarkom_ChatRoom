package com.example.views;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

import java.util.function.BiConsumer;

public class LoginView extends VBox {
    private final TextField usernameField;
    private final TextField ipField;
    private final Button loginBtn;

    public LoginView(BiConsumer<String, String> onLogin) {
        setSpacing(20);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(50));

        Label titleLabel = new Label("ChatApp");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
        
        Label subTitle = new Label("Silahkan masuk untuk mulai chatting");

        subTitle.getStyleClass().add("text-muted");
        ipField = new TextField();
        ipField.setPromptText("Masukkan IP Server...");
        ipField.setMaxWidth(300);

        usernameField = new TextField();
        usernameField.setPromptText("Masukkan Username...");
        usernameField.setMaxWidth(300);

        loginBtn = new Button("Hubungkan");
        loginBtn.setGraphic(new FontIcon(MaterialDesignL.LOGIN));
        loginBtn.getStyleClass().add("accent");
        loginBtn.setPrefWidth(300);

        loginBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String ip = ipField.getText().trim();
            if (ip.isEmpty()) {
                ip = "localhost"; 
            }
            if (!username.isEmpty()) {
                onLogin.accept(username, ip);
            }
        });

        getChildren().addAll(titleLabel, subTitle, usernameField, ipField, loginBtn);
    }
}