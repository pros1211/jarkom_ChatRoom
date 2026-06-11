package com.chatroom;

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.chatroom.database.DatabaseManager;

public class DatabaseTest {

    public static void main(String[] args) {

        try (
                Connection conn = DatabaseManager.getConnection()) {

            String sql = "INSERT INTO users(username) VALUES(?)";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, "Prospero");

            ps.executeUpdate();

            System.out.println("Insert Success!");

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}