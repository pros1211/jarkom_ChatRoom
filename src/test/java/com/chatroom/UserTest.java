package com.chatroom;

import com.chatroom.database.DatabaseManager;

public class UserTest {

    public static void main(String[] args)
            throws Exception {

        DatabaseManager db = new DatabaseManager();

        db.saveUser("Prospero");

        System.out.println("User saved!");
    }
}