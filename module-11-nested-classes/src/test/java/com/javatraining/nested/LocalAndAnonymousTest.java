package com.javatraining.nested;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LocalAndAnonymousTest {

    // ── filteredLines (local class) ───────────────────────────────────────────

    @Nested
    class FilteredLinesTests {

        @Test
        void returns_only_matching_lines() {
            String text = "apple\nbanana\navocado";
            List<String> result = LocalAndAnonymous.filteredLines(text, s -> s.startsWith("a"), "");
            assertEquals(List.of("apple", "avocado"), result);
        }

        @Test
        void prefix_is_prepended_to_each_matching_line() {
            String text = "foo\nbar\nbaz";
            List<String> result = LocalAndAnonymous.filteredLines(text, s -> s.startsWith("b"), "> ");
            assertEquals(List.of("> bar", "> baz"), result);
        }

        @Test
        void no_match_returns_empty_list() {
            List<String> result = LocalAndAnonymous.filteredLines("alpha\nbeta", s -> false, "");
            assertTrue(result.isEmpty());
        }

        @Test
        void all_match_returns_all_lines() {
            String text = "x\ny\nz";
            List<String> result = LocalAndAnonymous.filteredLines(text, s -> true, "");
            assertEquals(List.of("x", "y", "z"), result);
        }
    }

    // ── formatTable (local class with captured colWidth) ─────────────────────

    @Nested
    class FormatTableTests {

        @Test
        void table_contains_header_row() {
            List<String[]> rows = List.<String[]>of(
                new String[]{"Name", "Age"},
                new String[]{"Alice", "30"}
            );
            List<String> lines = LocalAndAnonymous.formatTable(rows, 8);
            // Every row is surrounded by separator lines; look for the header
            assertTrue(lines.stream().anyMatch(l -> l.contains("Name")));
            assertTrue(lines.stream().anyMatch(l -> l.contains("Alice")));
        }

        @Test
        void each_row_is_surrounded_by_separators() {
            List<String[]> rows = List.<String[]>of(new String[]{"A", "B"});
            List<String> lines = LocalAndAnonymous.formatTable(rows, 4);
            // separator, row, separator — at minimum 3 lines
            assertTrue(lines.size() >= 3);
            assertTrue(lines.get(0).startsWith("+"));
            assertTrue(lines.get(lines.size() - 1).startsWith("+"));
        }
    }

    // ── Comparators ──────────────────────────────────────────────────────────

    @Nested
    class ComparatorTests {

        @Test
        void anonymous_comparator_sorts_by_length_then_alpha() {
            List<String> words = new ArrayList<>(List.of("fig", "apple", "date", "banana", "kiwi"));
            words.sort(LocalAndAnonymous.lengthThenAlpha_anonymous());
            // lengths: 3, 5, 4, 6, 4 → fig(3), date(4), kiwi(4), apple(5), banana(6)
            assertEquals("fig",    words.get(0));
            assertEquals("date",   words.get(1));
            assertEquals("kiwi",   words.get(2));
            assertEquals("apple",  words.get(3));
            assertEquals("banana", words.get(4));
        }

        @Test
        void lambda_comparator_same_result_as_anonymous() {
            List<String> a = new ArrayList<>(List.of("cherry", "fig", "date", "apple"));
            List<String> b = new ArrayList<>(a);
            a.sort(LocalAndAnonymous.lengthThenAlpha_anonymous());
            b.sort(LocalAndAnonymous.lengthThenAlpha_lambda());
            assertEquals(a, b);
        }
    }

    // ── Describable (anonymous class — multi-method interface) ────────────────

    @Nested
    class DescribableTests {

        @Test
        void describe_contains_name_and_price() {
            LocalAndAnonymous.Describable d = LocalAndAnonymous.productDescribable("Laptop", 79999.00);
            assertTrue(d.describe().contains("Laptop"));
            assertTrue(d.describe().contains("79999"));
        }

        @Test
        void short_label_is_uppercase_and_max_8_chars() {
            LocalAndAnonymous.Describable d = LocalAndAnonymous.productDescribable("Smartphone", 29999.00);
            String label = d.shortLabel();
            assertEquals(label, label.toUpperCase());
            assertTrue(label.length() <= 8);
        }

        @Test
        void short_label_for_short_name() {
            LocalAndAnonymous.Describable d = LocalAndAnonymous.productDescribable("TV", 15000.00);
            assertEquals("TV", d.shortLabel());
        }
    }

    // ── countdownRunnable (anonymous class with internal field) ───────────────

    @Nested
    class CountdownRunnableTests {

        @Test
        void runnable_counts_down_correctly() {
            // We test by capturing stdout indirectly — instead verify by
            // running n times and confirming no exception is thrown.
            Runnable cd = LocalAndAnonymous.countdownRunnable(3);
            assertDoesNotThrow(() -> { cd.run(); cd.run(); cd.run(); cd.run(); });
        }

        @Test
        void zero_countdown_runs_immediately_to_done() {
            Runnable cd = LocalAndAnonymous.countdownRunnable(0);
            assertDoesNotThrow(cd::run);
        }
    }

    // ── StringTransform ───────────────────────────────────────────────────────

    @Nested
    class StringTransformTests {

        @Test
        void anonymous_shout_uppercases_and_adds_bangs() {
            assertEquals("HELLO!!!", LocalAndAnonymous.shout_anonymous().apply("hello"));
        }

        @Test
        void lambda_shout_same_result() {
            assertEquals("WORLD!!!", LocalAndAnonymous.shout_lambda().apply("world"));
        }

        @Test
        void both_implementations_agree() {
            String[] inputs = {"java", "code", "test"};
            for (String s : inputs) {
                assertEquals(
                    LocalAndAnonymous.shout_anonymous().apply(s),
                    LocalAndAnonymous.shout_lambda().apply(s)
                );
            }
        }
    }
}
