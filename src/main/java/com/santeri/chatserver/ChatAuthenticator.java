// Made by: Santeri Kottari
// Based on works made by: Antti Juustila https://github.com/anttijuu
package com.santeri.chatserver;

import java.util.Hashtable;
import java.util.Map;

import com.sun.net.httpserver.BasicAuthenticator;

public class ChatAuthenticator extends BasicAuthenticator {

    private Map<String, String> users = null;

    public ChatAuthenticator(String realm) {
        super(realm);
        users = new Hashtable<String, String>();
        users.put("user", "password");
    }

    @Override
    public boolean checkCredentials(String username, String password) {

        if (users.containsKey(username) && users.containsValue(password)) {
            return true;
        } else {
            return false;
        }

    }

    public boolean addUser(String username, String password) {

        /*
        Map<String, String> newUsers = null;
        newUsers = new Hashtable<String, String>();
        newUsers.put(username, password); */

        if (!users.containsKey(username)) {
            users.put(username, password);
            return true;
        }
        return false;
    }

}
