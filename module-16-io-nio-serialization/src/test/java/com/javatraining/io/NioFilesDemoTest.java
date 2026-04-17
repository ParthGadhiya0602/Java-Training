package com.javatraining.io;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NioFilesDemo")
class NioFilesDemoTest {

    @TempDir Path tempDir;

    // ── Path operations ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Path operations")
    class PathOps {
        @Test void resolve_creates_path_from_parts() {
            Path p = NioFilesDemo.resolve("/tmp", "foo", "bar.txt");
            assertTrue(p.toString().contains("foo"));
            assertTrue(p.toString().contains("bar.txt"));
        }

        @Test void normalize_removes_dots() {
            Path p = Path.of("/tmp/foo/../bar/./baz");
            Path normalized = NioFilesDemo.normalize(p);
            assertEquals(Path.of("/tmp/bar/baz"), normalized);
        }

        @Test void relativize_returns_relative_path() {
            Path parent = Path.of("/tmp/parent");
            Path child  = Path.of("/tmp/parent/sub/file.txt");
            Path rel    = NioFilesDemo.relativize(parent, child);
            assertEquals(Path.of("sub/file.txt"), rel);
        }
    }

    // ── Read / write ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Read and write")
    class ReadWrite {
        @Test void writeLines_readAllLines_roundtrip() throws IOException {
            Path f = tempDir.resolve("lines.txt");
            List<String> lines = List.of("alpha", "beta", "gamma");
            NioFilesDemo.writeLines(f, lines);
            assertEquals(lines, NioFilesDemo.readAllLines(f));
        }

        @Test void writeString_readString_roundtrip() throws IOException {
            Path f = tempDir.resolve("str.txt");
            NioFilesDemo.writeString(f, "hello world");
            assertEquals("hello world", NioFilesDemo.readString(f));
        }

        @Test void appendLines_accumulates() throws IOException {
            Path f = tempDir.resolve("append.txt");
            NioFilesDemo.writeLines(f, List.of("first"));
            NioFilesDemo.appendLines(f, List.of("second"));
            assertEquals(List.of("first", "second"), NioFilesDemo.readAllLines(f));
        }

        @Test void countMatchingLines_counts_keyword() throws IOException {
            Path f = tempDir.resolve("match.txt");
            NioFilesDemo.writeLines(f, List.of("foo bar", "baz", "foo qux", "hello"));
            assertEquals(2, NioFilesDemo.countMatchingLines(f, "foo"));
        }

        @Test void readNonBlankLines_skips_blanks() throws IOException {
            Path f = tempDir.resolve("blanks.txt");
            NioFilesDemo.writeLines(f, List.of("  line1  ", "", "  ", "line2"));
            List<String> result = NioFilesDemo.readNonBlankLines(f);
            assertEquals(List.of("line1", "line2"), result);
        }
    }

    // ── File management ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("File management")
    class FileManagement {
        @Test void createDirectories_creates_nested() throws IOException {
            Path nested = tempDir.resolve("a/b/c");
            NioFilesDemo.createDirectories(nested);
            assertTrue(Files.isDirectory(nested));
        }

        @Test void copy_creates_duplicate() throws IOException {
            Path src = tempDir.resolve("src.txt");
            Path dst = tempDir.resolve("dst.txt");
            NioFilesDemo.writeString(src, "content");
            NioFilesDemo.copy(src, dst);
            assertTrue(Files.exists(dst));
            assertEquals("content", NioFilesDemo.readString(dst));
        }

        @Test void move_relocates_file() throws IOException {
            Path src = tempDir.resolve("before.txt");
            Path dst = tempDir.resolve("after.txt");
            NioFilesDemo.writeString(src, "data");
            NioFilesDemo.move(src, dst);
            assertFalse(Files.exists(src));
            assertTrue(Files.exists(dst));
        }

        @Test void delete_removes_file() throws IOException {
            Path f = tempDir.resolve("gone.txt");
            NioFilesDemo.writeString(f, "bye");
            NioFilesDemo.delete(f);
            assertFalse(Files.exists(f));
        }

        @Test void delete_nonexistent_does_not_throw() {
            assertDoesNotThrow(() -> NioFilesDemo.delete(tempDir.resolve("nope.txt")));
        }
    }

    // ── Directory walking ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Directory walking")
    class Walking {
        @Test void findByExtension_returns_matching_files() throws IOException {
            Path sub = tempDir.resolve("sub");
            Files.createDirectories(sub);
            NioFilesDemo.writeString(tempDir.resolve("a.java"), "");
            NioFilesDemo.writeString(tempDir.resolve("b.txt"),  "");
            NioFilesDemo.writeString(sub.resolve("c.java"),     "");

            List<Path> javaFiles = NioFilesDemo.findByExtension(tempDir, ".java");
            assertEquals(2, javaFiles.size());
            assertTrue(javaFiles.stream().allMatch(p -> p.toString().endsWith(".java")));
        }

        @Test void directorySize_sums_file_sizes() throws IOException {
            NioFilesDemo.writeString(tempDir.resolve("f1.txt"), "hello");     // 5 bytes
            NioFilesDemo.writeString(tempDir.resolve("f2.txt"), "world!");    // 6 bytes
            long size = NioFilesDemo.directorySize(tempDir);
            assertTrue(size >= 11);
        }
    }

    // ── Attributes ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("File attributes")
    class Attributes {
        @Test void basicAttributes_returns_expected_keys() throws IOException {
            Path f = tempDir.resolve("attr.txt");
            NioFilesDemo.writeString(f, "data");
            Map<String, Object> attrs = NioFilesDemo.basicAttributes(f);
            assertTrue((Boolean) attrs.get("isRegularFile"));
            assertFalse((Boolean) attrs.get("isDirectory"));
            assertTrue((Long) attrs.get("size") > 0);
        }
    }
}
