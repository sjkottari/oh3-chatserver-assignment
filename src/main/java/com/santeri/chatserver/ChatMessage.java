package com.santeri.chatserver;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ChatMessage {
    public LocalDateTime timeSent;
    public String nickname;
    public String message;

    public ChatMessage(final LocalDateTime sent, final String nick, final String msg) {
        timeSent = sent;
        nickname = nick;
        message = msg;
    }

    long dateAsInt() {
        return timeSent.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    void setSent(final long epoch) {
        timeSent = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
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
