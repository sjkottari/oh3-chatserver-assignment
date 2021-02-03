// Made by: Santeri Kottari
// Based on works made by: Antti Juustila https://github.com/anttijuu

package com.santeri.chatserver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ChatHandler implements HttpHandler {
    private String responseBody = "";
    // empty arraylist for storing chat messages
    private ArrayList<String> messages = new ArrayList<String>();

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
        }
        // if handling the HTTP request fails, we end up in a server side error
        catch (Exception e) {
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
        if (!contentType.contains("text/plain")) {
            code = 411;
            responseBody = "Content type not supported. Only 'text/plain'";
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
                // add text from request body to messages-arraylist
                messages.add(messageText);
                // send back response code but no response body (-1)
                exchange.sendResponseHeaders(code, -1);
                ChatServer.log("New message saved");
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
        }
        // going through arraylist messages, adding them to messageBody
        String messageBody = "";
        for (String message : messages) {
            messageBody += message + "\n";
        }
        // informing the client of the amount of messages sent back
        ChatServer.log("Delivering " + messages.size() + " messages to client");
        // message encoded in UTF-8 into bytes array
        byte[] bytes = messageBody.toString().getBytes("UTF-8");
        // response code and length of body is sent back (headers)
        exchange.sendResponseHeaders(code, bytes.length);
        // response string body must be written to the stream
        OutputStream stream = exchange.getResponseBody();
        stream.write(bytes);
        stream.close();
        return code;
    }

}
