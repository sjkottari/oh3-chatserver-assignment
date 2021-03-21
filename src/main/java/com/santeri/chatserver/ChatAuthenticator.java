/* 
    Programming 3 Course Assignment - ChatServer.ChatAuthenticator
    Author: Santeri Kottari https://github.com/sjkottari
    Project repository: https://github.com/sjkottari/oh3-chatserver
    Based on works made by Antti Juustila https://github.com/anttijuu
    Information Processing Science - University of Oulu
*/

package com.santeri.chatserver;

import java.sql.SQLException;

import com.sun.net.httpserver.BasicAuthenticator;

public class ChatAuthenticator extends BasicAuthenticator {

    ChatDatabase database = ChatDatabase.getInstance();

    // Constructor for ChatAuthenticator, receives realm 
    // '/chat' or '/registration' as parameter
    public ChatAuthenticator(final String realm) {
        super(realm);
    }

    // Method for checking existing users from the database. Separate
    // method authenticateUser() returns true if username and password
    // have a match in DB. In turn, 'true' is returned to caller.
    @Override
    public boolean checkCredentials(String username, String password) {
        int code = 200;

        try {
            if (database.authenticateUser(username, password)) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Credentials that failed authentication result in error; 'false' 
        // is returned to caller.
        code = 401;
        ChatServer.log("Authentication error: " + code + " Invalid credentials");
        return false;
    }

    // Method for managing user addition to database. Returns 'true' 
    // to caller if successful. User registration is carried out in
    // separate method registerUser().
    public boolean addUser(String username, User newUser) {
        int code = 200;

        try {
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
