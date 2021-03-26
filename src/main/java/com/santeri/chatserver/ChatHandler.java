/* 
    Programming 3 Course Assignment - ChatServer.ChatHandler
    Author: Santeri Kottari https://github.com/sjkottari
    Project repository: https://github.com/sjkottari/oh3-chatserver-assignment
    Based on works made by Antti Juustila https://github.com/anttijuu
    Information Processing Science - University of Oulu
*/

package com.santeri.chatserver;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
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

    // Handle-method for handling client's POST/GET request. Server's
    // actions and responses are carried out in separate methods. If request
    // is not either POST or GET, we end up in error. This error is caught
    // along with other errors at the end of the method. In case of an error,
    // a response will be sent back to the client, informing about what went wrong.
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int code = 200;
        ChatServer.log("Request being handled in thread: " + Thread.currentThread().getId());

        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                code = handleChatMessage(exchange);
            } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                code = handleGetRequest(exchange);
            } else {
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

        if (code < 200 || code > 299) {
            ChatServer.log("ERROR: In /chat: " + code + " " + responseBody);
            byte[] bytes = responseBody.getBytes("UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream stream = exchange.getResponseBody();
            stream.write(bytes);
            stream.close();
        }
    }

    // Method for handling incoming POST requests (chat messages) from client.
    private int handleChatMessage(HttpExchange exchange) throws Exception {
        int code = 200;
        Headers headers = exchange.getRequestHeaders();
        int contentLength = 0;
        String contentType = "";

        if (headers.containsKey("Content-Length")) {
            contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
            ChatServer.log("Content-Length: " + contentLength);
        } else {
            code = 411;
            responseBody = "There was no content length";
            return code;
        }
        if (headers.containsKey("Content-Type")) {
            contentType = headers.get("Content-Type").get(0);
        } else {
            code = 400;
            responseBody = "There was no content type";
            return code;
        }

        // Verify Content-Type to be 'application/json' as required by server API
        if (!contentType.contains("application/json")) {
            code = 411;
            responseBody = "Content type not supported. Only 'application/json'";
            ChatServer.log(responseBody);
        } else {
            InputStream is = exchange.getRequestBody();
            String messageText = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                                 .lines().collect(Collectors.joining("\n"));
            is.close();

            // Check request body message is not empty. A JSON object containing
            // the body is also created. It is used for getting relevant chat
            // message values that can be put into ChatMessage-object, which in
            // turn will be stored to database.
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
                        OffsetDateTime odt = OffsetDateTime.parse(dateStr);
                        timeSent = odt.toLocalDateTime();

                        ChatMessage newMessage = new ChatMessage(timeSent, nickname, message, 
                                                                 location, temperature);

                        // If user has provided their location in the request body JSON,
                        // this block activates. Location string is get from JSON and 
                        // checked to be not empty. Getting the actual weather data 
                        // happens in separate getWeatherData()-method. 
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

                        // Chat messages are stored to database
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

    // Method for handling GET requests from client
    private int handleGetRequest(HttpExchange exchange) throws IOException, SQLException {
        int code = 200;
        LocalDateTime latestMsg = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        List<ChatMessage> messageList = new ArrayList<ChatMessage>();

        // Check if GET headers contain "If-Modified-Since"-key and corresponding value.
        // If true, messages only after certain timestamp are returned to messageList and
        // later to client. If false, 100 newest messages are returned to client.
        if (exchange.getRequestHeaders().containsKey("If-Modified-Since")) {
            String modifiedSince = exchange.getRequestHeaders().get("If-Modified-Since").get(0);
            long messagesSince = -1;

            ZonedDateTime modifiedToSend = ZonedDateTime.parse(modifiedSince, formatter);
            LocalDateTime fromWhichDate = modifiedToSend.toLocalDateTime();
            messagesSince = fromWhichDate.toInstant(ZoneOffset.UTC).toEpochMilli();
            messageList = database.getLatestMessages(messagesSince);

        } else {
            messageList = database.getMessages();
        }

        // If there are no messages (or new messages) to deliver, following occurs.
        if (messageList.isEmpty()) {
            ChatServer.log("No messages to deliver");
            code = 204;
            exchange.sendResponseHeaders(code, -1);
            return code;
        }

        JSONArray responseMessages = new JSONArray();

        // Every chatmessage in the returned messageList is put into a jsonobject
        // which is then put into jsonarray. Datetime variable 'latestMsg' is kept
        // up to date on the newest message (timestamp) in the database.
        for (ChatMessage message : messageList) {
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

            // Location and temperature are not put into jsonobject if they're empty.
            // This allows for personalized chat messages to be returned.
            if (!message.getLocation().trim().isEmpty() && !message.getLocation().equalsIgnoreCase("null")) {
                jsonMessage.put("location", message.getLocation());
                jsonMessage.put("temperature", message.getTemperature());
            }
            responseMessages.put(jsonMessage);
        }

        ZonedDateTime latestToSend = ZonedDateTime.of(latestMsg, ZoneId.of("UTC"));
        String latestDateText = latestToSend.format(formatter);
        // key 'Last-Modified' with latestDate-value is added to response headers
        exchange.getResponseHeaders().add("Last-Modified", latestDateText);

        ChatServer.log("Delivering " + messageList.size() + " messages to client");
        byte[] bytes = responseMessages.toString().getBytes("UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream stream = exchange.getResponseBody();
        stream.write(bytes);
        stream.close();

        return code;
    }

    // Method for getting weather data from fmi.fi -service. Part of Advanced-API Feature:
    // Attach weather information to user messages from fmi.fi -service. URL to the service
    // is formed to specifically get the temperature at the user-given location.
    private static String[] getWeatherData(String location, String temperature) throws Exception {

        URL url = new URL("http://opendata.fmi.fi/wfs/fin?service=WFS&version=2.0.0&request="
                + "GetFeature&storedquery_id=fmi::observations::weather::timevaluepair&place=" 
                + location + "&parameters=temperature&");
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;

        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(20000);

            // We want to get data from the service, so request method is set as 'GET'.
            // Connection is intended to be used for output and input, so their methods
            // DoOutput() and DoInput() are set as 'true' respectively.
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            ChatServer.log("WFService responded with: " + urlConnection.getResponseCode());

            inputStream = urlConnection.getInputStream();
            String inputDump = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                               .lines().collect(Collectors.joining("\n"));
            inputStream.close();

            // InputDump from the service is converted to a more
            // manageable XML format in a separate method.
            Document doc = convertToXML(inputDump);

            // XML elements with given tag are get from the document
            NodeList weatherList = doc.getElementsByTagName("wml2:MeasurementTVP");

            // Last item from the node list is get. The last item contains
            // the most up-to-date weather data in the list.
            Node node = weatherList.item(weatherList.getLength() - 1);

            // New element is created from the last node. Temperature
            // value is get from the element by certain tag name in string format.
            // String[] containing user location and location temperature are returned.
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node;
                temperature = elem.getElementsByTagName("wml2:value").item(0).getTextContent();

                String[] weatherData = { location, temperature };
                return weatherData;
            }
        } catch (SocketTimeoutException e) {
            ChatServer.log("Connection timed out while getting weather data: " + e.getMessage());
        } catch (Exception e) {
            ChatServer.log("Error while getting weather data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }

    // Method for converting the received input string from the service to XML.
    // A new instance of DocumentBuilderFactory is created, which enables
    // the use of DocumentBuilder for parsing the input string to XML doc.
    private static Document convertToXML(String inputDump) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(inputDump)));

        return doc;
    }
}
