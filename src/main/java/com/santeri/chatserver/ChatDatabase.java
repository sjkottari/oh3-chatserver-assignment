package com.santeri.chatserver;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class ChatDatabase {

    private static ChatDatabase singleton = null;
    private Connection connectionObj = null;

    // implement database as singleton and synchronize the method
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

    // method for opening the database
    public void open(String dbName) throws SQLException {
        boolean dbExistence;
        File file = new File(dbName);
        if (!file.isFile() && !file.isDirectory()) {
            dbExistence = false;
        } else {
            dbExistence = true;
        }

        try {
            // file-object used to obtain the absolute path to database
            String dbPath = new String("jdbc:sqlite:" + file.getAbsolutePath());
            // fire up connection to the database with given path
            connectionObj = DriverManager.getConnection(dbPath);

            if (dbExistence == false) {
                ChatServer.log("Initializing database...");
                initializeDatabase();
                ChatServer.log("Database initialized");
            } else {
                ChatServer.log("Database already exists. Opening...");
            }

        } catch (SQLException e) {
            ChatServer.log("Error while connecting to database " + e.getMessage());
        }
    }

    // method for creating the database (with few set tables) if it does not exist from before
    private boolean initializeDatabase() throws SQLException {

        String createRegTable = "CREATE TABLE IF NOT EXISTS registration \n"
                              + "(username varchar(50) PRIMARY KEY NOT NULL, \n"
                              + "password varchar(100) NOT NULL, \n"
                              + "email varchar(100) NOT NULL)";
        String createChatTable = "CREATE TABLE IF NOT EXISTS chatmessage \n"
                               + "(nickname varchar(50) NOT NULL, \n"
                               + "message varchar(500) NOT NULL, \n"
                               + "timestamp numeric NOT NULL, \n"
                               + "PRIMARY KEY(nickname, timestamp))";
        try {
            if (null != connectionObj) {
                Statement createStatement = connectionObj.createStatement();
                createStatement.executeUpdate(createRegTable);
                createStatement.executeUpdate(createChatTable);
                createStatement.close();
                return true;
            }
        } catch (SQLException e) {
            ChatServer.log("Error while initializing database " + e.getMessage());
        }
        return false;
    }

    // method for registering a new user into the database
    public boolean registerUser(String username, User newUser) throws SQLException {

        if (!validateUser(username)) {
            String createRegistration = "INSERT INTO registration VALUES('" + newUser.getUsername() 
                                      + "', '" + newUser.getPassword() 
                                      + "', '" + newUser.getEmail() + "')";
            Statement stmnt = connectionObj.createStatement();
            stmnt.executeUpdate(createRegistration);
            stmnt.close();
            return true;
        } else {
            ChatServer.log("Username validation failed");
            return false;
        }
    }

    // method for checking if a user already exists in the database
    private boolean validateUser(String user) throws SQLException {
        
        String getUser = "SELECT username FROM registration WHERE username = '" + user + "'";
        ChatServer.log("User to be checked: " + user );

        try (Statement stmnt = connectionObj.createStatement()) {
            ResultSet rs = stmnt.executeQuery(getUser);
        
            while (rs.next()) {
                String dbUser = rs.getString("username");
                if (dbUser.equals(user)) {
                    ChatServer.log("User already registered to database");
                    return true;
                }
            }
        }
        ChatServer.log("No match found, returning 'false'");
        return false;
    }

    // method for authenticating existing users through /login-function
    public boolean authenticateUser(String username, String password) throws SQLException {

        String getCredentials = "SELECT username, password FROM registration WHERE username = '" + username + "'";

        try (Statement stmnt = connectionObj.createStatement()) {
            ResultSet rs = stmnt.executeQuery(getCredentials);

            while (rs.next()){
                String dbUser = rs.getString("username");
                String dbPasswd = rs.getString("password");

                if(dbUser.equals(username) && dbPasswd.equals(password)) {
                    return true;
                }
            }
        }
        ChatServer.log("Invalid login credentials");
        return false;
    }

    // method for storing messages to database
    public void storeMessages(ChatMessage m) throws SQLException {

        String storeUpdate = "INSERT INTO chatmessage VALUES ('" + m.getNickname() + "','" + m.getMessage() + "','" + m.dateAsInt() + "')";

        try (Statement stmnt = connectionObj.createStatement()) {
            stmnt.executeUpdate(storeUpdate);
        } catch (SQLException e) {
            ChatServer.log("Error while storing chat messages to database " + e.getMessage());
        }
    }

    // method for getting all messages from the database
    public ArrayList<ChatMessage> getMessages() throws SQLException {

        ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();
        String getMessage = "SELECT * FROM chatmessage";

        try (Statement stmnt = connectionObj.createStatement()) {
            ResultSet rs = stmnt.executeQuery(getMessage);
            while (rs.next()) {
                String dbNick = rs.getString("nickname");
                String dbMsg = rs.getString("message");
                LocalDateTime dbTime = null;

                ChatMessage msg = new ChatMessage(dbTime, dbNick, dbMsg);
                
                msg.setSent(rs.getLong("timestamp"));
                messages.add(msg);
            }
        } catch (SQLException e) {
            ChatServer.log("Error while getting chat messages from database " + e.getMessage());
        }
        return messages;
    }
}