package com.example.network;

import com.example.protocol.MessageType;
import com.example.protocol.Packet;
import com.google.gson.Gson;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ChatClient {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private final Gson gson = new Gson();
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "chat-client-sender");
        thread.setDaemon(true);
        return thread;
    });
    private Consumer<Packet> onPacketReceived;
    private boolean running = false;

    public void connect(String host, int port, Runnable onConnected, Consumer<String> onError) {
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                socket.setTcpNoDelay(true);
                writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
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
                    try {
                        Packet packet = gson.fromJson(json, Packet.class);
                        if (onPacketReceived != null && packet != null) {
                            if (packet.getType() == MessageType.FILE_CHUNK) {
                                onPacketReceived.accept(packet);
                            } else {
                                Platform.runLater(() -> onPacketReceived.accept(packet));
                            }
                        }
                    } catch (Exception packetError) {
                        System.err.println("Gagal membaca paket dari server: " + packetError.getMessage());
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
        sendExecutor.execute(() -> sendPacketNow(packet));
    }

    public boolean sendPacketNow(Packet packet) {
        if (writer == null) {
            return false;
        }

        synchronized (writer) {
            writer.println(gson.toJson(packet));
            if (writer.checkError()) {
                System.err.println("Gagal mengirim paket ke server.");
                return false;
            }
        }

        return true;
    }

    public void setOnPacketReceived(Consumer<Packet> onPacketReceived) {
        this.onPacketReceived = onPacketReceived;
    }

    public void disconnect() {
        running = false;
        sendExecutor.shutdownNow();
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}
