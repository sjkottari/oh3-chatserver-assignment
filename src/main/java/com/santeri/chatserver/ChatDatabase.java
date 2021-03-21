/* 
    Programming 3 Course Assignment - ChatServer.ChatDatabase
    Author: Santeri Kottari https://github.com/sjkottari
    Project repository: https://github.com/sjkottari/oh3-chatserver-assignment
    Information Processing Science - University of Oulu
*/

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

    // Database-object is implemented as singleton in a synchronized method.
    // Ensures there is only one object in thread & operations are in sync.
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

    // Method for opening the database. Name for DB is given as command
    // line argument and it's prior existence is checked. Connection to
    // the database is established with a path to local database.
    public void open(String dbName) throws SQLException {
        boolean dbExistence;

        File file = new File(dbName);
        if (!file.isFile() && !file.isDirectory()) {
            dbExistence = false;
        } else {
            dbExistence = true;
        }

        try {
            String dbPath = new String("jdbc:sqlite:" + file.getAbsolutePath());
            connectionObj = DriverManager.getConnection(dbPath);

            // Database is initialized in separate method if it's not found prior.
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

    // Method for creating the database with few set tables. A new
    // statement is created with connection-object. Updates are then
    // executed to database. NEW: Added new attributes 'location' and
    // 'temperature' to schema in chatmessage-table.
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
                Statement stmnt = connectionObj.createStatement();
                stmnt.executeUpdate(createRegTable);
                stmnt.executeUpdate(createChatTable);
                stmnt.close();
                return true;
            }
        } catch (SQLException e) {
            ChatServer.log("Error while initializing database " + e.getMessage());
        }
        return false;
    }

    // Method for registering a new user into the database. If username-parameter
    // is valid in validateUser()-method, we can proceed on creating a new
    // registration for the user. User password is hashed with crypt-library method.
    public boolean registerUser(String username, User newUser) throws SQLException {

        if (validateUser(username)) {
            String hashedPassword = Crypt.crypt(newUser.getPassword());
            String createRegistration = "INSERT INTO registration VALUES('" + newUser.getUsername() 
                                       + "', '" + hashedPassword + "', '" + newUser.getEmail() + "')";

            Statement stmnt = connectionObj.createStatement();
            stmnt.executeUpdate(createRegistration);
            stmnt.close();
            return true;
        } else {
            ChatServer.log("Username validation failed");
            return false;
        }
    }

    // Method for checking if a user already exists in the database. Returns
    // 'true' if no username matches in the database.
    private boolean validateUser(String user) throws SQLException {

        String getUser = "SELECT username FROM registration WHERE username = '" + user + "'";
        ChatServer.log("New user to be checked: " + user);

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

    // Method for authenticating existing users. In use when users send
    // POST/GET requests in '/chat'-realm. Username and password are
    // queried from the database. Queried credentials are then compared
    // with ones the user has provided in POST/GET-request in '/chat'.
    public boolean authenticateUser(String username, String password) throws SQLException {

        String getCredentials = "SELECT username, password FROM registration WHERE username = '" 
                               + username + "'";

        try (Statement stmnt = connectionObj.createStatement()) {
            ResultSet rs = stmnt.executeQuery(getCredentials);
            while (rs.next()) {
                String dbUser = rs.getString("username");
                String hashedPwd = rs.getString("password");
                if (dbUser.equals(username) && hashedPwd.equals(Crypt.crypt(password, hashedPwd))) {
                    return true;
                }
            }
        }
        ChatServer.log("Invalid login credentials");
        return false;
    }

    // Method for storing messages to database. Update statement is
    // constructed from elements in ChatMessage-object. Timestamp is
    // inserted to database in epoch time format.
    public void storeMessages(ChatMessage m) throws SQLException {

        String storeUpdate = "INSERT INTO chatmessage VALUES ('" + m.getNickname() + "','"
                            + m.getMessage() + "','" + m.dateAsInt() + "', '"
                            + m.getLocation() + "', '" + m.getTemperature() + "')";

        try (Statement stmnt = connectionObj.createStatement()) {
            stmnt.executeUpdate(storeUpdate);
        } catch (SQLException e) {
            ChatServer.log("Error while storing chat messages to database " + e.getMessage());
        }
    }

    // Method for getting "all" messages from the database. Invariant 'i' reduces
    // the amount of chatmessages returned to client to just 100 newest messages
    // time-wise. Regarding fmi.fi weather data, location and temperature
    // -attributes are also queried. Sent-timestamp is converted from epoch time
    // to LocalDateTime on line 211 as required by server API.
    public List<ChatMessage> getMessages() throws SQLException {
        int i = 0;
        List<ChatMessage> messages = new ArrayList<ChatMessage>();
        String getMessage = "SELECT * FROM chatmessage ORDER BY timestamp DESC";

        try (Statement stmnt = connectionObj.createStatement()) {
            ResultSet rs = stmnt.executeQuery(getMessage);
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

    // Method for getting just the latest messages from the database after a
    // certain date. In the query, 'since' is the "If-Modified-Since" value
    // that is received with the request from the client. Otherwise very similar
    // to the getMessages()-method above.
    public List<ChatMessage> getLatestMessages(long since) throws SQLException {
        List<ChatMessage> list = new ArrayList<ChatMessage>();
        String getLatestMessage = "SELECT * FROM chatmessage WHERE timestamp > "
                                 + since + " ORDER BY timestamp ASC";

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

    // Small method for closing up the database.
    public void close() throws SQLException {
        try {
            connectionObj.close();
        } catch (SQLException e) {
            ChatServer.log("Error while closing database connection " + e.getMessage());
        }
    }
}
