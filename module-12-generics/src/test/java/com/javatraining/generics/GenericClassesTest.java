package com.javatraining.generics;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GenericClassesTest {

    // ── Pair ──────────────────────────────────────────────────────────────────

    @Nested
    class PairTests {

        @Test
        void stores_first_and_second() {
            GenericClasses.Pair<String, Integer> p = new GenericClasses.Pair<>("Alice", 42);
            assertEquals("Alice", p.first());
            assertEquals(42,      p.second());
        }

        @Test
        void swap_reverses_elements() {
            GenericClasses.Pair<String, Integer> p  = new GenericClasses.Pair<>("hi", 7);
            GenericClasses.Pair<Integer, String> sw = p.swap();
            assertEquals(7,    sw.first());
            assertEquals("hi", sw.second());
        }

        @Test
        void map_transforms_both_elements() {
            GenericClasses.Pair<String, Integer> p = new GenericClasses.Pair<>("hello", 3);
            GenericClasses.Pair<Integer, String> m = p.map(String::length, Object::toString);
            assertEquals(5, m.first());
            assertEquals("3", m.second());
        }

        @Test
        void equals_and_hashCode_by_value() {
            var a = new GenericClasses.Pair<>("x", 1);
            var b = new GenericClasses.Pair<>("x", 1);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void null_first_throws() {
            assertThrows(NullPointerException.class,
                () -> new GenericClasses.Pair<>(null, 1));
        }
    }

    // ── generic methods ───────────────────────────────────────────────────────

    @Nested
    class GenericMethodTests {

        @Test
        void first_returns_first_element() {
            assertEquals("a", GenericClasses.first(new String[]{"a", "b", "c"}));
        }

        @Test
        void first_on_empty_throws() {
            assertThrows(NoSuchElementException.class,
                () -> GenericClasses.first(new Integer[0]));
        }

        @Test
        void reversed_returns_reversed_list() {
            assertEquals(List.of(3, 2, 1), GenericClasses.reversed(List.of(1, 2, 3)));
        }

        @Test
        void reversed_original_unchanged() {
            List<Integer> original = List.of(1, 2, 3);
            GenericClasses.reversed(original);
            assertEquals(List.of(1, 2, 3), original);
        }

        @Test
        void zip_pairs_elements() {
            List<String>  names  = List.of("Alice", "Bob");
            List<Integer> scores = List.of(90, 80);
            var result = GenericClasses.zip(names, scores);
            assertEquals(new GenericClasses.Pair<>("Alice", 90), result.get(0));
            assertEquals(new GenericClasses.Pair<>("Bob",   80), result.get(1));
        }

        @Test
        void zip_stops_at_shorter_list() {
            var result = GenericClasses.zip(List.of(1, 2, 3), List.of("a", "b"));
            assertEquals(2, result.size());
        }
    }

    // ── bounded type parameter ────────────────────────────────────────────────

    @Nested
    class BoundedTypeTests {

        @Test
        void max_integers() {
            assertEquals(9, GenericClasses.max(3, 9));
        }

        @Test
        void max_strings() {
            assertEquals("mango", GenericClasses.max("apple", "mango"));
        }

        @Test
        void min_of_list() {
            assertEquals(1, GenericClasses.min(List.of(5, 1, 9, 3)));
        }

        @Test
        void min_empty_throws() {
            assertThrows(NoSuchElementException.class,
                () -> GenericClasses.min(List.<Integer>of()));
        }

        @Test
        void range_returns_lo_and_hi() {
            var r = GenericClasses.range(List.of(5, 1, 9, 3));
            assertEquals(1, r.first());
            assertEquals(9, r.second());
        }

        @Test
        void sum_integers() {
            assertEquals(15.0, GenericClasses.sum(List.of(1, 2, 3, 4, 5)));
        }

        @Test
        void sum_doubles() {
            assertEquals(6.0, GenericClasses.sum(List.of(1.5, 2.5, 2.0)));
        }

        @Test
        void median_odd_size() {
            assertEquals(3.0, GenericClasses.median(List.of(1, 2, 3, 4, 5)));
        }

        @Test
        void median_even_size() {
            assertEquals(2.5, GenericClasses.median(List.of(1, 2, 3, 4)));
        }
    }

    // ── SortedBag ─────────────────────────────────────────────────────────────

    @Nested
    class SortedBagTests {

        GenericClasses.SortedBag<Integer> bag;

        @BeforeEach
        void setUp() {
            bag = new GenericClasses.SortedBag<>();
            bag.add(5); bag.add(2); bag.add(8); bag.add(1);
        }

        @Test
        void elements_are_sorted() {
            assertEquals(List.of(1, 2, 5, 8), bag.toList());
        }

        @Test
        void min_returns_smallest() { assertEquals(1, bag.min()); }

        @Test
        void max_returns_largest()  { assertEquals(8, bag.max()); }

        @Test
        void contains_present_element() { assertTrue(bag.contains(5)); }

        @Test
        void contains_absent_element()  { assertFalse(bag.contains(7)); }

        @Test
        void remove_deletes_one_occurrence() {
            bag.add(5);   // now two 5s
            bag.remove(5);
            assertTrue(bag.contains(5));   // one 5 still present
            assertEquals(4, bag.size());
        }

        @Test
        void empty_bag_min_is_null() {
            assertNull(new GenericClasses.SortedBag<Integer>().min());
        }
    }
}
