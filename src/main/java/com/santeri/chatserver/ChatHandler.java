// Made by: Santeri Kottari
// Based on works made by: Antti Juustila https://github.com/anttijuu

package com.santeri.chatserver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatHandler implements HttpHandler {
    private String responseBody = "";
    ChatDatabase database = ChatDatabase.getInstance();

    // handle-method for checking client's request, preparing server's
    // response and writing response back to client
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int code = 200;

        ChatServer.log("Request being handled in thread: " + Thread.currentThread().getId());

        try {
            // HTTP POST request handling
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                code = handleChatMessage(exchange);
            // HTTP GET request handling
            } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                code = handleGetRequest(exchange);
            }
            // if HTTP request is not either POST or GET, we end up in a client side error
            else {
                code = 400;
                responseBody = "Feature not supported. Only POST or GET!";
            }
        } catch (IOException e) {
            code = 500;
            responseBody = "Error while handling the request " + e.getMessage();
        } catch (JSONException e) {
            code = 500;
            responseBody = "Error while handling JSON in POST/GET " + e.getMessage();
        } catch (Exception e) {
            code = 500;
            responseBody = "Internal server error " + e.getMessage();
            e.printStackTrace();
        }

        // Any error encountered previously is caught here
        if (code < 200 || code > 299) {
            // log-method prints code and response to server terminal
            ChatServer.log("ERROR: In /chat: " + code + " " + responseBody);
            // response is sent back to user, encoded in UTF-8
            byte[] bytes = responseBody.getBytes("UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            // response body bytes must be written to the stream
            OutputStream stream = exchange.getResponseBody();
            stream.write(bytes);
            stream.close();
        }
    }

    // Method for handling incoming chat messages
    private int handleChatMessage(HttpExchange exchange) throws Exception {
        int code = 200;
        Headers headers = exchange.getRequestHeaders();
        int contentLength = 0;
        String contentType = "";

        // getting content length and type have been refactored to resemble Antti's implementation
        if (headers.containsKey("Content-Length")) {
            contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
        } else {
            code = 411;
            return code;
        }
        if (headers.containsKey("Content-Type")) {
            contentType = headers.get("Content-Type").get(0);
        } else {
            code = 400;
            responseBody = "There was no content type";
            return code;
        }

        // check if headers do not contain type 'text/plain'. Not a desired option.
        if (!contentType.contains("application/json")) {
            code = 411;
            responseBody = "Content type not supported. Only 'application/json'";
            ChatServer.log(responseBody);
        } else {
            InputStream is = exchange.getRequestBody();
            // read text from request body
            String messageText = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                                                    .lines().collect(Collectors.joining("\n"));
            is.close();

            // confirm the read request body is not empty
            if (messageText != null && !messageText.trim().isEmpty()) {
                JSONObject chatJson = new JSONObject(messageText);

                String nickname = chatJson.getString("user");
                String message = chatJson.getString("message");
                LocalDateTime timeSent = null;

                if (!nickname.trim().isEmpty() && !message.trim().isEmpty()) {
                    ChatMessage newMessage = new ChatMessage(timeSent, nickname, message);
                    String dateStr = chatJson.getString("sent");

                    if (!dateStr.trim().isEmpty()) {
                        OffsetDateTime odt = OffsetDateTime.parse(dateStr);
                        newMessage.timeSent = odt.toLocalDateTime();

                        database.storeMessages(newMessage);

                        exchange.sendResponseHeaders(code, -1);
                        ChatServer.log("New message saved");
                    } else {
                        code = 400;
                        responseBody = "JSON field 'sent' empty in POST request";
                    }
                } else {
                    code = 400;
                    responseBody = "JSON field(s) empty in POST request";
                }
            } else {
                code = 400;
                responseBody = "HTTP POST request was empty";
                ChatServer.log(responseBody);
            }
        }
        return code;
    }

    // Method for handling get requests
    private int handleGetRequest(HttpExchange exchange) throws IOException, SQLException {
        int code = 200;
        LocalDateTime latestMsg = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        // new empty list for chatmessages to be sent to the client
        List<ChatMessage> messageList = new ArrayList<ChatMessage>();

        // check if GET headers contain "If-Modified-Since"-key and corresponding value. 
        // If true, only messages after certain timestamp are returned to messageList and 
        // later to client. If false, 100 newest messages are returned to client.
        if (exchange.getRequestHeaders().containsKey("If-Modified-Since")) {
            String modifiedSince = exchange.getRequestHeaders().get("If-Modified-Since").get(0);

            ZonedDateTime modifiedToSend = ZonedDateTime.parse(modifiedSince, formatter);
            LocalDateTime fromWhichDate = modifiedToSend.toLocalDateTime();
            long messagesSince = -1;
            messagesSince = fromWhichDate.toInstant(ZoneOffset.UTC).toEpochMilli();

            messageList = database.getLatestMessages(messagesSince);
        } else {
            messageList = database.getMessages();
        }

        // if there are no messages (or new messages) to send, do this
        if (messageList.isEmpty()) {
            ChatServer.log("No messages to deliver");
            code = 204;
            exchange.sendResponseHeaders(code, -1);
            return code;
        }

        // array for chatmessage jsonobjects
        JSONArray responseMessages = new JSONArray();

        // every chatmessage in the returned messageList is put into a jsonobject
        // which is then put into jsonarray. Datetime variable "latestMsg" is kept
        // up to date on the newest message (timestamp) in the database.
        for (ChatMessage message : messageList){
            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("user", message.getNickname());
            jsonMessage.put("message", message.getMessage());

            LocalDateTime date = message.getTimestamp();

            if (latestMsg == null || message.getTimestamp().compareTo(latestMsg) > 0) {
                latestMsg = message.getTimestamp();
            }
            ZonedDateTime toSend = ZonedDateTime.of(date, ZoneId.of("UTC"));
            String dateText = toSend.format(formatter);
            jsonMessage.put("sent", dateText);

            responseMessages.put(jsonMessage);
        }
        
        // latestMsg is converted to String format
        ZonedDateTime latestToSend = ZonedDateTime.of(latestMsg, ZoneId.of("UTC"));
        String latestDateText = latestToSend.format(formatter);

        // key "Last-Modified" with latestDate-value is added to the headers
        exchange.getResponseHeaders().add("Last-Modified", latestDateText);

        // informing the client of the amount of messages sent back
        ChatServer.log("Delivering " + messageList.size() + " messages to client");
        byte[] bytes = responseMessages.toString().getBytes("UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream stream = exchange.getResponseBody();
        stream.write(bytes);
        stream.close();
        
        return code;
    }
}
