package com.example.protocol;

public enum MessageType {
    LOGIN,
    LOGIN_SUCCESS,
    LOGIN_FAILED,

    CREATE_ROOM,
    ROOM_CREATED,

    GET_ROOMS,
    ROOM_LIST,

    JOIN_ROOM,
    LEAVE_ROOM,

    USER_JOINED,
    USER_LEFT,

    CHAT_MESSAGE,
    MESSAGE_HISTORY,
    FILE_MESSAGE,
    FILE_CHUNK,

    KICK_USER,
    USER_KICKED,

    DELETE_ROOM,
    ROOM_DELETED,

    ERROR,
    SYSTEM_NOTIFICATION
}
