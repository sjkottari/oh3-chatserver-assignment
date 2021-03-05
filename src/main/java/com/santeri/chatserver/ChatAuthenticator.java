// Made by: Santeri Kottari
// Based on works made by: Antti Juustila https://github.com/anttijuu
package com.santeri.chatserver;

import java.sql.SQLException;

import com.sun.net.httpserver.BasicAuthenticator;

public class ChatAuthenticator extends BasicAuthenticator {

    ChatDatabase database = ChatDatabase.getInstance();
    // constructor for ChatAuthenticator, receives realm '/chat' or '/registration'
    public ChatAuthenticator(final String realm) {
        super(realm);
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        int code = 200;
        try { 
            // authenticateUser returns true if username and password have a match in DB
            if (database.authenticateUser(username, password)) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // non-authenticated credentials result into error
        code = 401;
        ChatServer.log("Authentication error: " + code + " Invalid credentials");
        return false;
    }

    // method for adding user, returns true if successful
    public boolean addUser(String username, User newUser) {
        int code = 200;
        try {
            // registerUser returns true if adding a user is successful
            if (database.registerUser(username, newUser)) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        code = 401;
        ChatServer.log("Error while adding as user: " + code);
        return false;
    }
}
