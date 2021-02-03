// Made by: Santeri Kottari
// Based on works made by: Antti Juustila https://github.com/anttijuu

package com.santeri.chatserver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

//@SuppressWarnings("restriction")
public class ChatHandler implements HttpHandler {

    // empty arraylist for storing chat messages
    private ArrayList<String> messages = new ArrayList<String>();

    // handle-method for checking client's request, preparing server's response and
    // writing response back to client
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
                if(!contentType.contains("text/plain")) {
                    code = 400;
                    errorMessage = "ERROR: Content type not supported. Only 'text/plain'";
                }
                else {
                    InputStream is = exchange.getRequestBody();
                    // read text from request body
                    String messageText = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
                            .collect(Collectors.joining("\n"));
                    // confirm the read request body is not empty
                    if(messageText != null && !messageText.trim().isEmpty()) {
                        // add text from request body to messages-arraylist
                        messages.add(messageText);
                        is.close();
                        // send back response code but no response body (-1)
                        exchange.sendResponseHeaders(code, -1);
                    }
                    else {
                        code = 400;
                        errorMessage = "ERROR: HTTP POST request was empty";
                    }
                }

            // HTTP GET request handling
            } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {

                // going through arraylist messages, adding them to messagebody
                String messageBody = "";
                for (String message : messages) {
                    messageBody += message + "\n";
                }
                // message encoded in UTF-8 into bytes array
                byte[] bytes = messageBody.getBytes("UTF-8");
                // response code and length of body is sent back (headers)
                exchange.sendResponseHeaders(code, bytes.length);
                // output stream of bytes, response string bytes must be written to the stream
                OutputStream stream = exchange.getResponseBody();
                stream.write(bytes);
                stream.close();

            } 
            // if HTTP request is not either POST or GET, we end up in a client side error
            else {
                code = 400;
                errorMessage = "ERROR: Feature not supported. Only POST or GET!";
            }

        }
        // if handling the HTTP request fails, we end up in a server side error
        catch (Exception e) {
            code = 500;
            errorMessage = "ERROR: Internal server error";
        }
        // if previously ended up in an error, error code is sent back in response header
        // error message is written in response body (encoded in UTF-8 to byte array)
        if (code >= 400) {
            byte[] bytes = errorMessage.getBytes("UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            // output stream of bytes, response string bytes must be written to the stream
            OutputStream stream = exchange.getResponseBody();
            stream.write(bytes);
            stream.close();
        }

    }

}
