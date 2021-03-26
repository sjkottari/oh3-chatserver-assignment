/* 
    Programming 3 Course Assignment - ChatServer.RegistrationHandler
    Author: Santeri Kottari https://github.com/sjkottari
    Project repository: https://github.com/sjkottari/oh3-chatserver-assignment
    Based on works made by Antti Juustila https://github.com/anttijuu
    Information Processing Science - University of Oulu
*/

package com.santeri.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONException;
import org.json.JSONObject;

public class RegistrationHandler implements HttpHandler {

    ChatAuthenticator authenticator = null;

    RegistrationHandler(final ChatAuthenticator auth) {
        authenticator = auth;
    }

    // Handle-method for handling client's POST registration request.
    // Registering the user is carried out in ChatAuthenticator-class.
    // Errors are caught at the end of the method. In case of an error,
    // a response will be sent back to the client, informing about what 
    // went wrong.
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int code = 200;
        String errorMessage = "";

        ChatServer.log("Request being handled in thread: " + Thread.currentThread().getId());

        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                Headers headers = exchange.getRequestHeaders();
                int contentLength = 0;
                String contentType = "";

                if (headers.containsKey("Content-Length")) {
                    contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
                    ChatServer.log("Content-Length: " + contentLength);
                } else {
                    code = 411;
                }
                if (headers.containsKey("Content-Type")) {
                    contentType = headers.get("Content-Type").get(0);
                } else {
                    code = 400;
                    errorMessage = "There was no content type";
                }

                // Verify Content-Type to be 'application/json' as required by server API
                if (!contentType.contains("application/json")) {
                    code = 411;
                    errorMessage = "Content type not supported. Only 'application/json'";
                    ChatServer.log(errorMessage);
                } else {
                    InputStream is = exchange.getRequestBody();
                    String registrationText = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                                              .lines().collect(Collectors.joining("\n"));
                    is.close();

                    // Check registration request body message is not empty. A JSON object
                    // containing the body is also created. It is used for getting relevant
                    // registration message values that are put into newUser-object. It is
                    // used in turn to initialize user registration in another class. Result
                    // will be that a new user is added to the database.
                    if (registrationText != null && !registrationText.trim().isEmpty()) {
                        JSONObject regisJson = new JSONObject(registrationText);
                        String username = regisJson.getString("username");
                        String password = regisJson.getString("password");
                        String email = regisJson.getString("email");

                        if (!username.trim().isEmpty() && !password.trim().isEmpty() && !email.trim().isEmpty()) {
                            User newUser = new User(username, password, email);

                            // addUser-method in separate class returns true if 
                            // adding a user to database is successful.
                            if (authenticator.addUser(username, newUser)) {
                                exchange.sendResponseHeaders(code, -1);
                                ChatServer.log("Added as user: " + username);

                            } else {
                                code = 401;
                                errorMessage = "Invalid registration credentials";
                            }
                        } else {
                            code = 400;
                            errorMessage = "JSON field(s) empty in POST request";
                        }
                    } else {
                        code = 400;
                        errorMessage = "HTTP POST request was empty";
                        ChatServer.log(errorMessage);
                    }
                }
            } else {
                code = 400;
                errorMessage = "ERROR: Feature not supported. Only POST!";
            }
        } catch (IOException e) {
            code = 500;
            errorMessage = "Error while handling the request. " + e.getMessage();
        } catch (JSONException e) {
            code = 500;
            errorMessage = "Error while handling JSON from request. " + e.getMessage();
        } catch (Exception e) {
            code = 500;
            errorMessage = "ERROR: Internal server error. " + e.getMessage();
            e.printStackTrace();
        }

        if (code < 200 || code > 299) {
            ChatServer.log("ERROR: In /registration: " + code + " " + errorMessage);
            byte[] bytes = errorMessage.getBytes("UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream stream = exchange.getResponseBody();
            stream.write(bytes);
            stream.close();
        }
    }
}
