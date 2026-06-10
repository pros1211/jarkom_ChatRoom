package com.chatroom.model;

public class Message {

    private String sender;

    private String roomId;

    private String content;

    private long timestamp;

    public Message() {
    }

    public Message(
            String sender,
            String roomId,
            String content,
            long timestamp) {

        this.sender = sender;
        this.roomId = roomId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }
}