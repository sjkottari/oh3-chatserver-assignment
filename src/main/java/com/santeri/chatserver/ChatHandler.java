// Made by: Santeri Kottari
// Based on works made by: Antti Juustila https://github.com/anttijuu

package com.santeri.chatserver;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;

public class ChatHandler implements HttpHandler {
    private String responseBody = "";
    ChatDatabase database = ChatDatabase.getInstance();

    // handle-method for checking client's request, preparing server's
    // response and writing response back to client
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int code = 200;

        ChatServer.log("Request being handled in thread: " + Thread.currentThread().getId());

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
            responseBody = "Error while handling the request " + e.getMessage();
        } catch (JSONException e) {
            code = 500;
            responseBody = "Error while handling JSON in POST/GET " + e.getMessage();
        } catch (Exception e) {
            code = 500;
            responseBody = "Internal server error " + e.getMessage();
            e.printStackTrace();
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
        if (!contentType.contains("application/json")) {
            code = 411;
            responseBody = "Content type not supported. Only 'application/json'";
            ChatServer.log(responseBody);
        } else {
            InputStream is = exchange.getRequestBody();
            // read text from request body
            String messageText = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                                                    .lines().collect(Collectors.joining("\n"));
            is.close();

            // confirm the read request body is not empty
            if (messageText != null && !messageText.trim().isEmpty()) {
                JSONObject chatJson = new JSONObject(messageText);

                String nickname = chatJson.getString("user");
                String message = chatJson.getString("message");
                LocalDateTime timeSent = null;
                String location = null;
                String temperature = null;

                if (!nickname.trim().isEmpty() && !message.trim().isEmpty()) {

                    String dateStr = chatJson.getString("sent");
                    if (!dateStr.trim().isEmpty()) {
                        //LocalDateTime timeSent = null;
                        OffsetDateTime odt = OffsetDateTime.parse(dateStr);
                        timeSent = odt.toLocalDateTime();

                        ChatMessage newMessage = new ChatMessage(timeSent, nickname, message, location, temperature);

                        if (chatJson.has("location")) {
                            location = chatJson.getString("location");
                            if (!location.trim().isEmpty() && !location.equalsIgnoreCase("null")) {
                                String[] weatherData = getWeatherData(location, temperature);

                                newMessage.location = weatherData[0];
                                newMessage.temperature = weatherData[1];
                            } else {
                                ChatServer.log("'location' specified in POST request but was empty");
                            }
                        }

                        database.storeMessages(newMessage);
                        exchange.sendResponseHeaders(code, -1);
                        ChatServer.log("New message saved");
                    } else {
                        code = 400;
                        responseBody = "JSON field 'sent' empty in POST request";
                    }
                } else {
                    code = 400;
                    responseBody = "JSON field(s) empty in POST request";
                }
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
        LocalDateTime latestMsg = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        // new empty list for chatmessages to be sent to the client
        List<ChatMessage> messageList = new ArrayList<ChatMessage>();

        // check if GET headers contain "If-Modified-Since"-key and corresponding value. 
        // If true, only messages after certain timestamp are returned to messageList and 
        // later to client. If false, 100 newest messages are returned to client.
        if (exchange.getRequestHeaders().containsKey("If-Modified-Since")) {
            String modifiedSince = exchange.getRequestHeaders().get("If-Modified-Since").get(0);

            ZonedDateTime modifiedToSend = ZonedDateTime.parse(modifiedSince, formatter);
            LocalDateTime fromWhichDate = modifiedToSend.toLocalDateTime();
            long messagesSince = -1;
            messagesSince = fromWhichDate.toInstant(ZoneOffset.UTC).toEpochMilli();

            messageList = database.getLatestMessages(messagesSince);
        } else {
            messageList = database.getMessages();
        }

        // if there are no messages (or new messages) to send, do this
        if (messageList.isEmpty()) {
            ChatServer.log("No messages to deliver");
            code = 204;
            exchange.sendResponseHeaders(code, -1);
            return code;
        }

        // array for chatmessage jsonobjects
        JSONArray responseMessages = new JSONArray();

        // every chatmessage in the returned messageList is put into a jsonobject
        // which is then put into jsonarray. Datetime variable "latestMsg" is kept
        // up to date on the newest message (timestamp) in the database.
        for (ChatMessage message : messageList){
            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("user", message.getNickname());
            jsonMessage.put("message", message.getMessage());

            LocalDateTime date = message.getTimestamp();

            if (latestMsg == null || message.getTimestamp().compareTo(latestMsg) > 0) {
                latestMsg = message.getTimestamp();
            }
            ZonedDateTime toSend = ZonedDateTime.of(date, ZoneId.of("UTC"));
            String dateText = toSend.format(formatter);
            jsonMessage.put("sent", dateText);

            if (!message.getLocation().trim().isEmpty() && !message.getLocation().equalsIgnoreCase("null")) {
                jsonMessage.put("location", message.getLocation());
                jsonMessage.put("temperature", message.getTemperature());
            }

            responseMessages.put(jsonMessage);
        }
        
        // latestMsg is converted to String format
        ZonedDateTime latestToSend = ZonedDateTime.of(latestMsg, ZoneId.of("UTC"));
        String latestDateText = latestToSend.format(formatter);

        // key "Last-Modified" with latestDate-value is added to the headers
        exchange.getResponseHeaders().add("Last-Modified", latestDateText);

        // informing the client of the amount of messages sent back
        ChatServer.log("Delivering " + messageList.size() + " messages to client");
        byte[] bytes = responseMessages.toString().getBytes("UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream stream = exchange.getResponseBody();
        stream.write(bytes);
        stream.close();
        
        return code;
    }

    private static String[] getWeatherData(String location, String temperature) throws Exception {

        URL url = new URL("http://opendata.fmi.fi/wfs/fin?service=WFS&version=2.0.0&request=" 
                        + "GetFeature&storedquery_id=fmi::observations::weather::timevaluepair&place=" 
                        + location 
                        + "&parameters=temperature&");

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;

        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(20000);

            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true); //tarviiko?
            urlConnection.setDoInput(true); //tarviiko?

            System.out.println(urlConnection.getResponseCode());
            inputStream = urlConnection.getInputStream();
            String inputDump = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
                                .collect(Collectors.joining("\n"));
            inputStream.close();

            Document doc = convertToXML(inputDump);

            NodeList weatherList = doc.getElementsByTagName("wml2:MeasurementTVP");
            Node node = weatherList.item(weatherList.getLength()-1);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node;
                temperature = elem.getElementsByTagName("wml2:value").item(0).getTextContent();

                String[] weatherData = {location, temperature};
                return weatherData;
            }

        } catch (Exception e) {
            e.printStackTrace(); //change this
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return null;
    }

    private static Document convertToXML(String xmlString) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = null;
        Document doc = null;

        builder = factory.newDocumentBuilder();
        doc = builder.parse(new InputSource(new StringReader(xmlString)));
        return doc;
    }
}
