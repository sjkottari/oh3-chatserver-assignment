package com.santeri.chatserver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

//@SuppressWarnings("restriction")
public class ChatHandler implements HttpHandler {

    private ArrayList<String> messages = new ArrayList<String>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int code = 200;
        String errorMessage = "";
        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {

                InputStream input = exchange.getRequestBody();
                String text = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)).lines()
                        .collect(Collectors.joining("\n"));
                messages.add(text);
                input.close();
                exchange.sendResponseHeaders(code, -1);

            } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {

                String messageBody = "";
                for (String message : messages) {
                    messageBody += message + "\n";
                }
                byte[] bytes = messageBody.getBytes("UTF-8");
                exchange.sendResponseHeaders(code, bytes.length);

                OutputStream stream = exchange.getResponseBody();
                stream.write(bytes);
                stream.close();

            } else {
                code = 400;
                errorMessage = "Not supported";
            }

        } catch (Exception e) {
            code = 500;
            errorMessage = "Internal server error";
        }
        if (code >= 400) {
            byte[] bytes = errorMessage.getBytes("UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream stream = exchange.getResponseBody();
            stream.write(bytes);
            stream.close();
        }

    }

}
