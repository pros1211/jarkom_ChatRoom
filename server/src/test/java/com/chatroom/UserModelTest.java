package com.chatroom;

import com.chatroom.model.User;

public class UserModelTest {

    public static void main(String[] args) {

        User user = new User("Prospero");

        System.out.println(user.getUsername());
    }
}