package com.santeri.chatserver;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.Crypt;

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

    public ChatDatabase(final Connection connection) {
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
                              + "(username varchar(50) NOT NULL, \n"
                              + "password varchar(100) NOT NULL, \n"
                              + "email varchar(100) NOT NULL, \n"
                              + "PRIMARY KEY(username, password))";
        String createChatTable = "CREATE TABLE IF NOT EXISTS chatmessage \n"
                               + "(nickname varchar(50) NOT NULL, \n"
                               + "message varchar(500) NOT NULL, \n"
                               + "timestamp numeric NOT NULL, \n"
                               + "location text, \n"
                               + "temperature text, \n"
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

        if (validateUser(username)) {
            // hashing the password using crypt -library method
            String hashedPassword = Crypt.crypt(newUser.getPassword());

            String createRegistration = "INSERT INTO registration VALUES('" + newUser.getUsername() 
                                      + "', '" + hashedPassword
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
        ChatServer.log("New user to be checked: " + user );

        try (Statement stmnt = connectionObj.createStatement()) {
            ResultSet rs = stmnt.executeQuery(getUser);
        
            while (rs.next()) {
                String dbUser = rs.getString("username");
                if (dbUser.equals(user)) {
                    ChatServer.log("User already registered to database");
                    return false;
                }
            }
        }
        ChatServer.log("No match found");
        return true;
    }

    // method for authenticating existing users through /login-function
    public boolean authenticateUser(String username, String password) throws SQLException {

        String getCredentials = "SELECT username, password FROM registration WHERE username = '" 
                                + username + "'";

        try (Statement stmnt = connectionObj.createStatement()) {
            ResultSet rs = stmnt.executeQuery(getCredentials);

            while (rs.next()){
                String dbUser = rs.getString("username");
                String hashedPwd = rs.getString("password");

                if(dbUser.equals(username) && hashedPwd.equals(Crypt.crypt(password, hashedPwd))) {
                    return true;
                }
            }
        }
        ChatServer.log("Invalid login credentials");
        return false;
    }

    // method for storing messages to database
    public void storeMessages(ChatMessage m) throws SQLException {

        String storeUpdate = "INSERT INTO chatmessage VALUES ('" + m.getNickname() + "','" 
                             + m.getMessage() + "','" + m.dateAsInt() + "', '" + m.getLocation() + "', '" + m.getTemperature() + "')";

        try (Statement stmnt = connectionObj.createStatement()) {
            stmnt.executeUpdate(storeUpdate);
        } catch (SQLException e) {
            ChatServer.log("Error while storing chat messages to database " + e.getMessage());
        }
    }

    // method for getting "all" messages from the database
    public List<ChatMessage> getMessages() throws SQLException {
        int i = 0;
        // list for chatmessages queried from database
        List<ChatMessage> messages = new ArrayList<ChatMessage>();
        String getMessage = "SELECT * FROM chatmessage ORDER BY timestamp DESC";

        try (Statement stmnt = connectionObj.createStatement()) {
            ResultSet rs = stmnt.executeQuery(getMessage);
            // "i" reduces the amount of "all" chatmessages returned to client
            // to just 100 newest messages
            while (rs.next() && i < 100) {
                String dbNick = rs.getString("nickname");
                String dbMsg = rs.getString("message");
                String dbLocation = rs.getString("location");
                String dbTemp = rs.getString("temperature");
                LocalDateTime dbTime = null;

                ChatMessage msg = new ChatMessage(dbTime, dbNick, dbMsg, dbLocation, dbTemp);
                
                msg.setSent(rs.getLong("timestamp"));
                messages.add(msg);
                i++;
            }
        } catch (SQLException e) {
            ChatServer.log("Error while getting chat messages from database " + e.getMessage());
        }
        return messages;
    }

    // method for getting just the latest messages (after a certain date) from the database
    public List<ChatMessage> getLatestMessages(long since) throws SQLException {
        // list for chatmessages queried from database
        List<ChatMessage> list = new ArrayList<ChatMessage>();
        // "since" is the "If-Modified-Since" value that is received with the request from the client
        String getLatestMessage = "SELECT * FROM chatmessage WHERE timestamp > " + since
                                + " ORDER BY timestamp ASC";
        
        try (Statement stmnt = connectionObj.createStatement()) {
            ResultSet rs = stmnt.executeQuery(getLatestMessage);
            while (rs.next()) {
                String dbNick = rs.getString("nickname");
                String dbMsg = rs.getString("message");
                String dbLocation = rs.getString("location");
                String dbTemp = rs.getString("temperature");
                LocalDateTime dbTime = null;

                ChatMessage msg = new ChatMessage(dbTime, dbNick, dbMsg, dbLocation, dbTemp);

                msg.setSent(rs.getLong("timestamp"));
                list.add(msg);
            }
        } catch (SQLException e) {
            ChatServer.log("Error while getting latest chat messages from database " + e.getMessage());
        }
        return list;
    }

    public void close() throws SQLException{
        try {
            connectionObj.close();
        } catch (SQLException e) {
            ChatServer.log("Error while closing database connection " + e.getMessage());
        }
    }
}
