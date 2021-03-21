/* 
    Programming 3 Course Assignment - ChatServer
    Author: Santeri Kottari https://github.com/sjkottari
    Project repository: https://github.com/sjkottari/oh3-chatserver
    Based on works made by Antti Juustila https://github.com/anttijuu
    Information Processing Science - University of Oulu
*/

package com.santeri.chatserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class ChatServer {
    // Main-method of ChatServer. First, a new server instance is created to socket address 
    // (port) 8001. Then a new SSLContext is created which configures the server to use 
    // TLS/HTTPS. Onwards from line 54, new database singleton object and new Http contexts 
    // "/chat" and "/registration" are created. Http contexts have appropriate 
    // authentication and handles. Multi thread execution is also enabled on line 64.
    public static void main(String[] args) throws Exception {
        try {
            log("Launching ChatServer");
            if (args.length != 3) {
                log("Usage 'java -jar jar-file.jar database-name.db keystore.jks cert-password'");
                return;
            }
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);
            SSLContext sslContext = chatServerSSLContext(args);

            // Configuring server to use SSLContext for TLS/HTTPS-connection
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });
            ChatDatabase database = ChatDatabase.getInstance();
            ChatAuthenticator auth = new ChatAuthenticator("/chat");

            HttpContext httpcontext = server.createContext("/chat", new ChatHandler());
            httpcontext.setAuthenticator(auth);
            server.createContext("/registration", new RegistrationHandler(auth));

            // Open database with name argument
            database.open(args[0]);

            Executor exec = Executors.newCachedThreadPool();
            server.setExecutor(exec);
            server.start();
            log("ChatServer running...");

            // Loop for handling server & DB shutdown with '/quit'-command
            boolean running = true;
            while (running) {
                String input = System.console().readLine();

                if (input.equalsIgnoreCase("/quit")) {
                    running = false;
                    database.close();
                    server.stop(3);
                }
            }

        } catch (FileNotFoundException e) {
            log("Certificate not found " + e.getMessage());
        } catch (Exception e) {
            log("Failed to create HTTP server" + e.getMessage());
        }
    }

    // Method for configuring the server to use TLS/HTTPS. Receives given command line
    // args (keystore & passphrase for keystore) as parameters.
    private static SSLContext chatServerSSLContext(String[] args) throws Exception {

        char[] passphrase = args[2].toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(args[1]), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;
    }

    // Some colour choices for the output log text elements
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    // Method for printing server terminal/command line output logs
    public static void log(String message) {
        System.out.println(ANSI_GREEN + LocalDateTime.now() + ANSI_RESET + " " + message);
    }
}
