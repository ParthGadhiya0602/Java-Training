package com.javatraining.collections;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CollectionAlgorithmsTest {

    // ── Sorting ───────────────────────────────────────────────────────────────

    @Nested
    class SortingTests {

        @Test
        void sortByLengthThenAlpha_orders_correctly() {
            List<String> result = CollectionAlgorithms.sortByLengthThenAlpha(
                List.of("fig", "apple", "kiwi", "date", "banana"));
            // lengths: 3, 5, 4, 4, 6 → fig, date, kiwi, apple, banana
            assertEquals("fig",    result.get(0));
            assertEquals("date",   result.get(1));
            assertEquals("kiwi",   result.get(2));
            assertEquals("apple",  result.get(3));
            assertEquals("banana", result.get(4));
        }

        @Test
        void sortByLengthThenAlpha_does_not_modify_original() {
            List<String> original = List.of("banana", "fig");
            CollectionAlgorithms.sortByLengthThenAlpha(original);
            assertEquals(List.of("banana", "fig"), original); // unchanged
        }
    }

    // ── Binary search ─────────────────────────────────────────────────────────

    @Nested
    class BinarySearchTests {

        List<Integer> sorted = List.of(1, 3, 5, 7, 9, 11, 13);

        @Test
        void found_element_returns_its_index() {
            assertEquals(3, CollectionAlgorithms.binarySearch(new ArrayList<>(sorted), 7));
        }

        @Test
        void absent_element_returns_minus_one() {
            assertEquals(-1, CollectionAlgorithms.binarySearch(new ArrayList<>(sorted), 6));
        }

        @Test
        void insertion_point_for_middle_value() {
            assertEquals(3, CollectionAlgorithms.insertionPoint(new ArrayList<>(sorted), 6));
        }

        @Test
        void insertion_point_before_all() {
            assertEquals(0, CollectionAlgorithms.insertionPoint(new ArrayList<>(sorted), 0));
        }

        @Test
        void insertion_point_after_all() {
            assertEquals(7, CollectionAlgorithms.insertionPoint(new ArrayList<>(sorted), 99));
        }
    }

    // ── Frequency / disjoint ─────────────────────────────────────────────────

    @Nested
    class FrequencyTests {

        @Test
        void frequency_counts_occurrences() {
            assertEquals(3, CollectionAlgorithms.frequency(List.of("a", "b", "a", "c", "a"), "a"));
        }

        @Test
        void frequency_zero_for_absent() {
            assertEquals(0, CollectionAlgorithms.frequency(List.of("x", "y"), "z"));
        }

        @Test
        void disjoint_true_for_no_overlap() {
            assertTrue(CollectionAlgorithms.disjoint(List.of(1, 2), List.of(3, 4)));
        }

        @Test
        void disjoint_false_for_overlap() {
            assertFalse(CollectionAlgorithms.disjoint(List.of(1, 2), List.of(2, 3)));
        }
    }

    // ── Unmodifiable view ─────────────────────────────────────────────────────

    @Nested
    class UnmodifiableViewTests {

        @Test
        void view_reflects_changes_to_original() {
            CollectionAlgorithms.LiveScoreboard board = new CollectionAlgorithms.LiveScoreboard();
            List<Integer> view = board.publicView();
            board.addScore(10);
            board.addScore(20);
            assertEquals(List.of(10, 20), view);
        }

        @Test
        void write_to_view_throws() {
            CollectionAlgorithms.LiveScoreboard board = new CollectionAlgorithms.LiveScoreboard();
            board.addScore(1);
            assertThrows(UnsupportedOperationException.class,
                () -> board.publicView().add(99));
        }
    }

    // ── Utility ops ───────────────────────────────────────────────────────────

    @Nested
    class UtilityTests {

        @Test
        void repeat_creates_list_of_n_copies() {
            assertEquals(List.of(0, 0, 0, 0, 0), CollectionAlgorithms.repeat(0, 5));
        }

        @Test
        void fillWith_overwrites_all_elements() {
            List<String> list = new ArrayList<>(List.of("a", "b", "c"));
            CollectionAlgorithms.fillWith(list, "x");
            assertEquals(List.of("x", "x", "x"), list);
        }

        @Test
        void rotateRight_shifts_right() {
            assertEquals(List.of(4, 5, 1, 2, 3),
                CollectionAlgorithms.rotateRight(List.of(1, 2, 3, 4, 5), 2));
        }

        @Test
        void rotateRight_does_not_modify_original() {
            List<Integer> original = List.of(1, 2, 3);
            CollectionAlgorithms.rotateRight(original, 1);
            assertEquals(List.of(1, 2, 3), original);
        }

        @Test
        void reverseInPlace_reverses_list() {
            List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 4, 5));
            CollectionAlgorithms.reverseInPlace(list);
            assertEquals(List.of(5, 4, 3, 2, 1), list);
        }

        @Test
        void deterministicShuffle_same_seed_same_result() {
            List<Integer> a = CollectionAlgorithms.deterministicShuffle(List.of(1,2,3,4,5), 42);
            List<Integer> b = CollectionAlgorithms.deterministicShuffle(List.of(1,2,3,4,5), 42);
            assertEquals(a, b);
        }

        @Test
        void deterministicShuffle_different_seeds_different_result() {
            List<Integer> a = CollectionAlgorithms.deterministicShuffle(List.of(1,2,3,4,5,6,7,8), 1);
            List<Integer> b = CollectionAlgorithms.deterministicShuffle(List.of(1,2,3,4,5,6,7,8), 2);
            assertNotEquals(a, b);
        }
    }

    // ── min / max ─────────────────────────────────────────────────────────────

    @Nested
    class MinMaxTests {

        List<CollectionAlgorithms.Employee> employees = List.of(
            new CollectionAlgorithms.Employee("Alice", "Eng", 120_000),
            new CollectionAlgorithms.Employee("Bob",   "HR",   80_000),
            new CollectionAlgorithms.Employee("Carol", "Eng", 130_000)
        );

        @Test
        void highestPaid_returns_max_salary() {
            assertEquals("Carol",
                CollectionAlgorithms.highestPaid(employees).map(CollectionAlgorithms.Employee::name).orElse("?"));
        }

        @Test
        void lowestPaid_returns_min_salary() {
            assertEquals("Bob",
                CollectionAlgorithms.lowestPaid(employees).map(CollectionAlgorithms.Employee::name).orElse("?"));
        }

        @Test
        void highestPaid_empty_returns_empty() {
            assertTrue(CollectionAlgorithms.highestPaid(List.of()).isEmpty());
        }
    }
}
