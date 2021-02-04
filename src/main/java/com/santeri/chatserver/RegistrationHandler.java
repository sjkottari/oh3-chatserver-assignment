// Made by: Santeri Kottari
// Based on works made by: Antti Juustila https://github.com/anttijuu

package com.santeri.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONException;
import org.json.JSONObject;

public class RegistrationHandler implements HttpHandler {

    ChatAuthenticator authenticator = null;
    RegistrationHandler(ChatAuthenticator auth) {
        authenticator = auth;
    }
    private ArrayList<String> regisMessages = new ArrayList<String>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int code = 200;
        String errorMessage = "";

        try {
            // registration POST request handling
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                Headers headers = exchange.getRequestHeaders();
                int contentLength = 0;
                String contentType = "";

                // getting content length and type have been refactored to resemble Antti's implementation
                if (headers.containsKey("Content-Length")) {
                    contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
                } else {
                    code = 411;
                }
                if (headers.containsKey("Content-Type")) {
                    contentType = headers.get("Content-Type").get(0);
                } else {
                    code = 400;
                    errorMessage = "There was no content type";
                }

                // check if headers do not contain type 'text/plain'
                if (!contentType.contains("application/json")) {
                    code = 411;
                    errorMessage = "Content type not supported. Only 'application/json'";
                    ChatServer.log(errorMessage);
                } else {
                    InputStream is = exchange.getRequestBody();
                    // read registration credentials from request body
                    String registrationText = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                            .lines().collect(Collectors.joining("\n"));
                    ChatServer.log(registrationText);
                    is.close();

                    JSONObject regisJson = new JSONObject(registrationText);

                    String username = regisJson.getString("username");
                    String password = regisJson.getString("password");
                    String email = regisJson.getString("email");

                    User newUser = new User(username, password, email);

                    if (authenticator.addUser(username, newUser)) {
                        exchange.sendResponseHeaders(code, -1);
                        ChatServer.log("Added as user");
                    } else {
                        code = 400;
                        errorMessage = "Invalid user credentials";
                    }

                    /*
                    // confirm the read request body is not empty
                    if (registrationText != null && !registrationText.trim().isEmpty()) {
                        // credentials are split by ':' into 2
                        String[] items = registrationText.split(":", 2);
                        // check if there are exactly 2 items in string-array
                        if (items.length == 2) {
                            // check if credentials in items[] are more than 0 in length
                            if (items[0].trim().length() > 0 && items[1].trim().length() > 0) {
                                // if method addUser returns true, credentials are successfully registered
                                if (authenticator.addUser(items[0], items[1])) {
                                    // send back response code but no response body (-1)
                                    exchange.sendResponseHeaders(code, -1);
                                    ChatServer.log("Added as user");
                                } else {
                                    code = 400;
                                    errorMessage = "Invalid user credentials";
                                }
                            } else {
                                code = 400;
                                errorMessage = "Invalid user credentials";
                            }
                        } else {
                            code = 400;
                            errorMessage = "Invalid user credentials";
                        }
                        // add text from request body to messages-arraylist
                        regisMessages.add(registrationText);
                        is.close();
                    } else {
                        code = 400;
                        errorMessage = "HTTP POST request was empty";
                        ChatServer.log(errorMessage);
                    } */
                }
            }
            // if request isn't POST, we end up in a client side error
            else {
                code = 400;
                errorMessage = "ERROR: Feature not supported. Only POST!";
            }
        }
        // if handling the request fails, we end up in a server side error
        catch (IOException e) {
            code = 500;
            errorMessage = "Error while handling the request";
        } catch (JSONException e) {
            code = 500;
            errorMessage = "Error while handling JSON from request";
        } catch (Exception e) {
            code = 500;
            errorMessage = "ERROR: Internal server error";
        }

        // Any error encountered previously is caught here
        if (code < 200 || code > 299) {
            // log-method prints code and response to server terminal
            ChatServer.log("ERROR: In /registration: " + code + " " + errorMessage);
            byte[] bytes = errorMessage.getBytes("UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            // response body bytes must be written to the stream
            OutputStream stream = exchange.getResponseBody();
            stream.write(bytes);
            stream.close();
        }
    }
}
