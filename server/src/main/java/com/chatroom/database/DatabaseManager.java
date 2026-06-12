package com.chatroom.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.chatroom.model.Message;
import com.chatroom.model.Room;

public class DatabaseManager {

    private static final String SERVER_URL = "jdbc:mysql://localhost:3306/";

    private static final String DATABASE_NAME = "chatroom_db";

    private static final String URL = SERVER_URL + DATABASE_NAME;

    private static final String USER = "root";

    private static final String PASSWORD = "";

    private static boolean initialized = false;

    public static Connection getConnection()
            throws SQLException {

        initializeDatabase();
        return DriverManager.getConnection(
                URL,
                USER,
                PASSWORD);
    }

    private static synchronized void initializeDatabase()
            throws SQLException {

        if (initialized) {
            return;
        }

        try (
                Connection serverConn = DriverManager.getConnection(
                        SERVER_URL,
                        USER,
                        PASSWORD);
                Statement serverStatement = serverConn.createStatement()) {

            serverStatement.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS " + DATABASE_NAME);
        }

        try (
                Connection conn = DriverManager.getConnection(
                        URL,
                        USER,
                        PASSWORD);
                Statement statement = conn.createStatement()) {

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users ("
                            + "id INT AUTO_INCREMENT PRIMARY KEY,"
                            + "username VARCHAR(50) UNIQUE NOT NULL,"
                            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                            + ")");

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS rooms ("
                            + "id INT AUTO_INCREMENT PRIMARY KEY,"
                            + "room_id VARCHAR(100) UNIQUE NOT NULL,"
                            + "room_name VARCHAR(100) NOT NULL,"
                            + "owner_username VARCHAR(50),"
                            + "max_members INT DEFAULT 10,"
                            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                            + ")");
            ensureRoomsMaxMembersColumn(conn);

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS messages ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "room_id VARCHAR(100),"
                            + "sender VARCHAR(50),"
                            + "content TEXT,"
                            + "sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                            + ")");

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS files ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "room_id VARCHAR(100),"
                            + "sender VARCHAR(50),"
                            + "file_name VARCHAR(255),"
                            + "file_path TEXT,"
                            + "mime_type VARCHAR(100),"
                            + "file_size BIGINT,"
                            + "sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                            + ")");
        }

        initialized = true;
    }

    public void saveUser(String username)
            throws SQLException {

        String sql = "INSERT INTO users(username) VALUES(?) "
                + "ON DUPLICATE KEY UPDATE username = username";

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            ps.executeUpdate();
        }
    }

    public void saveRoom(String roomId, String roomName, String ownerUsername)
            throws SQLException {
        saveRoom(roomId, roomName, ownerUsername, 10);
    }

    public void saveRoom(String roomId, String roomName, String ownerUsername, int maxMembers)
            throws SQLException {

        String sql = "INSERT INTO rooms(room_id, room_name, owner_username, max_members) VALUES(?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE room_name = ?, owner_username = ?, max_members = ?";

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roomId);
            ps.setString(2, roomName);
            ps.setString(3, ownerUsername);
            ps.setInt(4, Math.max(2, maxMembers));
            ps.setString(5, roomName);
            ps.setString(6, ownerUsername);
            ps.setInt(7, Math.max(2, maxMembers));

            ps.executeUpdate();
        }
    }

    public void saveMessage(String roomId, String sender, String content)
            throws SQLException {

        String sql = "INSERT INTO messages(room_id, sender, content) VALUES(?, ?, ?)";

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roomId);
            ps.setString(2, sender);
            ps.setString(3, content);

            ps.executeUpdate();
        }
    }

    public void saveFileMetadata(
            String roomId,
            String sender,
            String fileName,
            String filePath,
            String mimeType,
            long fileSize)
            throws SQLException {

        String sql = "INSERT INTO files(room_id, sender, file_name, file_path, mime_type, file_size) "
                + "VALUES(?, ?, ?, ?, ?, ?)";

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roomId);
            ps.setString(2, sender);
            ps.setString(3, fileName);
            ps.setString(4, filePath);
            ps.setString(5, mimeType);
            ps.setLong(6, fileSize);

            ps.executeUpdate();
        }
    }

    public List<Message> getMessagesByRoom(String roomId)
            throws SQLException {

        String sql = "SELECT sender, room_id, content, sent_at "
                + "FROM messages WHERE room_id = ? ORDER BY sent_at ASC, id ASC";
        List<Message> messages = new ArrayList<>();

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roomId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(
                            new Message(
                                    rs.getString("sender"),
                                    rs.getString("room_id"),
                                    rs.getString("content"),
                                    rs.getTimestamp("sent_at").getTime()));
                }
            }
        }

        return messages;
    }

    public List<Room> getRooms()
            throws SQLException {

        String sql = "SELECT room_id, room_name, owner_username, max_members "
                + "FROM rooms ORDER BY created_at ASC, id ASC";
        List<Room> rooms = new ArrayList<>();

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rooms.add(
                                new Room(
                                        rs.getString("room_id"),
                                        rs.getString("room_name"),
                                        rs.getString("owner_username"),
                                        rs.getInt("max_members")));
            }
        }

        return rooms;
    }

    private static void ensureRoomsMaxMembersColumn(Connection conn)
            throws SQLException {

        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'rooms' AND COLUMN_NAME = 'max_members'";

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, DATABASE_NAME);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (Statement statement = conn.createStatement()) {
                        statement.executeUpdate("ALTER TABLE rooms ADD COLUMN max_members INT DEFAULT 10");
                    }
                }
            }
        }
    }

    public List<StoredFile> getFilesByRoom(String roomId)
            throws SQLException {

        String sql = "SELECT id, room_id, sender, file_name, file_path, mime_type, file_size "
                + "FROM files WHERE room_id = ? ORDER BY sent_at ASC, id ASC";
        List<StoredFile> files = new ArrayList<>();

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roomId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    files.add(
                            new StoredFile(
                                    rs.getLong("id"),
                                    rs.getString("room_id"),
                                    rs.getString("sender"),
                                    rs.getString("file_name"),
                                    rs.getString("file_path"),
                                    rs.getString("mime_type"),
                                    rs.getLong("file_size")));
                }
            }
        }

        return files;
    }

    public static class StoredFile {

        private final long id;

        private final String roomId;

        private final String sender;

        private final String fileName;

        private final String filePath;

        private final String mimeType;

        private final long fileSize;

        public StoredFile(
                long id,
                String roomId,
                String sender,
                String fileName,
                String filePath,
                String mimeType,
                long fileSize) {

            this.id = id;
            this.roomId = roomId;
            this.sender = sender;
            this.fileName = fileName;
            this.filePath = filePath;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
        }

        public long getId() {
            return id;
        }

        public String getRoomId() {
            return roomId;
        }

        public String getSender() {
            return sender;
        }

        public String getFileName() {
            return fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getMimeType() {
            return mimeType;
        }

        public long getFileSize() {
            return fileSize;
        }
    }
}
