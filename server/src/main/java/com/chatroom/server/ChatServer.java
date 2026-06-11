package com.chatroom.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private final int port;

    private ServerSocket serverSocket;

    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private final RoomManager roomManager = new RoomManager();

    public ChatServer(int port) {
        this.port = port;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public void start() {

        System.out.println("START METHOD CALLED");

        try {

            serverSocket = new ServerSocket(port);

            System.out.println(
                    "Server listening on port "
                            + port);

            while (true) {

                Socket clientSocket = serverSocket.accept();

                System.out.println(
                        "New connection: "
                                + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(
                        clientSocket,
                        this);

                threadPool.execute(handler);
            }

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}