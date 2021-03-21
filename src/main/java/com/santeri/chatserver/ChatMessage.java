/* 
    Programming 3 Course Assignment - ChatServer.ChatMessage
    Author: Santeri Kottari https://github.com/sjkottari
    Project repository: https://github.com/sjkottari/oh3-chatserver
    Based on works made by Antti Juustila https://github.com/anttijuu
    Information Processing Science - University of Oulu
*/

package com.santeri.chatserver;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

// Class for chat messages
public class ChatMessage {
    private LocalDateTime timeSent;
    private String nickname;
    private String message;
    String location;
    String temperature;

    public ChatMessage(final LocalDateTime sent, final String nick, final String msg, final String loc,
            final String tmp) {
        timeSent = sent;
        nickname = nick;
        message = msg;
        location = loc;
        temperature = tmp;
    }

    // Method that returns the time the message was sent in Epoch time format
    long dateAsInt() {
        return timeSent.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    // Method that sets the sent message time from epoch time format to
    // local date time with time zones taken into account.
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

    public String getLocation() {
        return location;
    }

    public String getTemperature() {
        return temperature;
    }
}
