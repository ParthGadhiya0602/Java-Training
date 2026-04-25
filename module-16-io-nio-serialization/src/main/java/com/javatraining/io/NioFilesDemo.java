package com.javatraining.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Module 16 - NIO.2 (java.nio.file)
 *
 * NIO.2 (Java 7+) replaces most java.io.File usage.  Key types:
 *
 *   Path      - immutable representation of a file-system path
 *   Files     - static utility methods for file operations
 *   Paths     - factory for Path instances (or use Path.of() in Java 11+)
 *   FileSystem / FileSystems - access to zip, in-memory, remote FSes
 *
 * Advantages over java.io.File:
 *   - Checked exceptions (no silent failures)
 *   - Atomic operations (move, createFile)
 *   - Symlink awareness
 *   - Rich attribute access
 *   - Stream-based directory traversal
 */
public class NioFilesDemo {

    // ── Path operations ───────────────────────────────────────────────────────

    /** Demonstrate Path construction and navigation. */
    public static Path resolve(String base, String... more) {
        return Path.of(base, more);
    }

    public static Path relativize(Path parent, Path child) {
        return parent.relativize(child);
    }

    public static Path normalize(Path path) {
        return path.normalize();  // removes . and .. components
    }

    // ── Reading files ─────────────────────────────────────────────────────────

    /** Read all lines - entire file loaded into memory. */
    public static List<String> readAllLines(Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    /** Read file as a single String (Java 11+). */
    public static String readString(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /** Stream lines lazily - good for large files. Must close the stream. */
    public static long countMatchingLines(Path path, String keyword) throws IOException {
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            return lines.filter(l -> l.contains(keyword)).count();
        }
    }

    /** BufferedReader for line-by-line processing with full control. */
    public static List<String> readNonBlankLines(Path path) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return br.lines()
                     .map(String::strip)
                     .filter(l -> !l.isEmpty())
                     .collect(Collectors.toList());
        }
    }

    // ── Writing files ─────────────────────────────────────────────────────────

    /** Write lines - creates or overwrites. */
    public static void writeLines(Path path, List<String> lines) throws IOException {
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    /** Write a String (Java 11+). */
    public static void writeString(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    /** Append lines to an existing file. */
    public static void appendLines(Path path, List<String> lines) throws IOException {
        Files.write(path, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // ── File / directory management ───────────────────────────────────────────

    public static Path createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(prefix, suffix);
    }

    public static void createDirectories(Path dir) throws IOException {
        Files.createDirectories(dir);  // no-op if already exists; creates parents too
    }

    /**
     * Copy a file. REPLACE_EXISTING prevents failure if target already exists.
     * COPY_ATTRIBUTES preserves timestamps.
     */
    public static void copy(Path source, Path target) throws IOException {
        Files.copy(source, target,
                   StandardCopyOption.REPLACE_EXISTING,
                   StandardCopyOption.COPY_ATTRIBUTES);
    }

    /**
     * Move is atomic on the same filesystem - guaranteed no partial state.
     */
    public static void move(Path source, Path target) throws IOException {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void delete(Path path) throws IOException {
        Files.deleteIfExists(path);
    }

    // ── Directory walking ─────────────────────────────────────────────────────

    /**
     * Files.walk returns a lazy Stream<Path> - depth-first.
     * Always use try-with-resources to close the stream and free OS handles.
     */
    public static List<Path> findByExtension(Path root, String ext) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                       .filter(p -> p.toString().endsWith(ext))
                       .sorted()
                       .collect(Collectors.toList());
        }
    }

    /** Collect directory size by summing regular file sizes. */
    public static long directorySize(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                       .mapToLong(p -> {
                           try { return Files.size(p); }
                           catch (IOException e) { return 0L; }
                       })
                       .sum();
        }
    }

    // ── File attributes ───────────────────────────────────────────────────────

    public static Map<String, Object> basicAttributes(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        return Map.of(
            "size",          attrs.size(),
            "isDirectory",   attrs.isDirectory(),
            "isRegularFile", attrs.isRegularFile(),
            "isSymbolicLink",attrs.isSymbolicLink(),
            "creationTime",  attrs.creationTime().toString(),
            "lastModified",  attrs.lastModifiedTime().toString()
        );
    }

    // ── Temporary directory with cleanup ──────────────────────────────────────

    /**
     * Pattern for tests: create a temp dir, do work, delete on exit.
     * Returns the temp dir path.
     */
    public static Path createTempDir() throws IOException {
        Path dir = Files.createTempDirectory("javatraining-");
        // Register JVM shutdown hook - fine for tests, not for production
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); }
                        catch (IOException ignored) {}
                    });
            } catch (IOException ignored) {}
        }));
        return dir;
    }
}
