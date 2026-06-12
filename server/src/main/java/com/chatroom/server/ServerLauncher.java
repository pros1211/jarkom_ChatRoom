package com.chatroom.server;

public class ServerLauncher {

    private static final int DEFAULT_PORT = 5000;

    public static void main(
            String[] args) {

        int port = resolvePort(args);
        ChatServer server = new ChatServer(port);

        server.start();
    }

    private static int resolvePort(String[] args) {
        String portValue = args.length > 0 ? args[0] : System.getenv("CHATROOM_PORT");
        if (portValue == null || portValue.trim().isEmpty()) {
            return DEFAULT_PORT;
        }

        try {
            int port = Integer.parseInt(portValue.trim());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("port out of range");
            }
            return port;
        } catch (NumberFormatException e) {
            System.err.println("Port tidak valid: " + portValue + ". Menggunakan port " + DEFAULT_PORT + ".");
            return DEFAULT_PORT;
        }
    }
}
