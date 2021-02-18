// Made by: Santeri Kottari
// Based on works made by: Antti Juustila https://github.com/anttijuu

package com.santeri.chatserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.time.LocalDateTime;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class ChatServer {
    public static void main(String[] args) throws Exception {
        try {
            log("Launching ChatServer");
            // create a new Http server instance to socket address (port) 8001
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);
            SSLContext sslContext = chatServerSSLContext();

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
            // create new Http context "/chat" and specify a handler for incoming requests
            HttpContext httpcontext = server.createContext("/chat", new ChatHandler());
            // set authenticator for httpcontext
            httpcontext.setAuthenticator(auth);
            // create new context for registration with reference to authenticator
            server.createContext("/registration", new RegistrationHandler(auth));

            server.setExecutor(null);
            server.start();
            database.open("chatdatabase.db");
            log("ChatServer running...");

        } catch (FileNotFoundException e) {
            // failed at finding certificate file
            log("Certificate not found " + e.getMessage());
        } catch (Exception e) {
            log("Failed to create HTTP server" + e.getMessage());
        }

    }

    private static SSLContext chatServerSSLContext() throws Exception {

        // SSL passphrase
        char[] passphrase = "kuukupoopotin".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("keystore.jks"), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;
    }

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    // method for printing server terminal logs
    public static void log(String message) {
        System.out.println(ANSI_GREEN + LocalDateTime.now() + ANSI_RESET + " " + message);
    }

}
