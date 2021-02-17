package com.santeri.chatserver;

import java.io.File;
import java.lang.Thread.State;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ChatDatabase {

    private static ChatDatabase singleton = null;
    private Connection connectionObj = null;

    public static synchronized ChatDatabase getInstance() {
        if (null == singleton) {
            singleton = new ChatDatabase();
        }
        return singleton;
    }

    private ChatDatabase() {
    }

    public ChatDatabase(Connection connection) {
        connectionObj = connection;
    }

    public Connection getConnection() {
        return connectionObj;
    }

    public void open(String dbName) throws SQLException {
        boolean existence;

        File file = new File(dbName);
        if (!file.isFile() && !file.isDirectory()) {
            existence = false;
        } else {
            existence = true;
        }

        String dbPath = new String("jdbc:sqlite:" + file.getAbsolutePath());
        connectionObj = DriverManager.getConnection(dbPath);

        if (existence == false) {
            ChatServer.log("Initializing database...");
            initializeDatabase();
            ChatServer.log("Database initialized");
        } else {
            ChatServer.log("Database already exists. Opening...");
        }
    }

    private boolean initializeDatabase() throws SQLException {

        String createRegTable = "CREATE TABLE IF NOT EXISTS registration \n"
                              + "(username varchar(50) PRIMARY KEY NOT NULL, \n"
                              + "password varchar(100) NOT NULL, \n"
                              + "email varchar(100) NOT NULL)";
        String createChatTable = "CREATE TABLE IF NOT EXISTS chatmessage \n"
                               + "(nickname varchar(50) NOT NULL, \n"
                               + "message varchar(500) NOT NULL, \n"
                               + "timestamp integer NOT NULL, \n"
                               + "PRIMARY KEY(nickname, timestamp))";
        if (null != connectionObj) {
            Statement createStatement = connectionObj.createStatement(); // try-lauseen sisään?
            createStatement.executeUpdate(createRegTable);
            createStatement.executeUpdate(createChatTable);
            createStatement.close();
            return true;
        }
        return false;
    }

    public boolean registerUser(String username, User newUser) throws SQLException {

        if (!validateUser(username)) {
            String createRegistration = "INSERT INTO registration VALUES('" + newUser.username 
                                      + "', '" + newUser.password 
                                      + "', '" + newUser.email + "')";
            Statement createStatement = connectionObj.createStatement();
            createStatement.executeUpdate(createRegistration);
            createStatement.close();
            return true;
        } else {
            ChatServer.log("Something went wrong");
            return false;
        }
    }

    private boolean validateUser(String user) throws SQLException {
        
        String getUser = "SELECT username FROM registration WHERE username = '" + user + "'";
        ChatServer.log("This is the user to be checked: " + user );

        Statement statement = connectionObj.createStatement();
        ResultSet rs = statement.executeQuery(getUser);
        
        while (rs.next()) {
            String dbUser = rs.getString("username");
            ChatServer.log("This is the DBuser: " + dbUser);
            if (dbUser.equals(user)) {
                ChatServer.log("User already registered to database");
                return true;
            }
        }
        statement.close();
        ChatServer.log("No match found, returning false");
        return false;
    }

    public boolean authenticateUser(String username, String password) throws SQLException {
        //int code = 200;
        String getCredentials = "SELECT username, password FROM registration WHERE username = '" + username + "'";

        Statement statement = connectionObj.createStatement();
        ResultSet rs = statement.executeQuery(getCredentials);

        while (rs.next()){
            String dbUser = rs.getString("username");
            String dbPasswd = rs.getString("password");

            if(dbUser.equals(username) && dbPasswd.equals(password)) {
                return true;
            }
        }
        statement.close();
        //code = 401;
        //ChatServer.log("Authentication error: " + code + " Invalid credentials");
        ChatServer.log("Invalid login credentials");
        return false;
    }

    public void storeMessages() throws SQLException {
        
    }

}
