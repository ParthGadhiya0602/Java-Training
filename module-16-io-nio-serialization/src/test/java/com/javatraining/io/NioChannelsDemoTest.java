package com.javatraining.io;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NioChannelsDemo")
class NioChannelsDemoTest {

    @TempDir Path tempDir;

    @Nested
    @DisplayName("ByteBuffer lifecycle")
    class BufferLifecycle {
        @Test void bufferRoundTrip_preserves_bytes() {
            byte[] input = "hello nio".getBytes(StandardCharsets.UTF_8);
            byte[] output = NioChannelsDemo.bufferRoundTrip(input);
            assertArrayEquals(input, output);
        }

        @Test void bufferRoundTrip_all_byte_values() {
            byte[] input = new byte[256];
            for (int i = 0; i < 256; i++) input[i] = (byte) i;
            assertArrayEquals(input, NioChannelsDemo.bufferRoundTrip(input));
        }

        @Test void allocateDirect_creates_direct_buffer() {
            ByteBuffer buf = NioChannelsDemo.allocateDirect(1024);
            assertTrue(buf.isDirect());
            assertEquals(1024, buf.capacity());
        }
    }

    @Nested
    @DisplayName("FileChannel read/write")
    class ChannelReadWrite {
        @Test void writeWithChannel_readWithChannel_roundtrip() throws IOException {
            Path f = tempDir.resolve("channel.bin");
            byte[] data = "FileChannel data".getBytes(StandardCharsets.UTF_8);
            NioChannelsDemo.writeWithChannel(f, data);
            assertArrayEquals(data, NioChannelsDemo.readWithChannel(f));
        }

        @Test void writeWithChannel_overwrites_existing() throws IOException {
            Path f = tempDir.resolve("overwrite.bin");
            NioChannelsDemo.writeWithChannel(f, "first".getBytes(StandardCharsets.UTF_8));
            NioChannelsDemo.writeWithChannel(f, "second".getBytes(StandardCharsets.UTF_8));
            assertArrayEquals("second".getBytes(StandardCharsets.UTF_8),
                              NioChannelsDemo.readWithChannel(f));
        }
    }

    @Nested
    @DisplayName("Zero-copy transfer")
    class ZeroCopy {
        @Test void copyWithTransfer_copies_content() throws IOException {
            Path src = tempDir.resolve("src.bin");
            Path dst = tempDir.resolve("dst.bin");
            byte[] data = "transfer me".getBytes(StandardCharsets.UTF_8);
            NioChannelsDemo.writeWithChannel(src, data);
            NioChannelsDemo.copyWithTransfer(src, dst);
            assertArrayEquals(data, NioChannelsDemo.readWithChannel(dst));
        }

        @Test void copyWithTransfer_works_for_large_data() throws IOException {
            Path src = tempDir.resolve("large.bin");
            Path dst = tempDir.resolve("large_copy.bin");
            byte[] data = new byte[1024 * 1024]; // 1 MB
            for (int i = 0; i < data.length; i++) data[i] = (byte)(i % 127);
            NioChannelsDemo.writeWithChannel(src, data);
            NioChannelsDemo.copyWithTransfer(src, dst);
            assertArrayEquals(data, NioChannelsDemo.readWithChannel(dst));
        }
    }

    @Nested
    @DisplayName("Memory-mapped files")
    class MemoryMapped {
        @Test void readMapped_reads_content() throws IOException {
            Path f = tempDir.resolve("mapped.txt");
            Files.writeString(f, "mapped content", StandardCharsets.UTF_8);
            assertEquals("mapped content", NioChannelsDemo.readMapped(f));
        }
    }

    @Nested
    @DisplayName("Pipe")
    class PipeTest {
        @Test void processThroughPipe_preserves_lines() throws IOException, InterruptedException {
            List<String> lines = List.of("line1", "line2", "line3");
            List<String> result = NioChannelsDemo.processThroughPipe(lines);
            assertEquals(lines, result);
        }

        @Test void processThroughPipe_empty_input() throws IOException, InterruptedException {
            assertTrue(NioChannelsDemo.processThroughPipe(List.of()).isEmpty());
        }
    }
}
