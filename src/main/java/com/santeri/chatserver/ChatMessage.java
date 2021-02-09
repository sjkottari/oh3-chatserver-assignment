package com.santeri.chatserver;

import java.time.LocalDateTime;

public class ChatMessage {
    public LocalDateTime timeSent;
    public String nickname;
    public String message;

    public ChatMessage(LocalDateTime sent, String nick, String msg) {
        timeSent = sent;
        nickname = nick;
        message = msg;
    }

    public String getNickname() {
        return nickname;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timeSent;
    }
}
