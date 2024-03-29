# oh3-chatserver-assignment

This repository is for Ohjelmointi 3 (811367A) Course Assignment Project source files.

### Author information:
- Full name: Santeri Juhani Kottari
- StudentID: Y52934672
- Email: kosaju99@gmail.com / skottari19@student.oulu.fi

### Advanced API
- Feature: Attach weather information to user messages from fmi.fi -service.
  - Curl usage with chat json-file required.

Example contents:
```json
{
  "user" : "nickname",
  "message" : "contents of the message",
  "location" : "Oulu",
  "sent" : "2021-03-26T12:00:00.123Z"
}
```

### Important note:
There is an elusive bug in the code that was discussed with @anttijuu during 5th of March. The bug relates to Crypt.crypt -function creating invalid salt values with the implementation presented in Exercise 5. The aforementioned salt value caused errors randomly during parallel unit testing, approximately 1 out of 10 times. 'Bytes & Base64 encoded' -implementation was discarded in favor of a simpler implementation. As a result, no further random errors have arosen during testing. The case was resolved as not being a fault in source code files, and the new implementation was approved by @anttijuu.

System & SW information for future reference:
* Model: Matebook D Signature Edition
* Processor: Intel Core i7-8550U
* OS: Windows 10 Home 64-bit, version 1909
* JDK: openJDK 15.0.1

### Passed arguments for running chatserver are:

"java -jar target/chatserver-1.0-SNAPSHOT-jar-with-dependencies.jar ..." followed by:

1. chatdatabase.db (or any other to one's liking)
2. name of keystore
3. password to keystore
