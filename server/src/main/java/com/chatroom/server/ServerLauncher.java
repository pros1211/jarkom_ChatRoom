package com.chatroom.server;

public class ServerLauncher {

    public static void main(
            String[] args) {

        ChatServer server = new ChatServer(5000);

        server.start();
    }
}