package com.javatraining.io;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Module 16 — Classic java.io
 *
 * The original Java I/O API is stream-based and byte-oriented at the bottom,
 * with character-based wrappers on top.  The key mental model:
 *
 *   InputStream / OutputStream  — raw bytes
 *   Reader / Writer              — characters (encode/decode automatically)
 *   Buffered* wrappers           — reduce system calls; always use them
 *
 * try-with-resources (Java 7+) guarantees close() even on exception.
 * Never use finally blocks to close I/O resources.
 */
public class ClassicIODemo {

    // ── Writing text ──────────────────────────────────────────────────────────

    /**
     * Write lines to a file using BufferedWriter.
     * try-with-resources auto-closes fw and bw even on exception.
     */
    public static void writeLines(File file, List<String> lines) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        }
    }

    /**
     * Append lines to an existing file.
     * FileWriter(file, true) — the boolean flag enables append mode.
     */
    public static void appendLines(File file, List<String> lines) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        }
    }

    // ── Reading text ──────────────────────────────────────────────────────────

    /** Read all lines from a file into a List. */
    public static List<String> readLines(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    /** Read entire file content as a single String. */
    public static String readAll(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (!first) sb.append(System.lineSeparator());
                sb.append(line);
                first = false;
            }
        }
        return sb.toString();
    }

    // ── Byte I/O ─────────────────────────────────────────────────────────────

    /** Write raw bytes to a file. */
    public static void writeBytes(File file, byte[] data) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            bos.write(data);
        }
    }

    /** Read all bytes from a file. */
    public static byte[] readBytes(File file) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            return bis.readAllBytes();
        }
    }

    // ── In-memory streams ─────────────────────────────────────────────────────

    /**
     * ByteArrayOutputStream / ByteArrayInputStream allow treating a byte[]
     * as a stream — useful for testing I/O code without touching the filesystem.
     */
    public static byte[] gatherLines(List<String> lines) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos))) {
            for (String line : lines) {
                pw.println(line);
            }
        }
        return baos.toByteArray();
    }

    public static List<String> splitLines(byte[] data) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(data)))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.add(line);
            }
        }
        return result;
    }

    // ── StreamTokenizer ───────────────────────────────────────────────────────

    /**
     * StreamTokenizer is an under-used classic API for simple parsing.
     * Here we count words vs numbers in a text.
     */
    public record TokenStats(int words, int numbers) {}

    public static TokenStats tokenize(String text) throws IOException {
        StreamTokenizer st = new StreamTokenizer(new StringReader(text));
        int words = 0, numbers = 0;
        while (st.nextToken() != StreamTokenizer.TT_EOF) {
            if (st.ttype == StreamTokenizer.TT_WORD)   words++;
            if (st.ttype == StreamTokenizer.TT_NUMBER) numbers++;
        }
        return new TokenStats(words, numbers);
    }

    // ── PrintWriter convenience ───────────────────────────────────────────────

    /**
     * PrintWriter wraps any Writer and adds printf/format support.
     * autoFlush=true flushes on println; useful for logging.
     */
    public static String formatReport(String title, List<String> items) throws IOException {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            pw.printf("=== %s ===%n", title);
            for (int i = 0; i < items.size(); i++) {
                pw.printf("%3d. %s%n", i + 1, items.get(i));
            }
            pw.printf("Total: %d items%n", items.size());
        }
        return sw.toString();
    }
}
