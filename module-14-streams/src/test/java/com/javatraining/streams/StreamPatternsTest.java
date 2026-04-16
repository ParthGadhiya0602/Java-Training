package com.javatraining.streams;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StreamPatternsTest {

    // ── flatMap ───────────────────────────────────────────────────────────────

    @Nested
    class FlatMapTests {

        @Test
        void allEmployees_flattens_departments() {
            List<StreamPatterns.Department> depts = List.of(
                new StreamPatterns.Department("Eng", List.of("Alice", "Bob")),
                new StreamPatterns.Department("HR",  List.of("Carol"))
            );
            List<String> result = StreamPatterns.allEmployees(depts);
            assertEquals(List.of("Alice", "Bob", "Carol"), result);
        }

        @Test
        void allEmployees_empty_department_skipped() {
            List<StreamPatterns.Department> depts = List.of(
                new StreamPatterns.Department("A", List.of("X")),
                new StreamPatterns.Department("B", List.of())
            );
            assertEquals(List.of("X"), StreamPatterns.allEmployees(depts));
        }

        @Test
        void uniqueWords_lowercased_deduped() {
            List<StreamPatterns.Sentence> sentences = List.of(
                new StreamPatterns.Sentence("The quick fox"),
                new StreamPatterns.Sentence("The fox jumps")
            );
            Set<String> words = StreamPatterns.uniqueWords(sentences);
            assertTrue(words.contains("the"));
            assertTrue(words.contains("fox"));
            // "the" and "fox" appear twice but deduped → {the, quick, fox, jumps} = 4
            assertEquals(4, words.size());
        }

        @Test
        void uniqueWords_correct_count() {
            List<StreamPatterns.Sentence> sentences = List.of(
                new StreamPatterns.Sentence("cat sat mat"),
                new StreamPatterns.Sentence("cat bat")
            );
            Set<String> words = StreamPatterns.uniqueWords(sentences);
            // cat, sat, mat, bat = 4 unique
            assertEquals(4, words.size());
        }

        @Test
        void flattenCsv_splits_and_trims() {
            List<String> result = StreamPatterns.flattenCsv(
                List.of("a,b,c", "d, e", "f"));
            assertEquals(List.of("a","b","c","d","e","f"), result);
        }

        @Test
        void flattenCsv_ignores_empty_tokens() {
            List<String> result = StreamPatterns.flattenCsv(List.of("a,,b"));
            assertEquals(List.of("a","b"), result);
        }

        @Test
        void customerProductPairs_builds_correct_pairs() {
            List<StreamPatterns.Invoice> invoices = List.of(
                new StreamPatterns.Invoice("Alice", List.of(
                    new StreamPatterns.LineItem("Laptop", 1, 1000),
                    new StreamPatterns.LineItem("Mouse",  1, 50))),
                new StreamPatterns.Invoice("Bob", List.of(
                    new StreamPatterns.LineItem("Phone", 1, 500)))
            );
            List<String> pairs = StreamPatterns.customerProductPairs(invoices);
            assertEquals(3, pairs.size());
            assertTrue(pairs.contains("Alice → Laptop"));
            assertTrue(pairs.contains("Bob → Phone"));
        }
    }

    // ── infinite streams ──────────────────────────────────────────────────────

    @Nested
    class InfiniteStreamTests {

        @Test
        void primes_first_5() {
            assertEquals(List.of(2, 3, 5, 7, 11), StreamPatterns.primes(5));
        }

        @Test
        void primes_first_10_has_10_elements() {
            assertEquals(10, StreamPatterns.primes(10).size());
        }

        @Test
        void powersOf2_first_8() {
            assertEquals(List.of(1L,2L,4L,8L,16L,32L,64L,128L),
                StreamPatterns.powersOf2(8));
        }

        @Test
        void randomSequence_deterministic() {
            List<Integer> a = StreamPatterns.randomSequence(5, 100, 42);
            List<Integer> b = StreamPatterns.randomSequence(5, 100, 42);
            assertEquals(a, b);
        }

        @Test
        void randomSequence_values_in_bound() {
            StreamPatterns.randomSequence(100, 10, 7)
                .forEach(n -> assertTrue(n >= 0 && n < 10));
        }
    }

    // ── pipelines ─────────────────────────────────────────────────────────────

    @Nested
    class PipelineTests {

        @Test
        void topCustomers_orders_by_total_desc() {
            List<StreamPatterns.Invoice> invoices = List.of(
                new StreamPatterns.Invoice("Alice", List.of(
                    new StreamPatterns.LineItem("Laptop", 1, 75000))),
                new StreamPatterns.Invoice("Bob",   List.of(
                    new StreamPatterns.LineItem("Phone",  1, 25000))),
                new StreamPatterns.Invoice("Alice", List.of(
                    new StreamPatterns.LineItem("Mouse",  1,   500)))
            );
            List<String> top = StreamPatterns.topCustomers(invoices, 2);
            assertEquals("Alice", top.get(0));   // 75500
            assertEquals("Bob",   top.get(1));   // 25000
        }

        @Test
        void wordFrequency_most_frequent_first() {
            List<String> text = List.of("the cat sat", "the cat is fat", "the fat cat");
            List<Map.Entry<String, Long>> freq = StreamPatterns.wordFrequency(text);
            // "the"=3, "cat"=3, "fat"=2, others=1
            assertEquals(3L, freq.get(0).getValue());
        }

        @Test
        void runLengthEncode_basic() {
            List<Map.Entry<Character, Integer>> result =
                StreamPatterns.runLengthEncode("aaabbbcc");
            assertEquals(3, result.size());
            assertEquals('a', result.get(0).getKey());
            assertEquals(3,   result.get(0).getValue());
            assertEquals('b', result.get(1).getKey());
            assertEquals(3,   result.get(1).getValue());
            assertEquals('c', result.get(2).getKey());
            assertEquals(2,   result.get(2).getValue());
        }

        @Test
        void runLengthEncode_single_char() {
            List<Map.Entry<Character, Integer>> result =
                StreamPatterns.runLengthEncode("x");
            assertEquals(1, result.size());
            assertEquals('x', result.get(0).getKey());
            assertEquals(1,   result.get(0).getValue());
        }

        @Test
        void runLengthEncode_empty_string() {
            assertTrue(StreamPatterns.runLengthEncode("").isEmpty());
        }

        @Test
        void transpose_2x3_becomes_3x2() {
            List<List<Integer>> matrix = List.of(
                List.of(1, 2, 3),
                List.of(4, 5, 6)
            );
            List<List<Integer>> result = StreamPatterns.transpose(matrix);
            assertEquals(3, result.size());
            assertEquals(List.of(1, 4), result.get(0));
            assertEquals(List.of(2, 5), result.get(1));
            assertEquals(List.of(3, 6), result.get(2));
        }

        @Test
        void transpose_empty_matrix() {
            assertTrue(StreamPatterns.transpose(List.of()).isEmpty());
        }
    }
}
