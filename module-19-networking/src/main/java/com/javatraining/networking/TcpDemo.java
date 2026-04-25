package com.javatraining.networking;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Module 19 - TCP Sockets
 *
 * TCP (Transmission Control Protocol) provides:
 *   - Reliable, ordered, error-checked byte stream
 *   - Connection-oriented: establish before data exchange
 *   - Full-duplex: both sides can send and receive simultaneously
 *
 * Key classes:
 *   ServerSocket   - listens on a port, accepts incoming connections
 *   Socket         - one end of a TCP connection (client or accepted server side)
 *   InetAddress    - IP address (v4 and v6)
 *   InetSocketAddress - IP + port combined
 *
 * Lifecycle:
 *   Server: ServerSocket(port) → accept() → getInputStream/getOutputStream → close()
 *   Client: Socket(host, port) → getInputStream/getOutputStream → close()
 *
 * Always use try-with-resources - sockets hold OS file descriptors.
 */
public class TcpDemo {

    // ── Echo server ───────────────────────────────────────────────────────────

    /**
     * Single-threaded echo server.  Binds to port 0 (OS assigns a free port).
     * Reads one line from the client, echoes it back, then closes.
     *
     * Returns the port the server bound to so tests can connect.
     */
    public static int startEchoServer(CountDownLatch ready, CountDownLatch done)
            throws IOException {
        ServerSocket server = new ServerSocket(0); // port 0 = OS-assigned
        int port = server.getLocalPort();

        Thread.ofVirtual().start(() -> {
            ready.countDown();
            try (ServerSocket ss = server;
                 Socket client = ss.accept()) {
                BufferedReader  in  = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter     out = new PrintWriter(
                    new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true);
                String line = in.readLine();
                if (line != null) out.println("ECHO: " + line);
            } catch (IOException e) {
                // server closed
            } finally {
                done.countDown();
            }
        });

        return port;
    }

    /**
     * TCP client: connect to host:port, send one message, read one response.
     */
    public static String sendAndReceive(String host, int port, String message)
            throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000); // 2s timeout
            socket.setSoTimeout(2000); // read timeout
            PrintWriter  out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out.println(message);
            return in.readLine();
        }
    }

    // ── Multi-client server ───────────────────────────────────────────────────

    /**
     * Concurrent echo server: spawns a virtual thread per accepted connection.
     * Runs until stopSignal is tripped.  Returns bound port.
     */
    public static int startConcurrentServer(CountDownLatch ready,
                                             CountDownLatch stopSignal) throws IOException {
        ServerSocket server = new ServerSocket(0);
        server.setSoTimeout(200); // accept() times out so we can check stopSignal
        int port = server.getLocalPort();

        Thread.ofVirtual().start(() -> {
            ready.countDown();
            try (ServerSocket ss = server) {
                while (stopSignal.getCount() > 0) {
                    try {
                        Socket client = ss.accept();
                        Thread.ofVirtual().start(() -> handleClient(client));
                    } catch (SocketTimeoutException ignored) {
                        // loop and recheck stopSignal
                    } catch (IOException e) {
                        break;
                    }
                }
            } catch (IOException ignored) {}
        });

        return port;
    }

    private static void handleClient(Socket socket) {
        try (Socket s = socket) {
            s.setSoTimeout(2000);
            BufferedReader in  = new BufferedReader(
                new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter    out = new PrintWriter(
                new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
            String line;
            while ((line = in.readLine()) != null) {
                out.println("ECHO: " + line);
            }
        } catch (IOException ignored) {}
    }

    // ── InetAddress utilities ─────────────────────────────────────────────────

    public static String localHostName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    public static boolean isReachable(String host, int timeoutMs) throws IOException {
        return InetAddress.getByName(host).isReachable(timeoutMs);
    }

    /** Parse and normalise an IP address string. */
    public static String normaliseAddress(String address) throws UnknownHostException {
        return InetAddress.getByName(address).getHostAddress();
    }

    // ── Socket options ────────────────────────────────────────────────────────

    /**
     * Key socket options:
     *   SO_TIMEOUT    - read timeout in ms (0 = block forever)
     *   SO_KEEPALIVE  - OS sends periodic probes to detect dead connections
     *   TCP_NODELAY   - disable Nagle's algorithm (send small packets immediately)
     *   SO_REUSEADDR  - allow binding to a port in TIME_WAIT state
     */
    public static Map<String, Object> inspectSocketOptions(Socket socket) throws SocketException {
        return Map.of(
            "soTimeout",    socket.getSoTimeout(),
            "keepAlive",    socket.getKeepAlive(),
            "tcpNoDelay",   socket.getTcpNoDelay(),
            "receiveBuffer",socket.getReceiveBufferSize(),
            "sendBuffer",   socket.getSendBufferSize()
        );
    }

    // ── Simple protocol: length-prefixed messages ────────────────────────────

    /**
     * Raw TCP is a byte stream with no message boundaries.
     * A common framing protocol: write the message length (4 bytes) then the bytes.
     */
    public static void writeFramed(OutputStream out, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
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
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** Start a framed-message echo server. Returns bound port. */
    public static int startFramedServer(CountDownLatch ready, CountDownLatch done)
            throws IOException {
        ServerSocket server = new ServerSocket(0);
        int port = server.getLocalPort();

        Thread.ofVirtual().start(() -> {
            ready.countDown();
            try (ServerSocket ss = server;
                 Socket client = ss.accept()) {
                client.setSoTimeout(2000);
                String msg = readFramed(client.getInputStream());
                writeFramed(client.getOutputStream(), "ECHO: " + msg);
            } catch (IOException e) {
                // ignored
            } finally {
                done.countDown();
            }
        });

        return port;
    }
}
