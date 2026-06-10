package com.chatroom.server;

import java.net.Socket;

public class ClientConnection {

    private String username;
    private Socket socket;

    public ClientConnection(
            String username,
            Socket socket) {

        this.username = username;
        this.socket = socket;
    }

    public String getUsername() {
        return username;
    }

    public Socket getSocket() {
        return socket;
    }
}