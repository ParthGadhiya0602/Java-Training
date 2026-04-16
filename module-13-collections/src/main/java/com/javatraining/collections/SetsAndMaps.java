package com.javatraining.collections;

import java.util.*;

/**
 * TOPIC: Set and Map family
 *
 * Set — no duplicates
 *   HashSet        — O(1) add/contains; no iteration order guarantee
 *   LinkedHashSet  — O(1) add/contains; preserves insertion order
 *   TreeSet        — O(log n) add/contains; sorted natural/Comparator order
 *                    Also implements NavigableSet (floor, ceiling, headSet, tailSet)
 *
 * Map — key→value, no duplicate keys
 *   HashMap        — O(1) get/put; no order guarantee
 *   LinkedHashMap  — O(1) get/put; preserves insertion order (or access order)
 *   TreeMap        — O(log n) get/put; sorted by key; implements NavigableMap
 *                    (floorKey, ceilingKey, headMap, tailMap, subMap)
 *
 * Choosing a Map/Set:
 *   Need fast lookup only?           → HashMap / HashSet
 *   Need order of insertion?         → LinkedHashMap / LinkedHashSet
 *   Need sorted order or range queries? → TreeMap / TreeSet
 */
public class SetsAndMaps {

    // -------------------------------------------------------------------------
    // 1. Set operations — union, intersection, difference
    // -------------------------------------------------------------------------

    /** Returns a new set containing all elements from both sets (union). */
    static <T> Set<T> union(Set<T> a, Set<T> b) {
        Set<T> result = new HashSet<>(a);
        result.addAll(b);
        return result;
    }

    /** Returns a new set containing only elements present in BOTH sets (intersection). */
    static <T> Set<T> intersection(Set<T> a, Set<T> b) {
        Set<T> result = new HashSet<>(a);
        result.retainAll(b);
        return result;
    }

    /** Returns elements in {@code a} that are NOT in {@code b} (set difference). */
    static <T> Set<T> difference(Set<T> a, Set<T> b) {
        Set<T> result = new HashSet<>(a);
        result.removeAll(b);
        return result;
    }

    /** Returns true if the two sets share no elements. */
    static <T> boolean disjoint(Set<T> a, Set<T> b) {
        return Collections.disjoint(a, b);
    }

    // -------------------------------------------------------------------------
    // 2. LinkedHashSet — deduplication preserving insertion order
    // -------------------------------------------------------------------------

    /** Removes duplicates from a list while preserving first-occurrence order. */
    static <T> List<T> deduplicate(List<T> list) {
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

    // -------------------------------------------------------------------------
    // 3. TreeSet — sorted set, range operations
    // -------------------------------------------------------------------------

    /**
     * Returns all elements in the sorted set whose string representation
     * is between [from, to] lexicographically (inclusive).
     */
    static NavigableSet<String> rangeQuery(NavigableSet<String> set,
                                           String from, String to) {
        return set.subSet(from, true, to, true);
    }

    /**
     * Returns the n closest elements to {@code target} from a sorted integer set.
     * Searches both above and below target.
     */
    static List<Integer> closestN(TreeSet<Integer> set, int target, int n) {
        Deque<Integer> deque = new ArrayDeque<>();
        Integer lo = set.floor(target);
        Integer hi = set.ceiling(target);
        // If target is exactly in the set, floor == ceiling: consume it once.
        if (lo != null && lo.equals(hi)) {
            deque.addLast(lo);
            lo = set.lower(lo);
            hi = set.higher(hi);
        }
        // Walk outward in both directions
        while (deque.size() < n) {
            if (lo == null && hi == null) break;
            if (lo == null) {
                deque.addLast(hi);
                hi = set.higher(hi);
            } else if (hi == null) {
                deque.addLast(lo);
                lo = set.lower(lo);
            } else if (Math.abs(lo - target) <= Math.abs(hi - target)) {
                deque.addLast(lo);
                lo = set.lower(lo);
            } else {
                deque.addLast(hi);
                hi = set.higher(hi);
            }
        }
        List<Integer> result = new ArrayList<>(deque);
        Collections.sort(result);
        return result;
    }

    // -------------------------------------------------------------------------
    // 4. HashMap — frequency counting, grouping
    // -------------------------------------------------------------------------

    /** Returns a map of each element to its occurrence count in the list. */
    static <T> Map<T, Integer> frequencies(List<T> list) {
        Map<T, Integer> freq = new HashMap<>();
        for (T item : list) freq.merge(item, 1, Integer::sum);
        return freq;
    }

    /**
     * Returns a map of first character → list of words starting with that character.
     * Preserves encounter order within each group.
     */
    static Map<Character, List<String>> groupByFirstChar(List<String> words) {
        Map<Character, List<String>> groups = new LinkedHashMap<>();
        for (String w : words) {
            if (w.isEmpty()) continue;
            groups.computeIfAbsent(w.charAt(0), k -> new ArrayList<>()).add(w);
        }
        return groups;
    }

    // -------------------------------------------------------------------------
    // 5. TreeMap — NavigableMap range queries
    // -------------------------------------------------------------------------

    /**
     * Given a price book (product→price), returns the products whose price
     * falls in [minPrice, maxPrice] as a sorted map.
     */
    static NavigableMap<String, Double> priceRange(TreeMap<String, Double> priceBook,
                                                   double minPrice, double maxPrice) {
        // Invert to price→name, query by price range, collect matching names
        TreeMap<Double, String> byPrice = new TreeMap<>();
        priceBook.forEach((name, price) -> byPrice.put(price, name));

        NavigableMap<Double, String> inRange = byPrice.subMap(minPrice, true, maxPrice, true);
        TreeMap<String, Double> result = new TreeMap<>();
        inRange.forEach((price, name) -> result.put(name, price));
        return result;
    }

    /**
     * Returns the floor entry (largest key ≤ given key) from a NavigableMap.
     * Useful for tiered pricing, rate tables, etc.
     */
    static <K, V> Optional<Map.Entry<K, V>> floorEntry(NavigableMap<K, V> map, K key) {
        return Optional.ofNullable(map.floorEntry(key));
    }

    // -------------------------------------------------------------------------
    // 6. LinkedHashMap as an LRU cache (access-order mode)
    // -------------------------------------------------------------------------
    static <K, V> LinkedHashMap<K, V> lruMap(int capacity) {
        return new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void setDemo() {
        System.out.println("=== Set operations ===");
        Set<Integer> a = Set.of(1, 2, 3, 4);
        Set<Integer> b = Set.of(3, 4, 5, 6);

        // Use TreeSet for deterministic output
        System.out.println("union:        " + new TreeSet<>(union(a, b)));
        System.out.println("intersection: " + new TreeSet<>(intersection(a, b)));
        System.out.println("difference:   " + new TreeSet<>(difference(a, b)));
        System.out.println("disjoint:     " + disjoint(Set.of(1,2), Set.of(3,4)));

        List<Integer> dup = List.of(3, 1, 4, 1, 5, 9, 2, 6, 5, 3);
        System.out.println("deduplicate:  " + deduplicate(dup));
    }

    static void treeSetDemo() {
        System.out.println("\n=== TreeSet (sorted, range) ===");
        NavigableSet<String> cities = new TreeSet<>(
            List.of("Mumbai","Delhi","Chennai","Kolkata","Bengaluru","Pune","Hyderabad"));
        System.out.println("cities C-M:  " + rangeQuery(cities, "C", "M"));
        System.out.println("first:       " + cities.first());
        System.out.println("last:        " + cities.last());
        System.out.println("floor(H):    " + cities.floor("H"));
        System.out.println("ceiling(H):  " + cities.ceiling("H"));

        TreeSet<Integer> nums = new TreeSet<>(List.of(1,3,5,7,9,11,13));
        System.out.println("closest 3 to 6: " + closestN(nums, 6, 3));
    }

    static void mapDemo() {
        System.out.println("\n=== HashMap / LinkedHashMap / TreeMap ===");
        List<String> words = List.of("apple","banana","avocado","blueberry","cherry","apricot");
        System.out.println("frequencies: " + new TreeMap<>(frequencies(words)));
        System.out.println("groupByFirst: " + groupByFirstChar(words));

        TreeMap<String, Double> prices = new TreeMap<>();
        prices.put("Apple",  120.0); prices.put("Banana", 40.0);
        prices.put("Cherry", 350.0); prices.put("Mango",  80.0);
        prices.put("Papaya", 60.0);
        System.out.println("price 50-150: " + priceRange(prices, 50.0, 150.0));

        // tiered discount table: threshold → discount %
        TreeMap<Integer, Integer> discounts = new TreeMap<>();
        discounts.put(0, 0); discounts.put(100, 5); discounts.put(500, 10); discounts.put(1000, 15);
        int order = 750;
        System.out.println("discount for " + order + ": "
            + floorEntry(discounts, order).map(e -> e.getValue() + "%").orElse("0%"));
    }

    public static void main(String[] args) {
        setDemo();
        treeSetDemo();
        mapDemo();
    }
}
