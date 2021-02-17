// Made by: Santeri Kottari
// Based on works made by: Antti Juustila https://github.com/anttijuu

package com.santeri.chatserver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatHandler implements HttpHandler {
    private String responseBody = "";
    // empty arraylist for storing chat messages
    private ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();
    ChatDatabase database = ChatDatabase.getInstance();

    // handle-method for checking client's request, preparing server's
    // response and writing response back to client
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int code = 200;

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
            responseBody = "Error while handling the request" + e.getMessage();
        } catch (JSONException e) {
            code = 500;
            responseBody = "Error while handling JSON in POST/GET" + e.getMessage();
        } catch (Exception e) {
            code = 500;
            responseBody = "Internal server error" + e.getMessage();
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
            String messageText = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
                                                    .collect(Collectors.joining("\n"));
            ChatServer.log(messageText); // send read message to client
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

                        messages.add(newMessage);
                        Collections.sort(messages, new Comparator<ChatMessage>() {
                            @Override
                            public int compare(ChatMessage lhs, ChatMessage rhs) {
                                return lhs.timeSent.compareTo(rhs.timeSent);}
                        });
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

        if (messages.isEmpty()) {
            ChatServer.log("No messages to deliver");
            code = 204;
            exchange.sendResponseHeaders(code, -1);
            return code;
        } else {
            JSONArray responseMessages = new JSONArray();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

            for (ChatMessage message : messages){
                JSONObject jsonMessage = new JSONObject();
                jsonMessage.put("user", message.getNickname());
                jsonMessage.put("message", message.getMessage());

                LocalDateTime date = message.timeSent;
                ZonedDateTime toSend = ZonedDateTime.of(date, ZoneId.of("UTC"));
                String dateText = toSend.format(formatter);
                jsonMessage.put("sent", dateText);

                responseMessages.put(jsonMessage);
            }

            // informing the client of the amount of messages sent back
            ChatServer.log("Delivering " + messages.size() + " messages to client");
            byte[] bytes = responseMessages.toString().getBytes("UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream stream = exchange.getResponseBody();
            stream.write(bytes);
            stream.close();
        }
        return code;
    }
}
