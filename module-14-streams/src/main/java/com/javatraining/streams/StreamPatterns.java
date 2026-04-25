package com.javatraining.streams;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * TOPIC: Stream patterns - flatMap, lazy evaluation, generate/iterate, pipelines
 *
 * Pattern 1 - flatMap
 *   Turns a Stream<List<T>> into a Stream<T> (one level of nesting removed).
 *   General rule: use flatMap when each element maps to ZERO OR MORE results.
 *
 * Pattern 2 - Lazy evaluation / short-circuit
 *   filter/map do no work until a terminal op fires.
 *   findFirst / anyMatch / allMatch / noneMatch stop as soon as the answer is known.
 *
 * Pattern 3 - Infinite streams via generate() and iterate()
 *   Always pair with limit() or a short-circuit terminal.
 *
 * Pattern 4 - Real-world pipelines
 *   Compose multiple operations to replace imperative nested loops.
 */
public class StreamPatterns {

    // ── shared domain model ──────────────────────────────────────────────────
    record Department(String name, List<String> employees) {}
    record Sentence(String text) {
        List<String> words() { return Arrays.asList(text.split("\\s+")); }
    }
    record Invoice(String customer, List<LineItem> items) {}
    record LineItem(String product, int qty, double unitPrice) {
        double total() { return qty * unitPrice; }
    }

    // -------------------------------------------------------------------------
    // Pattern 1 - flatMap
    // -------------------------------------------------------------------------

    /** Returns all employees from every department as a flat list. */
    static List<String> allEmployees(List<Department> departments) {
        return departments.stream()
            .flatMap(d -> d.employees().stream())
            .toList();
    }

    /** Returns all unique words across all sentences (lowercased). */
    static Set<String> uniqueWords(List<Sentence> sentences) {
        return sentences.stream()
            .flatMap(s -> s.words().stream())
            .map(String::toLowerCase)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Returns all (customer, product) pairs across all invoices.
     * flatMap turns List<Invoice> → Stream<(customer, product)> pairs.
     */
    static List<String> customerProductPairs(List<Invoice> invoices) {
        return invoices.stream()
            .flatMap(inv -> inv.items().stream()
                .map(item -> inv.customer() + " → " + item.product()))
            .toList();
    }

    /** Splits each CSV line into tokens and flattens them into one list. */
    static List<String> flattenCsv(List<String> csvLines) {
        return csvLines.stream()
            .flatMap(line -> Arrays.stream(line.split(",")))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    // -------------------------------------------------------------------------
    // Pattern 2 - peek (debugging without disrupting the pipeline)
    //   peek() is an intermediate op that consumes each element but passes it through.
    //   Use it to log intermediate state.  Never put side effects you depend on
    //   in peek() - the stream may skip elements via short-circuit.
    // -------------------------------------------------------------------------

    static List<String> filteredAndLogged(List<String> words, String prefix) {
        List<String> log = new ArrayList<>();
        List<String> result = words.stream()
            .peek(w -> log.add("before-filter: " + w))
            .filter(w -> w.startsWith(prefix))
            .peek(w -> log.add("after-filter:  " + w))
            .map(String::toUpperCase)
            .toList();
        return result;   // log is side-channel; not returned (used in tests via the list)
    }

    // -------------------------------------------------------------------------
    // Pattern 3 - Infinite streams
    // -------------------------------------------------------------------------

    /** Returns the first n prime numbers. */
    static List<Integer> primes(int n) {
        return Stream.iterate(2, i -> i + 1)
            .filter(StreamPatterns::isPrime)
            .limit(n)
            .toList();
    }

    private static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) if (n % i == 0) return false;
        return true;
    }

    /** Returns powers of 2: 1, 2, 4, 8, … up to limit n. */
    static List<Long> powersOf2(int n) {
        return Stream.iterate(1L, v -> v * 2)
            .limit(n)
            .toList();
    }

    /** Generates a deterministic sequence of n "random" ints in [0, bound). */
    static List<Integer> randomSequence(int n, int bound, long seed) {
        Random rng = new Random(seed);
        return Stream.generate(() -> rng.nextInt(bound))
            .limit(n)
            .toList();
    }

    // -------------------------------------------------------------------------
    // Pattern 4 - Real-world pipelines
    // -------------------------------------------------------------------------

    /**
     * Top-N customers by total invoice amount.
     * Pipeline: flatten items, group by customer, sum totals, sort desc, take N.
     */
    static List<String> topCustomers(List<Invoice> invoices, int n) {
        return invoices.stream()
            .collect(Collectors.toMap(
                Invoice::customer,
                inv -> inv.items().stream().mapToDouble(LineItem::total).sum(),
                Double::sum))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(n)
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Word frequency: returns a map of word → count, sorted by frequency desc.
     * Pipeline: split → lowercase → count → sort.
     */
    static List<Map.Entry<String, Long>> wordFrequency(List<String> sentences) {
        return sentences.stream()
            .flatMap(s -> Arrays.stream(s.split("\\W+")))
            .map(String::toLowerCase)
            .filter(w -> !w.isEmpty())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .toList();
    }

    /**
     * Run-length encoding: "aaabbbccca" → [(a,3),(b,3),(c,3),(a,1)]
     * Uses Stream.iterate with a stateful approach via reduce.
     */
    static List<Map.Entry<Character, Integer>> runLengthEncode(String s) {
        if (s.isEmpty()) return List.of();
        List<Map.Entry<Character, Integer>> result = new ArrayList<>();
        char[] chars = s.toCharArray();
        char   cur   = chars[0];
        int    cnt   = 1;
        for (int i = 1; i < chars.length; i++) {
            if (chars[i] == cur) {
                cnt++;
            } else {
                result.add(Map.entry(cur, cnt));
                cur = chars[i];
                cnt = 1;
            }
        }
        result.add(Map.entry(cur, cnt));
        return result;
    }

    /**
     * Matrix transpose: rows become columns.
     * Uses IntStream.range to build each new row by column index.
     */
    static List<List<Integer>> transpose(List<List<Integer>> matrix) {
        if (matrix.isEmpty()) return List.of();
        int rows = matrix.size();
        int cols = matrix.get(0).size();
        return IntStream.range(0, cols)
            .mapToObj(col -> IntStream.range(0, rows)
                .mapToObj(row -> matrix.get(row).get(col))
                .toList())
            .toList();
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void flatMapDemo() {
        System.out.println("=== flatMap ===");
        List<Department> depts = List.of(
            new Department("Eng", List.of("Alice", "Bob", "Carol")),
            new Department("HR",  List.of("Dave", "Eve")),
            new Department("Finance", List.of())
        );
        System.out.println("all employees: " + allEmployees(depts));

        List<Sentence> sentences = List.of(
            new Sentence("the quick brown fox"),
            new Sentence("the fox jumps high")
        );
        System.out.println("unique words:  " + uniqueWords(sentences));

        List<String> csv = List.of("a,b,c", "d, e, f", "g");
        System.out.println("flatten CSV:   " + flattenCsv(csv));
    }

    static void infiniteDemo() {
        System.out.println("\n=== Infinite streams ===");
        System.out.println("primes(10):    " + primes(10));
        System.out.println("powers2(8):    " + powersOf2(8));
        System.out.println("random(5,100,42): " + randomSequence(5, 100, 42));
    }

    static void pipelineDemo() {
        System.out.println("\n=== Real-world pipelines ===");
        List<Invoice> invoices = List.of(
            new Invoice("Alice", List.of(new LineItem("Laptop",  1, 75000),
                                         new LineItem("Mouse",   2,   500))),
            new Invoice("Bob",   List.of(new LineItem("Phone",   1, 25000))),
            new Invoice("Alice", List.of(new LineItem("Headphones", 1, 3500)))
        );
        System.out.println("top customers: " + topCustomers(invoices, 2));

        List<String> text = List.of("the cat sat on the mat", "the cat is fat");
        System.out.println("word freq:     " + wordFrequency(text).subList(0,3));

        System.out.println("RLE(aaabbbccca): " + runLengthEncode("aaabbbccca"));

        List<List<Integer>> matrix = List.of(
            List.of(1, 2, 3),
            List.of(4, 5, 6)
        );
        System.out.println("transpose:     " + transpose(matrix));
    }

    public static void main(String[] args) {
        flatMapDemo();
        infiniteDemo();
        pipelineDemo();
    }
}
