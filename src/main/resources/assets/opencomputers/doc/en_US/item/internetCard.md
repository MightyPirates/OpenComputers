# Internet Card

![Cat videos in 3, 2, ...](oredict:oc:internetCard)

The internet card grants [computers](../general/computer.md) access to the internet. It provides ways to perform simple HTTP requests, open plain TCP client sockets, and establish WebSocket connections for real-time communication.

## Features

- **HTTP Requests**: Perform GET, POST, and other HTTP requests to web servers
- **TCP Sockets**: Open raw TCP connections for custom protocols
- **WebSocket Support**: Establish WebSocket connections (ws:// and wss://) for real-time bidirectional communication with full SSL/TLS encryption support
- **Error Handling**: Proper handling of HTTP error responses and connection failures

## WebSocket Usage

WebSockets provide a persistent, full-duplex communication channel between the computer and a WebSocket server. This is ideal for real-time applications like chat systems, live data feeds, or interactive web applications.

Example usage:
```lua
local internet = require("internet")

-- Connect to a WebSocket server (unencrypted)
local ws = internet.websocket("ws://echo.websocket.org/")

-- Or connect to a secure WebSocket server (encrypted)
local wss = internet.websocket("wss://echo.websocket.org/")

-- Send a message
ws.send("Hello, WebSocket!")

-- Receive messages
local message = ws.receive()
if message then
  print("Received:", message)
end

-- Send binary data
ws.sendBinary("Binary data here")

-- Receive binary data
local binaryData = ws.receiveBinary()

-- Close the connection
ws.close()
```

Installing an internet card in a [computers](../general/computer.md) will also attach a custom file system that contains a few internet related applications, such as one for downloading and uploading snippets from/to pastebin as well as a `wget` clone that allows downloading data from arbitrary HTTP URLs.
