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
import java.util.List;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

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
            // HTTP POST request handling
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {

                // get content type from request headers
                List<String> contentType = exchange.getRequestHeaders().get("Content-Type");
                System.out.println(contentType);

                // check if content type is correct
                if (!contentType.contains("text/plain")) {
                    code = 400;
                    errorMessage = "ERROR: Content type not supported. Only 'text/plain'";
                } else {
                    InputStream is = exchange.getRequestBody();
                    // read text from request body
                    String registrationText = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                            .lines().collect(Collectors.joining("\n"));

                    // confirm the read request body is not empty
                    if (registrationText != null && !registrationText.trim().isEmpty()) {

                        //WIP
                        String[] items = registrationText.split(":", 2);
                        
                        if (items.length == 2) {
                            // myös isEmpty?
                            if (items[0].trim().length() > 0 && items[1].trim().length() > 0) {

                                if (authenticator.addUser(items[0], items[1])) {
                                    // send back response code but no response body (-1)
                                    exchange.sendResponseHeaders(code, -1);
                                    // ChatServer.log("Added as user"); TEE TÄÄ MAYBE
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
                        errorMessage = "ERROR: HTTP POST request was empty";
                    }
                }

            }
            // if HTTP request is not either POST or GET, we end up in a client side error
            else {
                code = 400;
                errorMessage = "ERROR: Feature not supported. Only POST!";
            }

        }
        // if handling the HTTP request fails, we end up in a server side error
        catch (IOException e) {
            code = 500;
            errorMessage = "Error while handling the request";
        }
        catch (Exception e) {
            code = 500;
            errorMessage = "ERROR: Internal server error";
        }

        if (code < 200 || code > 299) {
            byte[] bytes = errorMessage.getBytes("UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            // output stream of bytes, response string bytes must be written to the stream
            OutputStream stream = exchange.getResponseBody();
            stream.write(bytes);
            stream.close();
        }

    }

}
