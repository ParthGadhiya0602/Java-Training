package com.javatraining.modernjava;

import java.util.*;
import java.util.stream.*;

/**
 * Module 22 — Modern Java API Additions (Java 9–21)
 *
 * Selected API improvements across recent Java versions:
 *
 * Java 9:
 *   List.of(), Set.of(), Map.of()  — immutable factory methods
 *   Map.copyOf(), List.copyOf()
 *   Optional.ifPresentOrElse(), Optional.stream(), Optional.or()
 *   Stream.takeWhile(), dropWhile(), iterate(seed, hasNext, next), ofNullable()
 *   String improvements: chars(), codePoints() (earlier), indent(), stripLeading/Trailing()
 *
 * Java 10:
 *   var (local variable type inference)
 *   List.copyOf(), Map.copyOf(), Set.copyOf()
 *
 * Java 11:
 *   String.isBlank(), strip(), stripLeading(), stripTrailing(), lines(), repeat()
 *   Collection.toArray(IntFunction) — toArray(String[]::new)
 *   Optional.isEmpty()
 *
 * Java 12:
 *   String.indent(), String.transform()
 *
 * Java 14:
 *   Switch expressions (final)
 *
 * Java 15:
 *   Text blocks (final)
 *
 * Java 16:
 *   Stream.toList()   — unmodifiable, shorter than collect(Collectors.toList())
 *   instanceof pattern variables (final)
 *   Records (final)
 *
 * Java 17:
 *   Sealed classes (final)
 *
 * Java 21:
 *   Virtual threads (final) — covered in Module 17
 *   Pattern matching for switch (final)
 *   Record patterns (final)
 *   Sequenced collections (SequencedCollection, SequencedMap)
 */
public class ModernApiDemo {

    // ── Immutable collections (Java 9+) ───────────────────────────────────────

    public static List<String> immutableList(String... items) {
        return List.of(items);
    }

    public static Set<Integer> immutableSet(Integer... items) {
        return Set.of(items);
    }

    public static Map<String, Integer> immutableMap(String k1, int v1,
                                                     String k2, int v2) {
        return Map.of(k1, v1, k2, v2);
    }

    /** List.copyOf() is a defensive copy that is also unmodifiable. */
    public static List<String> defensiveCopy(List<String> input) {
        return List.copyOf(input);
    }

    // ── String API (Java 11+) ─────────────────────────────────────────────────

    public static boolean isBlankLine(String s)      { return s.isBlank(); }

    /** strip() is Unicode-aware; trim() only handles ASCII whitespace. */
    public static String stripUnicode(String s)       { return s.strip(); }
    public static String stripLeading(String s)       { return s.stripLeading(); }
    public static String stripTrailing(String s)      { return s.stripTrailing(); }
    public static String repeat(String s, int times)  { return s.repeat(times); }

    /** lines() splits on \n, \r, or \r\n and returns a Stream<String>. */
    public static List<String> splitLines(String text) {
        return text.lines().collect(Collectors.toList());
    }

    /** indent() adds n spaces per line and normalises line endings. */
    public static String indentText(String text, int spaces) {
        return text.indent(spaces);
    }

    /** transform() applies a function to the string; enables fluent chaining. */
    public static String transformChain(String input) {
        return input.transform(String::strip)
                    .transform(String::toUpperCase);
    }

    // ── Optional additions (Java 9–11) ────────────────────────────────────────

    /**
     * Optional.or() returns this Optional if present, else the supplier's Optional.
     * Chains fallback sources without nested ifPresent blocks.
     */
    public static Optional<String> firstNonEmpty(Optional<String> primary,
                                                  Optional<String> fallback) {
        return primary.filter(s -> !s.isBlank()).or(() -> fallback);
    }

    /** Optional.ifPresentOrElse() — handle both branches in one call. */
    public static String describeOptional(Optional<String> opt) {
        StringBuilder sb = new StringBuilder();
        opt.ifPresentOrElse(
            v  -> sb.append("present: ").append(v),
            () -> sb.append("absent")
        );
        return sb.toString();
    }

    /** Optional.stream() — bridge Optional into Stream pipelines. */
    public static List<String> flattenOptionals(List<Optional<String>> opts) {
        return opts.stream()
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }

    /** Optional.isEmpty() (Java 11) — explicit empty check. */
    public static boolean isEmpty(Optional<?> opt) { return opt.isEmpty(); }

    // ── Stream additions (Java 9+) ────────────────────────────────────────────

    /**
     * takeWhile() — takes elements while predicate is true (stops at first false).
     * dropWhile() — drops elements while predicate is true (passes rest through).
     * Both are short-circuiting and work well with ordered streams.
     */
    public static List<Integer> takeWhileLessThan(List<Integer> list, int limit) {
        return list.stream().takeWhile(n -> n < limit).collect(Collectors.toList());
    }

    public static List<Integer> dropWhileLessThan(List<Integer> list, int limit) {
        return list.stream().dropWhile(n -> n < limit).collect(Collectors.toList());
    }

    /**
     * Stream.iterate(seed, hasNext, next) — bounded iteration without limit().
     * Generates: seed, next(seed), next(next(seed)), ... while hasNext is true.
     */
    public static List<Integer> generateRange(int start, int end) {
        return Stream.iterate(start, n -> n < end, n -> n + 1)
            .collect(Collectors.toList());
    }

    /**
     * Stream.ofNullable() — wraps null as an empty stream, non-null as a one-element stream.
     * Avoids null checks before flatMap.
     */
    public static List<String> processNullable(String value) {
        return Stream.ofNullable(value)
            .map(String::toUpperCase)
            .collect(Collectors.toList());
    }

    /** Stream.toList() (Java 16) — shorter than collect(Collectors.toList()). */
    public static List<Integer> toList(Stream<Integer> stream) {
        return stream.toList();
    }

    // ── var — local type inference (Java 10) ─────────────────────────────────

    /**
     * var infers the type from the right-hand side at compile time.
     * It does NOT make Java dynamically typed — the type is fixed at compilation.
     * Only works for local variables with an initializer.
     */
    public static Map<String, List<Integer>> groupByLength(List<String> words) {
        var result = new LinkedHashMap<String, List<Integer>>();  // var infers LinkedHashMap
        for (var word : words) {
            var key   = String.valueOf(word.length());
            var group = result.computeIfAbsent(key, k -> new ArrayList<>());
            group.add(word.length());
        }
        return result;
    }

    // ── Text blocks (Java 15+) ────────────────────────────────────────────────

    /**
     * Text blocks start with """ followed by a newline.
     * Indentation is normalised by stripping common leading whitespace.
     * Escape sequences: \n (newline), \s (trailing space), \ (line continuation).
     */
    public static String jsonTemplate(String name, int age) {
        return """
                {
                    "name": "%s",
                    "age": %d
                }
                """.formatted(name, age);
    }

    public static String htmlSnippet() {
        return """
                <html>
                    <body>
                        <p>Hello, world!</p>
                    </body>
                </html>
                """;
    }

    // ── Sequenced collections (Java 21) ──────────────────────────────────────

    /**
     * SequencedCollection adds getFirst(), getLast(), addFirst(), addLast(),
     * reversed() to List, Deque, etc.
     * SequencedMap adds firstEntry(), lastEntry(), reversed() to LinkedHashMap etc.
     */
    public static String getFirstElement(SequencedCollection<String> col) {
        return col.getFirst();
    }

    public static String getLastElement(SequencedCollection<String> col) {
        return col.getLast();
    }

    public static List<String> reversed(SequencedCollection<String> col) {
        return col.reversed().stream().collect(Collectors.toList());
    }

    public static Map.Entry<String, Integer> firstMapEntry(
            SequencedMap<String, Integer> map) {
        return map.firstEntry();
    }
}
