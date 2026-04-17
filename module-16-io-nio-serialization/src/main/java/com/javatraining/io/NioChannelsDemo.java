package com.javatraining.io;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Module 16 — NIO Channels and Buffers
 *
 * NIO (java.nio) introduced a different I/O model built on:
 *
 *   Buffer   — fixed-capacity container for primitive data
 *   Channel  — bidirectional, interruptible connection to I/O resource
 *   Selector — single thread multiplexes many channels (non-blocking I/O)
 *
 * Buffer state machine:
 *   write into buffer → flip() → read from buffer → clear() / compact()
 *
 *   After writes:  position = bytes written,  limit = capacity
 *   After flip():  position = 0,              limit = bytes written
 *   After clear(): position = 0,              limit = capacity  (discard unread)
 *   After compact(): unread bytes moved to front; position = after them
 *
 * FileChannel is the most common channel in server-side code.
 * It supports memory-mapped files and zero-copy transferTo/transferFrom.
 */
public class NioChannelsDemo {

    // ── ByteBuffer basics ─────────────────────────────────────────────────────

    /** Demonstrate the write → flip → read lifecycle. */
    public static byte[] bufferRoundTrip(byte[] input) {
        ByteBuffer buf = ByteBuffer.allocate(input.length * 2);

        // Write phase
        buf.put(input);

        // Switch to read phase
        buf.flip();

        // Read phase
        byte[] output = new byte[buf.remaining()];
        buf.get(output);
        return output;
    }

    /**
     * Direct buffers are allocated outside the JVM heap — OS can DMA directly.
     * Faster for large I/O; slower to allocate; never garbage-collected promptly.
     * Use for long-lived, large-transfer buffers only.
     */
    public static ByteBuffer allocateDirect(int capacity) {
        return ByteBuffer.allocateDirect(capacity);
    }

    /** Demonstrate buffer slice — a view sharing the backing array. */
    public static ByteBuffer slice(ByteBuffer original, int offset, int length) {
        original.position(offset).limit(offset + length);
        return original.slice();
    }

    // ── FileChannel read / write ──────────────────────────────────────────────

    /** Write bytes to a file via FileChannel. */
    public static void writeWithChannel(Path path, byte[] data) throws IOException {
        try (FileChannel fc = FileChannel.open(path,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer buf = ByteBuffer.wrap(data);
            while (buf.hasRemaining()) {
                fc.write(buf);
            }
        }
    }

    /** Read all bytes from a file via FileChannel. */
    public static byte[] readWithChannel(Path path) throws IOException {
        try (FileChannel fc = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteBuffer buf = ByteBuffer.allocate(4096);
            while (fc.read(buf) != -1) {
                buf.flip();
                byte[] chunk = new byte[buf.remaining()];
                buf.get(chunk);
                baos.write(chunk);
                buf.clear();
            }
            return baos.toByteArray();
        }
    }

    // ── Zero-copy transfer ────────────────────────────────────────────────────

    /**
     * transferTo delegates to the OS sendfile(2) syscall on Linux — the data
     * never enters userspace, giving significant throughput gains for large files.
     */
    public static void copyWithTransfer(Path source, Path target) throws IOException {
        try (FileChannel src = FileChannel.open(source, StandardOpenOption.READ);
             FileChannel dst = FileChannel.open(target,
                     StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {
            long size = src.size();
            long transferred = 0;
            while (transferred < size) {
                transferred += src.transferTo(transferred, size - transferred, dst);
            }
        }
    }

    // ── Memory-mapped files ───────────────────────────────────────────────────

    /**
     * A memory-mapped file is mapped directly into the process address space.
     * Reads/writes go through the OS page cache — no explicit read() calls needed.
     * Ideal for random access to large files.
     */
    public static String readMapped(Path path) throws IOException {
        try (FileChannel fc = FileChannel.open(path, StandardOpenOption.READ)) {
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            byte[] bytes = new byte[mbb.remaining()];
            mbb.get(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    // ── Scatter / Gather I/O ─────────────────────────────────────────────────

    /**
     * Scatter read: fill multiple buffers in one syscall.
     * Gather write: drain multiple buffers in one syscall.
     * Useful for fixed-header + variable-body protocols.
     */
    public static void scatterGatherCopy(Path source, Path target) throws IOException {
        try (FileChannel src = FileChannel.open(source, StandardOpenOption.READ);
             FileChannel dst = FileChannel.open(target,
                     StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {

            // Scatter into two buffers (e.g. header + body split)
            ByteBuffer header = ByteBuffer.allocate(64);
            ByteBuffer body   = ByteBuffer.allocate((int) Math.max(0, src.size() - 64));
            src.read(new ByteBuffer[]{header, body});

            // Gather back out
            header.flip();
            body.flip();
            dst.write(new ByteBuffer[]{header, body});
        }
    }

    // ── Pipe ─────────────────────────────────────────────────────────────────

    /**
     * Pipe connects two threads: one writes to the sink, one reads from the source.
     * Useful for in-process producer/consumer without temp files.
     */
    public static List<String> processThroughPipe(List<String> lines) throws IOException, InterruptedException {
        Pipe pipe = Pipe.open();
        List<String> result = new ArrayList<>();

        // Producer thread writes lines to pipe sink
        Thread producer = Thread.ofVirtual().start(() -> {
            try (WritableByteChannel sink = pipe.sink()) {
                for (String line : lines) {
                    byte[] bytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
                    ByteBuffer buf = ByteBuffer.wrap(bytes);
                    while (buf.hasRemaining()) {
                        sink.write(buf);
                    }
                }
            } catch (IOException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Consumer: read from pipe source until closed
        try (ReadableByteChannel source = pipe.source()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteBuffer buf = ByteBuffer.allocate(256);
            while (source.read(buf) != -1) {
                buf.flip();
                byte[] chunk = new byte[buf.remaining()];
                buf.get(chunk);
                baos.write(chunk);
                buf.clear();
            }
            // Split on newlines
            for (String line : baos.toString(StandardCharsets.UTF_8).split("\n")) {
                if (!line.isEmpty()) result.add(line);
            }
        }

        producer.join();
        return result;
    }
}
