/* 
    Programming 3 Course Assignment - ChatServer.User
    Author: Santeri Kottari https://github.com/sjkottari
    Project repository: https://github.com/sjkottari/oh3-chatserver-assignment
    Information Processing Science - University of Oulu
*/

package com.santeri.chatserver;

// Class for users
public class User {

    private String username;
    private String password;
    private String email;

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
