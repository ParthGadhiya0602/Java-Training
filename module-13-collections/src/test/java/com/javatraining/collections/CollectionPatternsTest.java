package com.javatraining.collections;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CollectionPatternsTest {

    // ── Frequency map & modes ─────────────────────────────────────────────────

    @Nested
    class FrequencyPatternTests {

        @Test
        void frequencyMap_counts_all_elements() {
            Map<String, Integer> freq = CollectionPatterns.frequencyMap(
                List.of("a", "b", "a", "c", "a", "b"));
            assertEquals(3, freq.get("a"));
            assertEquals(2, freq.get("b"));
            assertEquals(1, freq.get("c"));
        }

        @Test
        void frequencyMap_empty_list_returns_empty_map() {
            assertTrue(CollectionPatterns.frequencyMap(List.of()).isEmpty());
        }

        @Test
        void modes_single_mode() {
            List<String> result = CollectionPatterns.modes(
                List.of("a", "b", "a", "c", "a"));
            assertEquals(List.of("a"), result);
        }

        @Test
        void modes_multiple_modes() {
            List<String> result = CollectionPatterns.modes(
                List.of("a", "b", "a", "b", "c"));
            assertTrue(result.contains("a"));
            assertTrue(result.contains("b"));
            assertEquals(2, result.size());
        }

        @Test
        void modes_empty_list_returns_empty() {
            assertTrue(CollectionPatterns.modes(List.of()).isEmpty());
        }

        @Test
        void groupAnagrams_groups_correctly() {
            List<List<String>> result = CollectionPatterns.groupAnagrams(
                List.of("eat", "tea", "tan", "ate", "nat", "bat"));
            // 3 groups: [ate,eat,tea], [bat], [nat,tan]
            assertEquals(3, result.size());
            // groups are sorted by first element, and elements within are sorted
            assertEquals(List.of("ate", "eat", "tea"), result.get(0));
            assertEquals(List.of("bat"),               result.get(1));
            assertEquals(List.of("nat", "tan"),        result.get(2));
        }
    }

    // ── Multimap ──────────────────────────────────────────────────────────────

    @Nested
    class MultimapTests {

        @Test
        void put_and_get() {
            CollectionPatterns.Multimap<String, Integer> mm = new CollectionPatterns.Multimap<>();
            mm.put("a", 1);
            mm.put("a", 2);
            mm.put("b", 3);
            assertEquals(List.of(1, 2), mm.get("a"));
            assertEquals(List.of(3),    mm.get("b"));
        }

        @Test
        void get_absent_key_returns_empty_list() {
            CollectionPatterns.Multimap<String, Integer> mm = new CollectionPatterns.Multimap<>();
            assertTrue(mm.get("missing").isEmpty());
        }

        @Test
        void size_counts_all_values() {
            CollectionPatterns.Multimap<String, Integer> mm = new CollectionPatterns.Multimap<>();
            mm.put("a", 1); mm.put("a", 2); mm.put("b", 3);
            assertEquals(3, mm.size());
        }

        @Test
        void groupBy_groups_by_key_function() {
            CollectionPatterns.Multimap<Integer, String> mm =
                CollectionPatterns.Multimap.groupBy(
                    List.of("a", "bb", "ccc", "dd", "e"),
                    String::length);
            assertEquals(List.of("a", "e"),   mm.get(1));
            assertEquals(List.of("bb", "dd"), mm.get(2));
            assertEquals(List.of("ccc"),      mm.get(3));
        }

        @Test
        void get_returns_unmodifiable_view() {
            CollectionPatterns.Multimap<String, Integer> mm = new CollectionPatterns.Multimap<>();
            mm.put("x", 1);
            assertThrows(UnsupportedOperationException.class,
                () -> mm.get("x").add(99));
        }
    }

    // ── BiMap ─────────────────────────────────────────────────────────────────

    @Nested
    class BiMapTests {

        CollectionPatterns.BiMap<String, Integer> biMap;

        @BeforeEach
        void setUp() {
            biMap = new CollectionPatterns.BiMap<>();
            biMap.put("HTTP",  80);
            biMap.put("HTTPS", 443);
            biMap.put("SSH",   22);
        }

        @Test
        void getByKey_returns_value() {
            assertEquals(Optional.of(80), biMap.getByKey("HTTP"));
        }

        @Test
        void getByValue_returns_key() {
            assertEquals(Optional.of("HTTPS"), biMap.getByValue(443));
        }

        @Test
        void getByKey_absent_returns_empty() {
            assertTrue(biMap.getByKey("FTP").isEmpty());
        }

        @Test
        void duplicate_key_throws() {
            assertThrows(IllegalArgumentException.class,
                () -> biMap.put("HTTP", 8080));
        }

        @Test
        void duplicate_value_throws() {
            assertThrows(IllegalArgumentException.class,
                () -> biMap.put("NEWHTTP", 80));
        }

        @Test
        void removeKey_deletes_both_directions() {
            biMap.removeKey("HTTP");
            assertTrue(biMap.getByKey("HTTP").isEmpty());
            assertTrue(biMap.getByValue(80).isEmpty());
            assertEquals(2, biMap.size());
        }
    }

    // ── Sliding window maximum ────────────────────────────────────────────────

    @Nested
    class SlidingWindowTests {

        @Test
        void window_of_3_returns_correct_maxima() {
            int[] nums = {1, 3, -1, -3, 5, 3, 6, 7};
            assertEquals(List.of(3, 3, 5, 5, 6, 7),
                CollectionPatterns.slidingWindowMax(nums, 3));
        }

        @Test
        void window_of_1_returns_all_elements() {
            int[] nums = {4, 2, 7, 1};
            assertEquals(List.of(4, 2, 7, 1),
                CollectionPatterns.slidingWindowMax(nums, 1));
        }

        @Test
        void window_equal_array_size_returns_single_max() {
            int[] nums = {3, 1, 4, 1, 5};
            assertEquals(List.of(5),
                CollectionPatterns.slidingWindowMax(nums, 5));
        }

        @Test
        void all_same_elements() {
            int[] nums = {2, 2, 2, 2};
            assertEquals(List.of(2, 2, 2),
                CollectionPatterns.slidingWindowMax(nums, 2));
        }
    }

    // ── Top-K most frequent ───────────────────────────────────────────────────

    @Nested
    class TopKTests {

        List<String> words = List.of(
            "apple","banana","apple","cherry","banana","apple","date","banana");

        @Test
        void topK_returns_k_most_frequent() {
            List<String> top2 = CollectionPatterns.topKFrequent(words, 2);
            assertEquals(2, top2.size());
            assertTrue(top2.contains("apple"));
            assertTrue(top2.contains("banana"));
        }

        @Test
        void topK_1_returns_mode() {
            List<String> top1 = CollectionPatterns.topKFrequent(words, 1);
            assertEquals(1, top1.size());
            assertEquals("apple", top1.get(0));
        }

        @Test
        void topK_with_integers() {
            List<Integer> nums = List.of(1, 1, 1, 2, 2, 3);
            List<Integer> top2 = CollectionPatterns.topKFrequent(nums, 2);
            assertEquals(2, top2.size());
            assertTrue(top2.contains(1));
            assertTrue(top2.contains(2));
        }
    }
}
