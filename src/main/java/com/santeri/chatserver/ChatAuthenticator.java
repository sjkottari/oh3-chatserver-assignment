package com.santeri.chatserver;

import java.util.Hashtable;
import java.util.Map;

import com.sun.net.httpserver.BasicAuthenticator;

public class ChatAuthenticator extends BasicAuthenticator {

    private Map<String, String> users = null;
    
    public ChatAuthenticator(String chat) {
        super(chat);
        users = new Hashtable<String,String>();
        users.put("user", "password");
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        
        if(users.containsKey(username) && users.containsValue(password)) {
            return true;
        }
        else {
            return false;    
        }

    }
    
}
