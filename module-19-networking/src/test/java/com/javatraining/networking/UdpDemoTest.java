package com.javatraining.networking;

import org.junit.jupiter.api.*;

import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UdpDemo")
class UdpDemoTest {

    @Nested
    @DisplayName("Echo server")
    class EchoServer {
        @Test void client_receives_echoed_datagram() throws Exception {
            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch done  = new CountDownLatch(1);
            int port = UdpDemo.startUdpEchoServer(ready, done);
            assertTrue(ready.await(2, TimeUnit.SECONDS));

            String response = UdpDemo.sendUdp("localhost", port, "hello", 2000);
            assertTrue(done.await(2, TimeUnit.SECONDS));
            assertEquals("ECHO: hello", response);
        }

        @Test void echo_preserves_content() throws Exception {
            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch done  = new CountDownLatch(1);
            int port = UdpDemo.startUdpEchoServer(ready, done);
            ready.await(2, TimeUnit.SECONDS);

            String response = UdpDemo.sendUdp("localhost", port, "UDP rocks", 2000);
            done.await(2, TimeUnit.SECONDS);
            assertEquals("ECHO: UDP rocks", response);
        }
    }

    @Nested
    @DisplayName("Connected UDP")
    class ConnectedUdp {
        @Test void connected_roundtrip_returns_echo() throws Exception {
            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch done  = new CountDownLatch(1);
            int port = UdpDemo.startUdpEchoServer(ready, done);
            ready.await(2, TimeUnit.SECONDS);

            String response = UdpDemo.connectedUdpRoundTrip("localhost", port, "ping", 2000);
            done.await(2, TimeUnit.SECONDS);
            assertEquals("ECHO: ping", response);
        }
    }

    @Nested
    @DisplayName("Multi-packet server")
    class MultiPacket {
        @Test void handles_three_sequential_datagrams() throws Exception {
            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch done  = new CountDownLatch(1);
            int port = UdpDemo.startMultiPacketServer(3, ready, done);
            assertTrue(ready.await(2, TimeUnit.SECONDS));

            for (int i = 0; i < 3; i++) {
                String response = UdpDemo.sendUdp("localhost", port, "pkt-" + i, 2000);
                assertEquals("ECHO: pkt-" + i, response);
            }
            assertTrue(done.await(2, TimeUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("Datagram utilities")
    class DatagramUtils {
        @Test void makePacket_sets_correct_address_and_port() throws Exception {
            InetAddress addr = InetAddress.getByName("localhost");
            var packet = UdpDemo.makePacket("hello", addr, 9999);
            assertEquals(addr, packet.getAddress());
            assertEquals(9999, packet.getPort());
        }

        @Test void extractPayload_round_trips_string() throws Exception {
            InetAddress addr = InetAddress.getByName("localhost");
            var packet = UdpDemo.makePacket("round-trip test", addr, 1234);
            assertEquals("round-trip test", UdpDemo.extractPayload(packet));
        }
    }
}
