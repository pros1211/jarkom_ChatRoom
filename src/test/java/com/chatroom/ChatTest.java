package com.chatroom;

import java.io.PrintWriter;
import java.net.Socket;

import com.chatroom.protocol.MessageType;
import com.chatroom.protocol.Packet;
import com.google.gson.Gson;

public class ChatTest {

        public static void main(String[] args)
                        throws Exception {

                Socket socket = new Socket(
                                "localhost",
                                5000);

                Gson gson = new Gson();

                PrintWriter writer = new PrintWriter(
                                socket.getOutputStream(),
                                true);

                /*
                 * LOGIN
                 */
                Packet login = new Packet(
                                MessageType.LOGIN);

                login.setUsername(
                                "Prospero");

                writer.println(
                                gson.toJson(login));

                Thread.sleep(1000);

                /*
                 * JOIN ROOM
                 */
                Packet join = new Packet(
                                MessageType.JOIN_ROOM);

                join.setRoomId(
                                "ROOM001");

                writer.println(
                                gson.toJson(join));

                System.out.println(
                                "Joined ROOM001");

                Thread.sleep(1000);

                /*
                 * SEND MESSAGE
                 */
                Packet message = new Packet(
                                MessageType.CHAT_MESSAGE);

                message.setMessage(
                                "Halo Semua");

                writer.println(
                                gson.toJson(message));

                System.out.println(
                                "Message sent");

                /*
                 * biarkan koneksi hidup
                 */
                while (true) {

                        Thread.sleep(1000);
                }
        }
}