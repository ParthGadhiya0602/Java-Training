package com.javatraining.networking;

import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NioSelectorDemo")
class NioSelectorDemoTest {

    @Nested
    @DisplayName("NIO echo server")
    class NioEchoServer {
        @Test void single_client_receives_echo() throws Exception {
            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch stop  = new CountDownLatch(1);
            int port = NioSelectorDemo.startNioEchoServer(ready, stop);
            assertTrue(ready.await(2, TimeUnit.SECONDS));

            String response = NioSelectorDemo.nioSendReceive("localhost", port, "hello-nio", 2000);
            assertEquals("hello-nio", response);
            stop.countDown();
        }

        @Test void multiple_sequential_clients() throws Exception {
            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch stop  = new CountDownLatch(1);
            int port = NioSelectorDemo.startNioEchoServer(ready, stop);
            ready.await(2, TimeUnit.SECONDS);

            for (int i = 0; i < 3; i++) {
                String msg = "msg-" + i;
                String response = NioSelectorDemo.nioSendReceive("localhost", port, msg, 2000);
                assertEquals(msg, response);
            }
            stop.countDown();
        }
    }

    @Nested
    @DisplayName("Pipe round-trip")
    class PipeRoundTrip {
        @Test void pipe_preserves_message() throws Exception {
            String result = NioSelectorDemo.pipeRoundTrip("pipe-test");
            assertEquals("pipe-test", result);
        }

        @Test void pipe_handles_unicode() throws Exception {
            String result = NioSelectorDemo.pipeRoundTrip("hello world");
            assertEquals("hello world", result);
        }
    }

    @Nested
    @DisplayName("Channel utilities")
    class ChannelUtils {
        @Test void transferAll_copies_all_bytes() throws Exception {
            byte[] data = "transfer-test".getBytes();
            var src = Channels.newChannel(new ByteArrayInputStream(data));
            var baos = new ByteArrayOutputStream();
            var dst = Channels.newChannel(baos);

            long count = NioSelectorDemo.transferAll(src, dst);
            assertEquals(data.length, count);
            assertArrayEquals(data, baos.toByteArray());
        }

        @Test void readAllBytes_returns_exact_bytes() throws Exception {
            byte[] data = "read-all-test".getBytes();
            var channel = Channels.newChannel(new ByteArrayInputStream(data));
            byte[] result = NioSelectorDemo.readAllBytes(channel);
            assertArrayEquals(data, result);
        }

        @Test void transferAll_with_empty_input_returns_zero() throws Exception {
            var src = Channels.newChannel(new ByteArrayInputStream(new byte[0]));
            var baos = new ByteArrayOutputStream();
            var dst = Channels.newChannel(baos);

            long count = NioSelectorDemo.transferAll(src, dst);
            assertEquals(0, count);
        }
    }
}
