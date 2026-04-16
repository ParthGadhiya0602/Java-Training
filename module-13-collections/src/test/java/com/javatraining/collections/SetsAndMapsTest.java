package com.javatraining.collections;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SetsAndMapsTest {

    // ── Set operations ────────────────────────────────────────────────────────

    @Nested
    class SetOperationTests {

        @Test
        void union_contains_all_elements() {
            Set<Integer> result = SetsAndMaps.union(Set.of(1, 2, 3), Set.of(3, 4, 5));
            assertEquals(Set.of(1, 2, 3, 4, 5), result);
        }

        @Test
        void intersection_contains_shared_elements() {
            Set<Integer> result = SetsAndMaps.intersection(Set.of(1, 2, 3), Set.of(2, 3, 4));
            assertEquals(Set.of(2, 3), result);
        }

        @Test
        void difference_contains_only_left_elements() {
            Set<Integer> result = SetsAndMaps.difference(Set.of(1, 2, 3), Set.of(2, 3, 4));
            assertEquals(Set.of(1), result);
        }

        @Test
        void disjoint_true_when_no_overlap() {
            assertTrue(SetsAndMaps.disjoint(Set.of(1, 2), Set.of(3, 4)));
        }

        @Test
        void disjoint_false_when_overlap() {
            assertFalse(SetsAndMaps.disjoint(Set.of(1, 2), Set.of(2, 3)));
        }

        @Test
        void deduplicate_preserves_first_occurrence_order() {
            List<Integer> result = SetsAndMaps.deduplicate(List.of(3, 1, 4, 1, 5, 9, 2, 6, 5, 3));
            assertEquals(List.of(3, 1, 4, 5, 9, 2, 6), result);
        }
    }

    // ── TreeSet ───────────────────────────────────────────────────────────────

    @Nested
    class TreeSetTests {

        @Test
        void rangeQuery_returns_inclusive_range() {
            NavigableSet<String> set = new TreeSet<>(
                List.of("apple", "banana", "cherry", "date", "elderberry"));
            NavigableSet<String> result = SetsAndMaps.rangeQuery(set, "banana", "date");
            assertEquals(new TreeSet<>(Set.of("banana", "cherry", "date")), result);
        }

        @Test
        void closestN_returns_closest_elements() {
            TreeSet<Integer> set = new TreeSet<>(List.of(1, 3, 5, 7, 9, 11));
            List<Integer> result = SetsAndMaps.closestN(set, 6, 3);
            // closest to 6 are: 5 (dist 1), 7 (dist 1), 3 (dist 3)
            assertEquals(3, result.size());
            assertTrue(result.contains(5));
            assertTrue(result.contains(7));
        }

        @Test
        void closestN_more_than_set_size_returns_all() {
            TreeSet<Integer> set = new TreeSet<>(List.of(1, 2));
            assertEquals(2, SetsAndMaps.closestN(set, 1, 10).size());
        }
    }

    // ── HashMap operations ────────────────────────────────────────────────────

    @Nested
    class HashMapTests {

        @Test
        void frequencies_counts_correctly() {
            Map<String, Integer> freq = SetsAndMaps.frequencies(
                List.of("a", "b", "a", "c", "b", "a"));
            assertEquals(3, freq.get("a"));
            assertEquals(2, freq.get("b"));
            assertEquals(1, freq.get("c"));
        }

        @Test
        void groupByFirstChar_groups_correctly() {
            Map<Character, List<String>> groups = SetsAndMaps.groupByFirstChar(
                List.of("apple", "avocado", "banana", "cherry", "apricot"));
            assertEquals(List.of("apple", "avocado", "apricot"), groups.get('a'));
            assertEquals(List.of("banana"), groups.get('b'));
        }

        @Test
        void groupByFirstChar_empty_string_skipped() {
            Map<Character, List<String>> groups = SetsAndMaps.groupByFirstChar(
                List.of("", "apple"));
            assertFalse(groups.containsKey('\0'));
            assertEquals(List.of("apple"), groups.get('a'));
        }
    }

    // ── TreeMap / NavigableMap ────────────────────────────────────────────────

    @Nested
    class TreeMapTests {

        TreeMap<String, Double> priceBook;

        @BeforeEach
        void setUp() {
            priceBook = new TreeMap<>();
            priceBook.put("Apple",   120.0);
            priceBook.put("Banana",   40.0);
            priceBook.put("Cherry",  350.0);
            priceBook.put("Mango",    80.0);
            priceBook.put("Papaya",   60.0);
        }

        @Test
        void priceRange_returns_items_in_range() {
            NavigableMap<String, Double> result =
                SetsAndMaps.priceRange(priceBook, 50.0, 130.0);
            assertTrue(result.containsKey("Apple"));
            assertTrue(result.containsKey("Mango"));
            assertTrue(result.containsKey("Papaya"));
            assertFalse(result.containsKey("Cherry"));
            assertFalse(result.containsKey("Banana"));
        }

        @Test
        void floorEntry_returns_largest_key_not_exceeding_target() {
            TreeMap<Integer, String> tiers = new TreeMap<>();
            tiers.put(0, "bronze"); tiers.put(100, "silver"); tiers.put(500, "gold");
            assertEquals("silver",
                SetsAndMaps.floorEntry(tiers, 200).map(Map.Entry::getValue).orElse("?"));
        }

        @Test
        void floorEntry_returns_empty_when_all_keys_greater() {
            TreeMap<Integer, String> map = new TreeMap<>();
            map.put(100, "a");
            assertTrue(SetsAndMaps.floorEntry(map, 50).isEmpty());
        }
    }
}
