package com.javatraining.concurrency;

import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConcurrentCollections")
class ConcurrentCollectionsTest {

    @Nested
    @DisplayName("ConcurrentHashMap")
    class CHM {
        @Test void wordFrequency_counts_correctly() {
            List<String> words = List.of("a", "b", "a", "c", "b", "a");
            Map<String, Integer> freq = ConcurrentCollections.wordFrequency(words);
            assertEquals(3, freq.get("a"));
            assertEquals(2, freq.get("b"));
            assertEquals(1, freq.get("c"));
        }

        @Test void groupBy_groups_by_first_char() {
            List<String> items = List.of("apple", "avocado", "banana", "blueberry");
            Map<String, List<String>> groups = ConcurrentCollections.groupBy(
                items, s -> String.valueOf(s.charAt(0)));
            assertEquals(2, groups.get("a").size());
            assertEquals(2, groups.get("b").size());
        }

        @Test void incrementAll_adds_one_to_each_value() {
            Map<String, Integer> source = Map.of("x", 1, "y", 2, "z", 3);
            Map<String, Integer> result = ConcurrentCollections.incrementAll(source);
            assertEquals(2, result.get("x"));
            assertEquals(3, result.get("y"));
            assertEquals(4, result.get("z"));
        }
    }

    @Nested
    @DisplayName("CopyOnWriteArrayList EventBus")
    class COWAL {
        @Test void register_and_publish_fires_listener() {
            ConcurrentCollections.EventBus bus = new ConcurrentCollections.EventBus();
            int[] count = {0};
            bus.register(() -> count[0]++);
            bus.publish();
            assertEquals(1, count[0]);
        }

        @Test void deregister_stops_listener() {
            ConcurrentCollections.EventBus bus = new ConcurrentCollections.EventBus();
            int[] count = {0};
            Runnable listener = () -> count[0]++;
            bus.register(listener);
            bus.deregister(listener);
            bus.publish();
            assertEquals(0, count[0]);
        }

        @Test void multiple_listeners_all_called() {
            ConcurrentCollections.EventBus bus = new ConcurrentCollections.EventBus();
            int[] a = {0}, b = {0};
            bus.register(() -> a[0]++);
            bus.register(() -> b[0]++);
            bus.publish();
            assertEquals(1, a[0]);
            assertEquals(1, b[0]);
        }
    }

    @Nested
    @DisplayName("BlockingQueue producer-consumer")
    class BQ {
        @Test void all_items_consumed() throws InterruptedException {
            List<Integer> consumed = ConcurrentCollections.producerConsumer(20);
            assertEquals(20, consumed.size());
            // All values 0..19 present
            List<Integer> sorted = new ArrayList<>(consumed);
            Collections.sort(sorted);
            assertEquals(IntStream.range(0, 20).boxed().toList(), sorted);
        }
    }

    @Nested
    @DisplayName("PriorityBlockingQueue")
    class PBQ {
        @Test void drains_in_priority_order() throws InterruptedException {
            List<ConcurrentCollections.PriorityTask> tasks = List.of(
                new ConcurrentCollections.PriorityTask("low",    1),
                new ConcurrentCollections.PriorityTask("high",   10),
                new ConcurrentCollections.PriorityTask("medium", 5)
            );
            List<String> order = ConcurrentCollections.drainPriorityQueue(tasks);
            assertEquals(List.of("high", "medium", "low"), order);
        }
    }

    @Nested
    @DisplayName("ConcurrentSkipListMap")
    class CSLM {
        @Test void sorted_order_maintained() {
            List<Map.Entry<Integer, String>> entries = List.of(
                Map.entry(3, "c"), Map.entry(1, "a"), Map.entry(2, "b")
            );
            NavigableMap<Integer, String> map =
                ConcurrentCollections.buildSortedConcurrentMap(entries);
            assertEquals(List.of(1, 2, 3), new ArrayList<>(map.keySet()));
        }

        @Test void rangeQuery_returns_inclusive_range() {
            ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<>();
            for (int i = 1; i <= 5; i++) map.put(i, "v" + i);
            Map<Integer, String> range = ConcurrentCollections.rangeQuery(map, 2, 4);
            assertEquals(Set.of(2, 3, 4), range.keySet());
        }
    }

    @Nested
    @DisplayName("ConcurrentLinkedQueue")
    class CLQ {
        @Test void all_items_enqueued_and_dequeued() throws InterruptedException {
            int producers = 4, itemsEach = 25;
            int total = ConcurrentCollections.concurrentEnqueueDequeue(producers, itemsEach);
            assertEquals(producers * itemsEach, total);
        }
    }
}
