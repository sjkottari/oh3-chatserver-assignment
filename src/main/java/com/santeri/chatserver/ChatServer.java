// Made by: Santeri Kottari
// Based on works made by: https://github.com/anttijuu

package com.santeri.chatserver;

import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

//@SuppressWarnings("restriction")
public class ChatServer {
    public static void main(String[] args) throws Exception {

        try {

            // create a new Http server instance to socket address (port) 8001
            HttpServer server = HttpServer.create(new InetSocketAddress(8001), 0);
            // create new Http context "/chat" and specify a handler for incoming requests
            server.createContext("/chat", new ChatHandler());
            // executing HTTP server
            server.setExecutor(null);
            server.start();

        } catch (Exception e) {
            System.out.println("Failed to create HTTP server");
        }

    }
}
