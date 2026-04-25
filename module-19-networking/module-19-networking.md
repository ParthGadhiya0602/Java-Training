---
title: "19 — Networking & Sockets"
parent: "Phase 2 — Core APIs"
nav_order: 19
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-19-networking/src){: .btn .btn-outline }

# Module 19 — Networking & Sockets
{: .no_toc }

<details open markdown="block">
  <summary>Table of contents</summary>
  {: .text-delta }
1. TOC
{:toc}
</details>

---

## Overview

Java's networking stack covers everything from raw TCP/UDP sockets to the modern
`HttpClient` API and NIO non-blocking channels:

| Layer | Class / API | Use when |
|---|---|---|
| TCP stream | `ServerSocket` / `Socket` | Reliable, ordered byte stream |
| UDP datagram | `DatagramSocket` / `DatagramPacket` | Low-latency, fire-and-forget |
| HTTP | `HttpClient` (Java 11+) | REST/HTTP calls |
| Non-blocking | `SocketChannel` + `Selector` | Many idle connections |

---

## TCP Sockets

TCP provides a **reliable, ordered, full-duplex byte stream** over an established
connection.

### Key classes

```java
ServerSocket server = new ServerSocket(0);   // port 0 → OS assigns free port
int port = server.getLocalPort();

Socket client = server.accept();             // blocks until connection arrives
InputStream  in  = client.getInputStream();
OutputStream out = client.getOutputStream();
```

Always use **try-with-resources** — sockets hold OS file descriptors.

### Echo server (single-threaded)

```java
public static int startEchoServer(CountDownLatch ready, CountDownLatch done)
        throws IOException {
    ServerSocket server = new ServerSocket(0);
    int port = server.getLocalPort();

    Thread.ofVirtual().start(() -> {
        ready.countDown();
        try (ServerSocket ss = server; Socket client = ss.accept()) {
            BufferedReader in  = new BufferedReader(
                new InputStreamReader(client.getInputStream(), UTF_8));
            PrintWriter    out = new PrintWriter(
                new OutputStreamWriter(client.getOutputStream(), UTF_8), true);
            String line = in.readLine();
            if (line != null) out.println("ECHO: " + line);
        } catch (IOException e) { /* closed */ }
        finally { done.countDown(); }
    });
    return port;
}
```

### Concurrent server (virtual thread per connection)

```java
public static int startConcurrentServer(CountDownLatch ready,
                                         CountDownLatch stopSignal) throws IOException {
    ServerSocket server = new ServerSocket(0);
    server.setSoTimeout(200);  // accept() times out so we can check stopSignal
    int port = server.getLocalPort();

    Thread.ofVirtual().start(() -> {
        ready.countDown();
        try (ServerSocket ss = server) {
            while (stopSignal.getCount() > 0) {
                try {
                    Socket client = ss.accept();
                    Thread.ofVirtual().start(() -> handleClient(client));
                } catch (SocketTimeoutException ignored) {
                } catch (IOException e) { break; }
            }
        } catch (IOException ignored) {}
    });
    return port;
}
```

### Framed messages (length-prefixed protocol)

Raw TCP is a byte stream with no message boundaries. A common framing protocol
writes the message length (4 bytes) then the payload:

```java
public static void writeFramed(OutputStream out, String message) throws IOException {
    byte[] bytes = message.getBytes(UTF_8);
    DataOutputStream dos = new DataOutputStream(out);
    dos.writeInt(bytes.length);
    dos.write(bytes);
    dos.flush();
}

public static String readFramed(InputStream in) throws IOException {
    DataInputStream dis = new DataInputStream(in);
    int length = dis.readInt();
    byte[] bytes = new byte[length];
    dis.readFully(bytes);
    return new String(bytes, UTF_8);
}
```

### Socket options

```java
socket.setSoTimeout(2000);       // read timeout in ms (0 = block forever)
socket.setKeepAlive(true);       // OS probes to detect dead connections
socket.setTcpNoDelay(true);      // disable Nagle — send small packets immediately
socket.setReuseAddress(true);    // bind to port in TIME_WAIT state
```

---

## UDP Sockets

UDP (User Datagram Protocol) is **connectionless and unreliable** — packets may be
dropped, reordered, or duplicated.  Useful for real-time media, DNS, game state.

Max practical payload: **1,472 bytes** (Ethernet MTU 1500 − 20 IP − 8 UDP).

### Key classes

```java
DatagramSocket socket = new DatagramSocket();  // client (random port)
DatagramSocket server = new DatagramSocket(0); // server (OS assigns port)

// Send
byte[] data = message.getBytes(UTF_8);
DatagramPacket send = new DatagramPacket(data, data.length, addr, port);
socket.send(send);

// Receive
byte[] buf = new byte[1024];
DatagramPacket recv = new DatagramPacket(buf, buf.length);
socket.receive(recv);  // blocks until packet arrives or timeout
String msg = new String(recv.getData(), 0, recv.getLength(), UTF_8);
```

### Connected UDP

`connect()` on a `DatagramSocket` records the remote address — subsequent `send()`
calls don't need to specify a destination, and packets from other sources are
silently discarded:

```java
socket.connect(InetAddress.getByName(host), port);
socket.send(new DatagramPacket(data, data.length)); // no address needed
```

---

## HttpClient (Java 11+)

`java.net.http.HttpClient` replaces `HttpURLConnection` with a modern, immutable,
fluent API supporting HTTP/1.1, HTTP/2, sync, and async requests.

### Build a shared client

```java
HttpClient client = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(5))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .version(HttpClient.Version.HTTP_1_1)
    .build();
```

Reuse instances — they manage connection pools.

### Synchronous GET

```java
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(url))
    .timeout(Duration.ofSeconds(10))
    .header("Accept", "application/json")
    .GET()
    .build();
HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
```

### Synchronous POST (JSON)

```java
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(url))
    .header("Content-Type", "application/json")
    .POST(BodyPublishers.ofString(jsonBody))
    .build();
```

### Asynchronous GET

```java
CompletableFuture<String> body = client
    .sendAsync(request, BodyHandlers.ofString())
    .thenApply(HttpResponse::body);
```

### Fan-out (N concurrent requests)

```java
List<CompletableFuture<String>> futures = urls.stream()
    .map(url -> getAsync(client, url))
    .collect(toList());

List<String> results = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(toList()))
    .get();
```

### URI vs URL

- **URI** — identifies a resource (may be abstract, relative, or opaque)
- **URL** — a URI that also includes how to locate it (scheme + authority + path)

Prefer `URI` in APIs; use `url.toURI()` when interoperating with legacy code.

---

## NIO Non-Blocking Channels + Selector

NIO non-blocking channels allow a **single thread** to multiplex many connections.

### Core types

| Type | Purpose |
|---|---|
| `ServerSocketChannel` | Non-blocking analogue of `ServerSocket` |
| `SocketChannel` | Non-blocking analogue of `Socket` |
| `Selector` | Monitors multiple channels for readiness events |
| `SelectionKey` | Represents a channel registered with a `Selector` |

### Interest ops

```java
SelectionKey.OP_ACCEPT   // ServerSocketChannel has a pending connection
SelectionKey.OP_CONNECT  // SocketChannel has finished connecting
SelectionKey.OP_READ     // channel has data to read
SelectionKey.OP_WRITE    // channel has space in its send buffer
```

### Event loop pattern

```java
while (running) {
    selector.select();  // blocks until at least one channel is ready
    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
    while (iter.hasNext()) {
        SelectionKey key = iter.next();
        iter.remove();  // MUST remove manually
        if (key.isAcceptable()) accept(key);
        if (key.isReadable())   read(key);
    }
}
```

### NIO echo server

```java
ServerSocketChannel serverChannel = ServerSocketChannel.open();
serverChannel.configureBlocking(false);
serverChannel.bind(new InetSocketAddress(0));

Selector selector = Selector.open();
serverChannel.register(selector, SelectionKey.OP_ACCEPT);
```

### Non-blocking connect, blocking I/O

A common client pattern: async connect, then switch to blocking for simple
request/response:

```java
SocketChannel channel = SocketChannel.open();
channel.configureBlocking(false);
channel.connect(new InetSocketAddress(host, port));
while (!channel.finishConnect()) Thread.yield();   // wait for connect

channel.configureBlocking(true);  // switch to blocking for read/write
```

### Pipe (in-process channel pair)

`Pipe.open()` creates a connected pair of channels — useful for in-process
producer/consumer without network overhead:

```java
Pipe pipe = Pipe.open();
// Write to sink
pipe.sink().write(ByteBuffer.wrap(data));
// Read from source
pipe.source().read(buf);
```

### When to use NIO selectors

- **Yes:** proxy servers, protocol gateways, tens of thousands of idle connections
- **No:** typical server-side apps — virtual threads handle this more simply

---

## InetAddress utilities

```java
InetAddress.getLocalHost().getHostName()     // local machine hostname
InetAddress.getByName("127.0.0.1")          // parse / resolve address
InetAddress.getByName(host).isReachable(ms) // ICMP ping-like probe
```

---

## Summary

| Concept | Class | Key detail |
|---|---|---|
| TCP server | `ServerSocket` | `accept()` blocks; use virtual threads |
| TCP client | `Socket` | `setSoTimeout()` prevents blocking forever |
| Framing | `DataOutputStream.writeInt` | 4-byte length prefix |
| UDP | `DatagramSocket` | Connectionless; max 1472 bytes |
| HTTP | `HttpClient` | Reuse instances; prefer `sendAsync` |
| NIO | `Selector` + `SocketChannel` | Single thread, many connections |
| In-process | `Pipe` | `sink()` write / `source()` read |
