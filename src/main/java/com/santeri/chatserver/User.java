package com.santeri.chatserver;

public class User {

    String username;
    String password;
    String email;

    public User(final String user, final String passwd, final String emailaddr) {
        username = user;
        password = passwd;
        email = emailaddr;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }
}
