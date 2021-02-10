// Made by: Santeri Kottari
// Based on works made by: Antti Juustila https://github.com/anttijuu
package com.santeri.chatserver;

import java.util.Hashtable;
import java.util.Map;

import com.sun.net.httpserver.BasicAuthenticator;

public class ChatAuthenticator extends BasicAuthenticator {
    // users-map initialized 
    private Map<String, User> users = null;
    // constructor for ChatAuthenticator, receives realm '/chat' or '/registration'
    public ChatAuthenticator(String realm) {
        super(realm);
        // new HashTable initialized into 'users'
        users = new Hashtable<String, User>();
        User admin = new User("Santeri", "salasana", "sjkottari@email.com");
        users.put("Santeri", admin);
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        int code = 200;
        // checking if users hashtable contains username and password inserted in the request
        if (users.containsKey(username)) {
            // check if key-value pair matches
            if (users.get(username).getPassword().equals(password)) {
                return true;
            }
        }
        // non-authenticated credentials result into error
        code = 401;
        ChatServer.log("Authentication error: " + code + " Invalid credentials");
        return false;
    }

    // method for adding user, returns true if successful
    public boolean addUser(String username, User newUser) {
        // check if users hashtable does not contain the username beforehand
        if (!users.containsKey(username)) {
            // new key-value pair added to users
            users.put(username, newUser);
            return true;
        }
        return false;
    }
}
