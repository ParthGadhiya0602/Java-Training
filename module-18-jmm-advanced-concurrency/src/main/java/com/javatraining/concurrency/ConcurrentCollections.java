package com.javatraining.concurrency;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Module 18 - Concurrent Collections
 *
 * java.util.concurrent ships thread-safe collections designed for concurrent access:
 *
 *   ConcurrentHashMap          - scalable hash map; segment-level locking
 *   CopyOnWriteArrayList       - reads are lock-free; writes copy the array
 *   CopyOnWriteArraySet        - backed by CopyOnWriteArrayList
 *   BlockingQueue family       - producer-consumer with blocking operations
 *     ArrayBlockingQueue       - bounded, backed by array
 *     LinkedBlockingQueue      - optionally bounded, backed by linked nodes
 *     PriorityBlockingQueue    - unbounded, ordered by Comparator
 *     DelayQueue               - elements become available after a delay
 *     SynchronousQueue         - zero-capacity rendezvous point
 *   ConcurrentLinkedQueue      - unbounded lock-free queue
 *   ConcurrentLinkedDeque      - unbounded lock-free deque
 *   ConcurrentSkipListMap/Set  - sorted, concurrent equivalents of TreeMap/Set
 */
public class ConcurrentCollections {

    // ── ConcurrentHashMap ─────────────────────────────────────────────────────

    /**
     * ConcurrentHashMap allows concurrent reads and concurrent writes to
     * different segments without a global lock.
     *
     * Atomic update methods to know:
     *   compute(k, (k,v) -> newV)       - compute from current value
     *   computeIfAbsent(k, k -> v)      - only if absent
     *   computeIfPresent(k, (k,v) -> v) - only if present
     *   merge(k, v, (old,new) -> merged)- combine or insert
     *   putIfAbsent(k, v)               - atomic insert-if-absent
     *   replace(k, expect, update)      - CAS update
     */
    public static Map<String, Integer> wordFrequency(List<String> words) {
        ConcurrentHashMap<String, Integer> freq = new ConcurrentHashMap<>();
        words.parallelStream().forEach(w ->
            freq.merge(w, 1, Integer::sum)
        );
        return freq;
    }

    /** computeIfAbsent: safe lazy-initialisation of nested structures. */
    public static Map<String, List<String>> groupBy(List<String> items,
                                                     java.util.function.Function<String, String> keyFn) {
        ConcurrentHashMap<String, List<String>> map = new ConcurrentHashMap<>();
        items.parallelStream().forEach(item ->
            map.computeIfAbsent(keyFn.apply(item),
                                k -> Collections.synchronizedList(new ArrayList<>()))
               .add(item)
        );
        return map;
    }

    /** forEachEntry: parallel iteration - runs action in ForkJoinPool. */
    public static Map<String, Integer> incrementAll(Map<String, Integer> source) {
        ConcurrentHashMap<String, Integer> result = new ConcurrentHashMap<>(source);
        result.replaceAll((k, v) -> v + 1);
        return result;
    }

    /** reduce: aggregate all values in parallel. */
    public static int sumAllValues(ConcurrentHashMap<String, Integer> map) {
        return map.reduceValues(1, Integer::sum);
    }

    // ── CopyOnWriteArrayList ──────────────────────────────────────────────────

    /**
     * CopyOnWriteArrayList: every write (add, set, remove) copies the
     * underlying array.  Reads are lock-free and never throw
     * ConcurrentModificationException.
     *
     * Best for: collections read far more often than written, where
     * iteration must not be disrupted by concurrent modifications.
     * Classic use: event listener lists.
     */
    public static class EventBus {
        private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

        public void register(Runnable listener) { listeners.add(listener); }
        public void deregister(Runnable listener) { listeners.remove(listener); }

        /** Safe to iterate while listeners are being added/removed concurrently. */
        public void publish() {
            for (Runnable listener : listeners) {
                listener.run();
            }
        }

        public int listenerCount() { return listeners.size(); }
    }

    // ── BlockingQueue ─────────────────────────────────────────────────────────

    /**
     * BlockingQueue API:
     *   put(e)            - insert, blocks if full
     *   take()            - remove, blocks if empty
     *   offer(e, t, unit) - insert with timeout
     *   poll(t, unit)     - remove with timeout
     *   offer(e)          - non-blocking insert; returns false if full
     *   peek()            - inspect head without removing
     */
    public static List<Integer> producerConsumer(int itemCount) throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(16);
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(itemCount);

        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < itemCount; i++) {
                    queue.put(i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    Integer item = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (item == null) break;
                    consumed.add(item);
                    done.countDown();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        done.await();
        producer.join();
        consumer.join();
        return consumed;
    }

    /** PriorityBlockingQueue: tasks consumed in priority order. */
    public static class PriorityTask implements Comparable<PriorityTask> {
        public final String name;
        public final int priority;

        public PriorityTask(String name, int priority) {
            this.name = name; this.priority = priority;
        }

        @Override public int compareTo(PriorityTask other) {
            return Integer.compare(other.priority, this.priority); // higher = first
        }
    }

    public static List<String> drainPriorityQueue(List<PriorityTask> tasks)
            throws InterruptedException {
        PriorityBlockingQueue<PriorityTask> pq = new PriorityBlockingQueue<>();
        pq.addAll(tasks);

        List<String> order = new ArrayList<>();
        while (!pq.isEmpty()) {
            order.add(pq.take().name);
        }
        return order;
    }

    // ── ConcurrentSkipListMap ─────────────────────────────────────────────────

    /**
     * ConcurrentSkipListMap: sorted concurrent map (like TreeMap but thread-safe).
     * O(log n) for get/put/remove.  Supports headMap, tailMap, subMap in concurrent context.
     */
    public static NavigableMap<Integer, String> buildSortedConcurrentMap(
            List<Map.Entry<Integer, String>> entries) {
        ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<>();
        entries.parallelStream().forEach(e -> map.put(e.getKey(), e.getValue()));
        return map;
    }

    /** Range query on ConcurrentSkipListMap - safe under concurrent modification. */
    public static Map<Integer, String> rangeQuery(ConcurrentSkipListMap<Integer, String> map,
                                                    int fromKey, int toKey) {
        return new LinkedHashMap<>(map.subMap(fromKey, true, toKey, true));
    }

    // ── ConcurrentLinkedQueue ─────────────────────────────────────────────────

    /**
     * ConcurrentLinkedQueue: lock-free, unbounded FIFO queue.
     * Ideal for many-producer many-consumer when blocking is not needed.
     */
    public static int concurrentEnqueueDequeue(int producers, int itemsEach)
            throws InterruptedException {
        ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();
        AtomicInteger total = new AtomicInteger();
        CountDownLatch produced = new CountDownLatch(producers);

        for (int i = 0; i < producers; i++) {
            final int base = i * itemsEach;
            new Thread(() -> {
                for (int j = 0; j < itemsEach; j++) queue.offer(base + j);
                produced.countDown();
            }).start();
        }
        produced.await();

        Integer item;
        while ((item = queue.poll()) != null) total.incrementAndGet();
        return total.get();
    }
}
