package com.javatraining.networking;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Module 19 - Non-blocking I/O with Selectors
 *
 * NIO non-blocking channels allow a single thread to multiplex many connections:
 *
 *   ServerSocketChannel - non-blocking analogue of ServerSocket
 *   SocketChannel       - non-blocking analogue of Socket
 *   Selector            - monitors multiple channels for readiness events
 *   SelectionKey        - represents a channel registered with a Selector
 *
 * SelectionKey interest ops:
 *   OP_ACCEPT   - ServerSocketChannel has a pending connection
 *   OP_CONNECT  - SocketChannel has finished connecting
 *   OP_READ     - channel has data to read
 *   OP_WRITE    - channel has space in its send buffer
 *
 * Event loop pattern:
 *   while (running) {
 *       selector.select();       // blocks until at least one channel is ready
 *       for (SelectionKey key : selector.selectedKeys()) {
 *           if (key.isAcceptable()) { accept(key); }
 *           if (key.isReadable())   { read(key);   }
 *           selector.selectedKeys().remove(key);  // MUST remove manually
 *       }
 *   }
 *
 * When to use NIO selectors:
 *   YES: proxy servers, protocol gateways, tens of thousands of idle connections
 *   NO:  typical server-side apps - virtual threads handle this more simply
 */
public class NioSelectorDemo {

    // ── Non-blocking echo server ──────────────────────────────────────────────

    /**
     * Single-threaded NIO echo server using a Selector.
     * Handles multiple clients without blocking.
     * Runs until stopSignal is tripped.  Returns bound port.
     */
    public static int startNioEchoServer(CountDownLatch ready,
                                          CountDownLatch stopSignal) throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(0));
        int port = ((InetSocketAddress) serverChannel.getLocalAddress()).getPort();

        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        Thread.ofVirtual().start(() -> {
            ready.countDown();
            Map<SocketChannel, ByteBuffer> buffers = new HashMap<>();
            try {
                while (stopSignal.getCount() > 0) {
                    if (selector.select(100) == 0) continue; // 100ms timeout

                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();

                        if (key.isAcceptable()) {
                            SocketChannel client = serverChannel.accept();
                            if (client != null) {
                                client.configureBlocking(false);
                                client.register(selector, SelectionKey.OP_READ);
                                buffers.put(client, ByteBuffer.allocate(1024));
                            }
                        } else if (key.isReadable()) {
                            SocketChannel client = (SocketChannel) key.channel();
                            ByteBuffer buf = buffers.get(client);
                            int read = client.read(buf);
                            if (read == -1) {
                                client.close();
                                buffers.remove(client);
                            } else if (read > 0) {
                                buf.flip();
                                // Echo back immediately
                                while (buf.hasRemaining()) client.write(buf);
                                buf.clear();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // server closed
            } finally {
                try { serverChannel.close(); selector.close(); }
                catch (IOException ignored) {}
            }
        });

        return port;
    }

    // ── Non-blocking client ───────────────────────────────────────────────────

    /**
     * Connect with a non-blocking SocketChannel, then switch back to blocking
     * mode for simple request/response.  This is the common pattern for clients
     * that do async connect but synchronous I/O.
     */
    public static String nioSendReceive(String host, int port, String message,
                                         int timeoutMs) throws IOException {
        try (SocketChannel channel = SocketChannel.open()) {
            channel.configureBlocking(false);
            channel.connect(new InetSocketAddress(host, port));

            // Wait for connection to complete (with timeout)
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (!channel.finishConnect()) {
                if (System.currentTimeMillis() > deadline)
                    throw new SocketTimeoutException("connect timed out");
                Thread.yield();
            }

            // Switch to blocking for simple read/write
            channel.configureBlocking(true);
            channel.socket().setSoTimeout(timeoutMs);

            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            channel.write(ByteBuffer.wrap(data));

            ByteBuffer readBuf = ByteBuffer.allocate(1024);
            int totalRead = 0;
            long readDeadline = System.currentTimeMillis() + timeoutMs;
            while (totalRead < data.length && System.currentTimeMillis() < readDeadline) {
                int n = channel.read(readBuf);
                if (n == -1) break;
                totalRead += n;
            }
            readBuf.flip();
            return StandardCharsets.UTF_8.decode(readBuf).toString();
        }
    }

    // ── Pipe (in-process channel pair) ───────────────────────────────────────

    /**
     * Pipe.open() creates a connected pair of channels:
     *   sink   - write end
     *   source - read end
     * Useful for in-process producer/consumer without network overhead.
     */
    public static String pipeRoundTrip(String message) throws IOException {
        Pipe pipe = Pipe.open();
        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        // Write to sink
        try (WritableByteChannel sink = pipe.sink()) {
            ByteBuffer buf = ByteBuffer.wrap(data);
            while (buf.hasRemaining()) sink.write(buf);
        }

        // Read from source
        try (ReadableByteChannel source = pipe.source()) {
            ByteBuffer buf = ByteBuffer.allocate(data.length + 64);
            source.read(buf);
            buf.flip();
            return StandardCharsets.UTF_8.decode(buf).toString();
        }
    }

    // ── Channel utilities ─────────────────────────────────────────────────────

    /** Transfer all bytes from source channel to destination channel. */
    public static long transferAll(ReadableByteChannel src, WritableByteChannel dst)
            throws IOException {
        ByteBuffer buf = ByteBuffer.allocateDirect(8192);
        long total = 0;
        int read;
        while ((read = src.read(buf)) != -1) {
            buf.flip();
            while (buf.hasRemaining()) dst.write(buf);
            total += read;
            buf.clear();
        }
        return total;
    }

    /** Read all bytes from a channel into a byte array. */
    public static byte[] readAllBytes(ReadableByteChannel channel) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel dst = Channels.newChannel(baos);
        transferAll(channel, dst);
        return baos.toByteArray();
    }
}
