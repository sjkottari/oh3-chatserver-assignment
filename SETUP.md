# How to set up ChatServer

In directory: `C:\Users\Santeri\Scripts\OH3-exercises\ChatServer\chatserver`

cmd:

- `mvn package`
- `java -jar target/chatserver-1.0-SNAPSHOT-jar-with-dependencies.jar chatdatabase.db keystore.jks kuukupoopotin`

    * Where:
        * chatdatabase.db = desired name for database
        * kuukupoopotin = password for the keystore
