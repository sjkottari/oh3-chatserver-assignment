// Made by: Santeri Kottari
// Based on works made by: Antti Juustila https://github.com/anttijuu
package com.santeri.chatserver;

import java.util.Hashtable;
import java.util.Map;

import com.sun.net.httpserver.BasicAuthenticator;

public class ChatAuthenticator extends BasicAuthenticator {
    // users-map initialized 
    private Map<String, String> users = null;
    // constructor for ChatAuthenticator, receives realm '/chat' or '/registration'
    public ChatAuthenticator(String realm) {
        super(realm);
        // new HashTable initialized into 'users'
        users = new Hashtable<String, String>();
        users.put("admin", "password"); 
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        int code = 200;
        // checking if users hashtable contains username and password inserted in the request
        if (users.containsKey(username) && users.containsValue(password)) {
            // check if key-value pair matches
            if (users.get(username).equals(password)) {
                return true;
            }
        }
        // non-authenticated credentials result into error
        code = 401;
        ChatServer.log("Authentication error: " + code + " Invalid credentials");
        return false;
    }

    // method for adding user, returns true if successful
    public boolean addUser(String username, String password) {
        // check if users hashtable does not contain the username beforehand
        if (!users.containsKey(username)) {
            // new key-value pair added to users
            users.put(username, password);
            return true;
        }
        return false;
    }

}
