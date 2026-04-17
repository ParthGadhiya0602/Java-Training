package com.javatraining.networking;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TcpDemo")
class TcpDemoTest {

    @Nested
    @DisplayName("Echo server")
    class EchoServer {
        @Test void client_receives_echoed_message() throws Exception {
            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch done  = new CountDownLatch(1);
            int port = TcpDemo.startEchoServer(ready, done);
            assertTrue(ready.await(2, TimeUnit.SECONDS));

            String response = TcpDemo.sendAndReceive("localhost", port, "hello");
            assertTrue(done.await(2, TimeUnit.SECONDS));
            assertEquals("ECHO: hello", response);
        }

        @Test void echo_preserves_content() throws Exception {
            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch done  = new CountDownLatch(1);
            int port = TcpDemo.startEchoServer(ready, done);
            ready.await(2, TimeUnit.SECONDS);

            String response = TcpDemo.sendAndReceive("localhost", port,
                                                     "Java networking 2024");
            done.await(2, TimeUnit.SECONDS);
            assertEquals("ECHO: Java networking 2024", response);
        }
    }

    @Nested
    @DisplayName("Concurrent server")
    class ConcurrentServer {
        @Test void handles_multiple_clients_sequentially() throws Exception {
            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch stop  = new CountDownLatch(1);
            int port = TcpDemo.startConcurrentServer(ready, stop);
            assertTrue(ready.await(2, TimeUnit.SECONDS));

            for (int i = 0; i < 3; i++) {
                String response = TcpDemo.sendAndReceive("localhost", port, "msg-" + i);
                assertEquals("ECHO: msg-" + i, response);
            }
            stop.countDown();
        }
    }

    @Nested
    @DisplayName("Framed messages")
    class Framed {
        @Test void framed_roundtrip_via_server() throws Exception {
            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch done  = new CountDownLatch(1);
            int port = TcpDemo.startFramedServer(ready, done);
            assertTrue(ready.await(2, TimeUnit.SECONDS));

            try (Socket socket = new Socket("localhost", port)) {
                socket.setSoTimeout(2000);
                TcpDemo.writeFramed(socket.getOutputStream(), "framed-msg");
                String reply = TcpDemo.readFramed(socket.getInputStream());
                assertEquals("ECHO: framed-msg", reply);
            }
            assertTrue(done.await(2, TimeUnit.SECONDS));
        }

        @Test void writeFramed_readFramed_in_memory_roundtrip() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TcpDemo.writeFramed(baos, "test message");
            String result = TcpDemo.readFramed(
                new ByteArrayInputStream(baos.toByteArray()));
            assertEquals("test message", result);
        }
    }

    @Nested
    @DisplayName("InetAddress utilities")
    class InetUtils {
        @Test void normalise_loopback_address() throws UnknownHostException {
            String normalised = TcpDemo.normaliseAddress("127.0.0.1");
            assertEquals("127.0.0.1", normalised);
        }

        @Test void localHostName_is_not_blank() throws UnknownHostException {
            assertFalse(TcpDemo.localHostName().isBlank());
        }
    }

    @Nested
    @DisplayName("Socket options")
    class SocketOptions {
        @Test void inspect_returns_expected_keys() throws Exception {
            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch stop  = new CountDownLatch(1);
            int port = TcpDemo.startConcurrentServer(ready, stop);
            ready.await(2, TimeUnit.SECONDS);

            try (Socket socket = new Socket("localhost", port)) {
                var opts = TcpDemo.inspectSocketOptions(socket);
                assertTrue(opts.containsKey("soTimeout"));
                assertTrue(opts.containsKey("tcpNoDelay"));
                assertTrue(opts.containsKey("receiveBuffer"));
            }
            stop.countDown();
        }
    }
}
