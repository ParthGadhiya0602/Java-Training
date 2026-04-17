package com.javatraining.io;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClassicIODemo")
class ClassicIODemoTest {

    @TempDir File tempDir;

    @Nested
    @DisplayName("Text write and read")
    class TextWriteRead {
        @Test void writeLines_then_readLines_roundtrip() throws IOException {
            File f = new File(tempDir, "test.txt");
            List<String> lines = List.of("alpha", "beta", "gamma");
            ClassicIODemo.writeLines(f, lines);
            assertEquals(lines, ClassicIODemo.readLines(f));
        }

        @Test void readAll_joins_with_line_separator() throws IOException {
            File f = new File(tempDir, "all.txt");
            ClassicIODemo.writeLines(f, List.of("line1", "line2"));
            String content = ClassicIODemo.readAll(f);
            assertTrue(content.contains("line1"));
            assertTrue(content.contains("line2"));
        }

        @Test void appendLines_adds_to_existing_file() throws IOException {
            File f = new File(tempDir, "append.txt");
            ClassicIODemo.writeLines(f, List.of("first"));
            ClassicIODemo.appendLines(f, List.of("second"));
            List<String> lines = ClassicIODemo.readLines(f);
            assertEquals(List.of("first", "second"), lines);
        }

        @Test void write_empty_list_creates_empty_file() throws IOException {
            File f = new File(tempDir, "empty.txt");
            ClassicIODemo.writeLines(f, List.of());
            assertEquals(List.of(), ClassicIODemo.readLines(f));
        }
    }

    @Nested
    @DisplayName("Byte I/O")
    class ByteIO {
        @Test void writeBytes_readBytes_roundtrip() throws IOException {
            File f = new File(tempDir, "bytes.bin");
            byte[] data = {1, 2, 3, 127, (byte) 200};
            ClassicIODemo.writeBytes(f, data);
            assertArrayEquals(data, ClassicIODemo.readBytes(f));
        }

        @Test void writeBytes_preserves_all_byte_values() throws IOException {
            File f = new File(tempDir, "allbytes.bin");
            byte[] all = new byte[256];
            for (int i = 0; i < 256; i++) all[i] = (byte) i;
            ClassicIODemo.writeBytes(f, all);
            assertArrayEquals(all, ClassicIODemo.readBytes(f));
        }
    }

    @Nested
    @DisplayName("In-memory streams")
    class InMemory {
        @Test void gatherLines_splitLines_roundtrip() throws IOException {
            List<String> original = List.of("one", "two", "three");
            byte[] data = ClassicIODemo.gatherLines(original);
            assertEquals(original, ClassicIODemo.splitLines(data));
        }
    }

    @Nested
    @DisplayName("StreamTokenizer")
    class Tokenizer {
        @Test void tokenize_counts_words_and_numbers() throws IOException {
            ClassicIODemo.TokenStats stats = ClassicIODemo.tokenize("hello world 42 foo 3.14");
            assertEquals(3, stats.words());
            assertEquals(2, stats.numbers());
        }

        @Test void tokenize_empty_string_returns_zeros() throws IOException {
            ClassicIODemo.TokenStats stats = ClassicIODemo.tokenize("");
            assertEquals(0, stats.words());
            assertEquals(0, stats.numbers());
        }
    }

    @Nested
    @DisplayName("PrintWriter report")
    class Report {
        @Test void formatReport_contains_title_and_items() throws IOException {
            String report = ClassicIODemo.formatReport("Items", List.of("apple", "banana"));
            assertTrue(report.contains("Items"));
            assertTrue(report.contains("apple"));
            assertTrue(report.contains("banana"));
            assertTrue(report.contains("Total: 2 items"));
        }

        @Test void formatReport_numbers_items() throws IOException {
            String report = ClassicIODemo.formatReport("T", List.of("x", "y", "z"));
            assertTrue(report.contains("1."));
            assertTrue(report.contains("2."));
            assertTrue(report.contains("3."));
        }
    }
}
