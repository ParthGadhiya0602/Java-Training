package com.javatraining.collections;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TOPIC: Practical collection patterns
 *
 * Pattern 1 — Frequency map (histogram)
 *   Count occurrences; find mode; build anagram groups.
 *
 * Pattern 2 — Multimap (key → many values)
 *   Map<K, List<V>> — one key maps to a list of values.
 *   Used for grouping, inverted indexes, graph adjacency lists.
 *
 * Pattern 3 — Bidirectional map
 *   Two HashMaps: forward (K→V) and inverse (V→K).
 *   O(1) lookup in both directions; keys and values must be unique.
 *
 * Pattern 4 — Sliding window with Deque
 *   O(n) maximum/minimum of every window of size k.
 *   Monotonic deque keeps the index of the window's best element at front.
 *
 * Pattern 5 — Top-K with PriorityQueue
 *   Find k most frequent elements in O(n log k).
 */
public class CollectionPatterns {

    // -------------------------------------------------------------------------
    // Pattern 1 — Frequency map
    // -------------------------------------------------------------------------

    /** Builds a frequency map: element → count. */
    static <T> Map<T, Integer> frequencyMap(List<T> items) {
        Map<T, Integer> map = new HashMap<>();
        for (T item : items) map.merge(item, 1, Integer::sum);
        return map;
    }

    /** Returns the element(s) that appear most often. */
    static <T> List<T> modes(List<T> items) {
        if (items.isEmpty()) return List.of();
        Map<T, Integer> freq = frequencyMap(items);
        int max = Collections.max(freq.values());
        return freq.entrySet().stream()
            .filter(e -> e.getValue() == max)
            .map(Map.Entry::getKey)
            .sorted(Comparator.comparing(Object::toString))
            .toList();
    }

    /**
     * Groups anagrams together.
     * ["eat","tea","tan","ate","nat","bat"] →
     *   [["ate","eat","tea"], ["bat"], ["nat","tan"]]
     * Sorted within each group and groups sorted by first element.
     */
    static List<List<String>> groupAnagrams(List<String> words) {
        Map<String, List<String>> groups = new HashMap<>();
        for (String w : words) {
            char[] chars = w.toCharArray();
            Arrays.sort(chars);
            String key = new String(chars);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(w);
        }
        List<List<String>> result = new ArrayList<>();
        for (List<String> group : groups.values()) {
            Collections.sort(group);
            result.add(group);
        }
        result.sort(Comparator.comparing(g -> g.get(0)));
        return result;
    }

    // -------------------------------------------------------------------------
    // Pattern 2 — Multimap
    // -------------------------------------------------------------------------
    static final class Multimap<K, V> {
        private final Map<K, List<V>> map = new LinkedHashMap<>();

        void put(K key, V value) {
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        List<V> get(K key) {
            return Collections.unmodifiableList(map.getOrDefault(key, List.of()));
        }

        boolean containsKey(K key) { return map.containsKey(key); }
        Set<K>  keySet()           { return Collections.unmodifiableSet(map.keySet()); }
        int     size()             { return map.values().stream().mapToInt(List::size).sum(); }
        int     keyCount()         { return map.size(); }

        /** Returns all (key, value) pairs as an unmodifiable map snapshot. */
        Map<K, List<V>> asMap() {
            Map<K, List<V>> copy = new LinkedHashMap<>();
            map.forEach((k, vs) -> copy.put(k, Collections.unmodifiableList(vs)));
            return Collections.unmodifiableMap(copy);
        }

        /**
         * Builds a Multimap from a list by computing the key for each element.
         * Equivalent to Collectors.groupingBy but returns a Multimap.
         */
        static <K, V> Multimap<K, V> groupBy(List<V> items,
                                              java.util.function.Function<V, K> keyFn) {
            Multimap<K, V> mm = new Multimap<>();
            for (V item : items) mm.put(keyFn.apply(item), item);
            return mm;
        }
    }

    // -------------------------------------------------------------------------
    // Pattern 3 — Bidirectional map
    // -------------------------------------------------------------------------
    static final class BiMap<K, V> {
        private final Map<K, V> forward = new HashMap<>();
        private final Map<V, K> inverse = new HashMap<>();

        /** Puts a key-value pair. Throws if key OR value already mapped. */
        void put(K key, V value) {
            if (forward.containsKey(key))
                throw new IllegalArgumentException("key already mapped: " + key);
            if (inverse.containsKey(value))
                throw new IllegalArgumentException("value already mapped: " + value);
            forward.put(key, value);
            inverse.put(value, key);
        }

        /** Removes the mapping for key. Returns the removed value, or null. */
        V removeKey(K key) {
            V value = forward.remove(key);
            if (value != null) inverse.remove(value);
            return value;
        }

        Optional<V> getByKey(K key)   { return Optional.ofNullable(forward.get(key)); }
        Optional<K> getByValue(V val) { return Optional.ofNullable(inverse.get(val)); }
        boolean containsKey(K key)    { return forward.containsKey(key); }
        boolean containsValue(V val)  { return inverse.containsKey(val); }
        int     size()                { return forward.size(); }
    }

    // -------------------------------------------------------------------------
    // Pattern 4 — Sliding window maximum (monotonic deque)
    //    For each window of size k, return the maximum element.
    //    The deque stores INDICES, in decreasing order of their values.
    //    Front of deque is always the index of the current window maximum.
    // -------------------------------------------------------------------------
    static List<Integer> slidingWindowMax(int[] nums, int k) {
        List<Integer> result = new ArrayList<>();
        Deque<Integer> deque = new ArrayDeque<>();  // stores indices

        for (int i = 0; i < nums.length; i++) {
            // Remove indices outside the current window
            while (!deque.isEmpty() && deque.peekFirst() < i - k + 1)
                deque.pollFirst();

            // Remove indices whose values are less than current (they'll never be max)
            while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i])
                deque.pollLast();

            deque.addLast(i);

            // Window is fully formed
            if (i >= k - 1) result.add(nums[deque.peekFirst()]);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Pattern 5 — Top-K most frequent elements
    //    Build a frequency map, then use a min-heap of size k.
    //    O(n log k) time; returns elements in descending frequency order.
    // -------------------------------------------------------------------------
    static <T> List<T> topKFrequent(List<T> items, int k) {
        Map<T, Integer> freq = frequencyMap(items);
        // min-heap by frequency; when size > k, remove least-frequent
        PriorityQueue<Map.Entry<T, Integer>> minHeap =
            new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));

        for (Map.Entry<T, Integer> entry : freq.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > k) minHeap.poll();
        }

        // Drain heap (ascending) then reverse for descending frequency
        List<T> result = new ArrayList<>();
        while (!minHeap.isEmpty()) result.add(minHeap.poll().getKey());
        Collections.reverse(result);
        return result;
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void frequencyDemo() {
        System.out.println("=== Frequency map & modes ===");
        List<Integer> nums = List.of(1, 2, 3, 2, 3, 3, 4, 2);
        System.out.println("freq:  " + new TreeMap<>(frequencyMap(nums)));
        System.out.println("modes: " + modes(nums));

        List<String> words = List.of("eat","tea","tan","ate","nat","bat");
        System.out.println("anagrams: " + groupAnagrams(words));
    }

    static void multimapDemo() {
        System.out.println("\n=== Multimap ===");
        Multimap<String, String> courses = Multimap.groupBy(
            List.of("Alice","Bob","Carol","Dave","Eve"),
            name -> name.length() % 2 == 0 ? "even" : "odd"
        );
        System.out.println("even-length names: " + courses.get("even"));
        System.out.println("odd-length names:  " + courses.get("odd"));
        System.out.println("total entries:     " + courses.size());
    }

    static void biMapDemo() {
        System.out.println("\n=== BiMap ===");
        BiMap<String, Integer> portMap = new BiMap<>();
        portMap.put("HTTP",  80);
        portMap.put("HTTPS", 443);
        portMap.put("SSH",   22);

        System.out.println("HTTP→:   " + portMap.getByKey("HTTP").orElse(-1));
        System.out.println("443→:    " + portMap.getByValue(443).orElse("?"));
        portMap.removeKey("HTTP");
        System.out.println("after remove HTTP, size: " + portMap.size());

        try { portMap.put("HTTPS2", 443); }
        catch (IllegalArgumentException e) { System.out.println("duplicate value blocked: " + e.getMessage()); }
    }

    static void slidingWindowDemo() {
        System.out.println("\n=== Sliding window max (deque) ===");
        int[] nums = {1, 3, -1, -3, 5, 3, 6, 7};
        System.out.println("window=3 max: " + slidingWindowMax(nums, 3));
    }

    static void topKDemo() {
        System.out.println("\n=== Top-K most frequent ===");
        List<String> words = List.of(
            "apple","banana","apple","cherry","banana","apple","date","banana","cherry");
        System.out.println("top 2: " + topKFrequent(words, 2));
        System.out.println("top 3: " + topKFrequent(words, 3));
    }

    public static void main(String[] args) {
        frequencyDemo();
        multimapDemo();
        biMapDemo();
        slidingWindowDemo();
        topKDemo();
    }
}
