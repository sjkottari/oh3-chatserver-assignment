// Made by: Santeri Kottari
// Based on works made by: Antti Juustila https://github.com/anttijuu

package com.santeri.chatserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpHandler;

//@SuppressWarnings("restriction")
public class ChatServer {
    public static void main(String[] args) throws Exception {

        try {
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
            //HttpHandler handler = new HttpHandler("/registration");

            ChatAuthenticator auth = new ChatAuthenticator("/chat");
            // create new Http context "/chat" and specify a handler for incoming requests
            HttpContext httpcontext = server.createContext("/chat", new ChatHandler());
            httpcontext.setAuthenticator(auth);

            server.createContext("/registration", new RegistrationHandler(auth));
            server.setExecutor(null);
            server.start();

        } catch (FileNotFoundException e) {
            // failed in finding certificate file
            System.out.println("Certificate not found");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Failed to create HTTP server");
            e.printStackTrace();
        }

    }

    private static SSLContext chatServerSSLContext() throws Exception {

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

}
