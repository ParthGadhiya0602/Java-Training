package com.javatraining.networking;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

/**
 * Module 19 - UDP Sockets
 *
 * UDP (User Datagram Protocol):
 *   - Connectionless: no handshake; each packet is independent
 *   - Unreliable: no delivery guarantee, no ordering, no duplicate protection
 *   - Low overhead: useful for real-time media, DNS, game state, telemetry
 *
 * Key classes:
 *   DatagramSocket  - send/receive UDP datagrams
 *   DatagramPacket  - a single UDP datagram (data + address + port)
 *   MulticastSocket - extends DatagramSocket for group multicast
 *
 * Max payload: 65,507 bytes (65,535 - 20 IP header - 8 UDP header).
 * In practice, keep payloads under 1,472 bytes to avoid IP fragmentation
 * on standard Ethernet (MTU 1500 - 20 IP - 8 UDP = 1472).
 */
public class UdpDemo {

    // ── UDP echo server ───────────────────────────────────────────────────────

    /**
     * Receives one datagram, sends it back with "ECHO: " prefix.
     * Uses port 0 so the OS assigns a free port.
     */
    public static int startUdpEchoServer(CountDownLatch ready, CountDownLatch done)
            throws SocketException {
        DatagramSocket server = new DatagramSocket(0);
        server.setSoTimeout(3000); // don't block forever in tests
        int port = server.getLocalPort();

        Thread.ofVirtual().start(() -> {
            ready.countDown();
            try (DatagramSocket s = server) {
                byte[] buf = new byte[1024];
                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                s.receive(recv);

                String msg = new String(recv.getData(), 0, recv.getLength(),
                                        StandardCharsets.UTF_8);
                byte[] reply = ("ECHO: " + msg).getBytes(StandardCharsets.UTF_8);
                DatagramPacket send = new DatagramPacket(
                    reply, reply.length, recv.getAddress(), recv.getPort());
                s.send(send);
            } catch (IOException e) {
                // timeout or close
            } finally {
                done.countDown();
            }
        });

        return port;
    }

    // ── UDP client ────────────────────────────────────────────────────────────

    /**
     * Send a UDP datagram to host:port and wait for a reply.
     * Because UDP is connectionless, "connect()" here just sets the default
     * destination and filters incoming packets - it does not perform a handshake.
     */
    public static String sendUdp(String host, int port, String message, int timeoutMs)
            throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(timeoutMs);

            byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
            InetAddress address = InetAddress.getByName(host);
            DatagramPacket send = new DatagramPacket(sendData, sendData.length, address, port);
            socket.send(send);

            byte[] recvBuf = new byte[1024];
            DatagramPacket recv = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(recv);
            return new String(recv.getData(), 0, recv.getLength(), StandardCharsets.UTF_8);
        }
    }

    // ── Connected UDP socket ──────────────────────────────────────────────────

    /**
     * connect() on a DatagramSocket records the remote address.
     * Subsequent send() calls don't need to specify destination.
     * Packets from other sources are silently discarded.
     */
    public static String connectedUdpRoundTrip(String host, int port, String message,
                                                int timeoutMs) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName(host), port);
            socket.setSoTimeout(timeoutMs);

            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length)); // no address needed

            byte[] buf = new byte[1024];
            DatagramPacket reply = new DatagramPacket(buf, buf.length);
            socket.receive(reply);
            return new String(reply.getData(), 0, reply.getLength(), StandardCharsets.UTF_8);
        }
    }

    // ── Multi-packet server ───────────────────────────────────────────────────

    /**
     * Receives exactly `count` datagrams and echoes each one back.
     */
    public static int startMultiPacketServer(int count, CountDownLatch ready,
                                              CountDownLatch done) throws SocketException {
        DatagramSocket server = new DatagramSocket(0);
        server.setSoTimeout(3000);
        int port = server.getLocalPort();

        Thread.ofVirtual().start(() -> {
            ready.countDown();
            try (DatagramSocket s = server) {
                for (int i = 0; i < count; i++) {
                    byte[] buf = new byte[512];
                    DatagramPacket recv = new DatagramPacket(buf, buf.length);
                    s.receive(recv);
                    String msg = new String(recv.getData(), 0, recv.getLength(),
                                            StandardCharsets.UTF_8);
                    byte[] reply = ("ECHO: " + msg).getBytes(StandardCharsets.UTF_8);
                    s.send(new DatagramPacket(reply, reply.length,
                                             recv.getAddress(), recv.getPort()));
                }
            } catch (IOException e) {
                // timeout or close
            } finally {
                done.countDown();
            }
        });

        return port;
    }

    // ── Datagram utilities ────────────────────────────────────────────────────

    /** Build a DatagramPacket from a string for sending. */
    public static DatagramPacket makePacket(String message, InetAddress addr, int port) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(data, data.length, addr, port);
    }

    /** Extract the string payload from a received DatagramPacket. */
    public static String extractPayload(DatagramPacket packet) {
        return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
    }
}
