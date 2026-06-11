package com.example.network;

import com.example.protocol.MessageType;
import com.example.protocol.Packet;
import com.google.gson.Gson;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class ChatClient {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private final Gson gson = new Gson();
    private Consumer<Packet> onPacketReceived;
    private boolean running = false;

    public void connect(String host, int port, Runnable onConnected, Consumer<String> onError) {
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                writer = new PrintWriter(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                running = true;
                
                Platform.runLater(onConnected);
                startListening();
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept("Gagal terhubung ke server: " + e.getMessage()));
            }
        }).start();
    }

    private void startListening() {
        new Thread(() -> {
            try {
                String json;
                while (running && (json = reader.readLine()) != null) {
                    Packet packet = gson.fromJson(json, Packet.class);
                    if (onPacketReceived != null) {
                        Platform.runLater(() -> onPacketReceived.accept(packet));
                    }
                }
            } catch (Exception e) {
                if (running) {
                    Platform.runLater(() -> System.err.println("Koneksi terputus: " + e.getMessage()));
                }
            }
        }).start();
    }

    public void sendPacket(Packet packet) {
        if (writer != null) {
            new Thread(() -> writer.println(gson.toJson(packet))).start();
        }
    }

    public void setOnPacketReceived(Consumer<Packet> onPacketReceived) {
        this.onPacketReceived = onPacketReceived;
    }

    public void disconnect() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}