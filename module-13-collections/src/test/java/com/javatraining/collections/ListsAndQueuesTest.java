package com.javatraining.collections;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ListsAndQueuesTest {

    // ── ArrayList helpers ─────────────────────────────────────────────────────

    @Nested
    class ArrayListTests {

        @Test
        void removeAll_removes_all_occurrences() {
            List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 2, 4, 2));
            ListsAndQueues.removeAll(list, 2);
            assertEquals(List.of(1, 3, 4), list);
        }

        @Test
        void removeAll_no_match_leaves_list_unchanged() {
            List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
            ListsAndQueues.removeAll(list, 9);
            assertEquals(List.of(1, 2, 3), list);
        }

        @Test
        void everyOther_returns_even_indexed_elements() {
            assertEquals(List.of(1, 3, 5),
                ListsAndQueues.everyOther(List.of(1, 2, 3, 4, 5)));
        }

        @Test
        void everyOther_single_element() {
            assertEquals(List.of(42),
                ListsAndQueues.everyOther(List.of(42)));
        }

        @Test
        void everyOther_empty_list() {
            assertTrue(ListsAndQueues.everyOther(List.of()).isEmpty());
        }

        @Test
        void rotateLeft_shifts_elements() {
            List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 4, 5));
            ListsAndQueues.rotateLeft(list, 2);
            assertEquals(List.of(3, 4, 5, 1, 2), list);
        }

        @Test
        void rotateLeft_by_length_is_identity() {
            List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
            ListsAndQueues.rotateLeft(list, 3);
            assertEquals(List.of(1, 2, 3), list);
        }
    }

    // ── Deque / bracket balancing ─────────────────────────────────────────────

    @Nested
    class DequeTests {

        @Test
        void balanced_mixed_brackets() {
            assertTrue(ListsAndQueues.isBalanced("([{}])"));
        }

        @Test
        void unbalanced_wrong_close() {
            assertFalse(ListsAndQueues.isBalanced("([)]"));
        }

        @Test
        void unbalanced_extra_open() {
            assertFalse(ListsAndQueues.isBalanced("(("));
        }

        @Test
        void empty_string_is_balanced() {
            assertTrue(ListsAndQueues.isBalanced(""));
        }

        @Test
        void print_queue_preserves_FIFO_order() {
            assertEquals(List.of("a", "b", "c"),
                ListsAndQueues.processPrintQueue(List.of("a", "b", "c")));
        }

        @Test
        void lastK_returns_last_k_elements() {
            assertEquals(List.of(5, 6, 7),
                ListsAndQueues.lastK(List.of(1, 2, 3, 4, 5, 6, 7), 3));
        }

        @Test
        void lastK_when_fewer_than_k_elements() {
            assertEquals(List.of(1, 2),
                ListsAndQueues.lastK(List.of(1, 2), 5));
        }

        @Test
        void lastK_zero_returns_empty() {
            assertTrue(ListsAndQueues.lastK(List.of(1, 2, 3), 0).isEmpty());
        }
    }

    // ── PriorityQueue ─────────────────────────────────────────────────────────

    @Nested
    class PriorityQueueTests {

        @Test
        void topKSmallest_returns_k_smallest_sorted() {
            List<Integer> data = List.of(5, 1, 9, 3, 7, 2, 8);
            assertEquals(List.of(1, 2, 3), ListsAndQueues.topKSmallest(data, 3));
        }

        @Test
        void topKSmallest_k_equals_size() {
            List<Integer> data = List.of(3, 1, 2);
            assertEquals(List.of(1, 2, 3), ListsAndQueues.topKSmallest(data, 3));
        }

        @Test
        void topKSmallest_zero_k_returns_empty() {
            assertTrue(ListsAndQueues.topKSmallest(List.of(1, 2, 3), 0).isEmpty());
        }

        @Test
        void mergeKSorted_merges_correctly() {
            List<List<Integer>> lists = List.of(
                List.of(1, 4, 7),
                List.of(2, 5, 8),
                List.of(3, 6, 9)
            );
            assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9),
                ListsAndQueues.mergeKSorted(lists));
        }

        @Test
        void mergeKSorted_with_empty_sublists() {
            List<List<Integer>> lists = List.of(
                List.of(1, 3),
                List.of(),
                List.of(2, 4)
            );
            assertEquals(List.of(1, 2, 3, 4), ListsAndQueues.mergeKSorted(lists));
        }

        @Test
        void scheduleTasks_highest_priority_first() {
            List<ListsAndQueues.Task> tasks = List.of(
                new ListsAndQueues.Task("low",    1),
                new ListsAndQueues.Task("high",   5),
                new ListsAndQueues.Task("medium", 3)
            );
            List<String> order = ListsAndQueues.scheduleTasks(tasks);
            assertEquals("high",   order.get(0));
            assertEquals("medium", order.get(1));
            assertEquals("low",    order.get(2));
        }
    }
}
