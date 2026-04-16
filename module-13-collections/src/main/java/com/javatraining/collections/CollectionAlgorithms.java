package com.javatraining.collections;

import java.util.*;

/**
 * TOPIC: java.util.Collections utility class
 *
 * Collections provides static algorithms that work on any Collection or List:
 *
 *   Ordering     sort, reverseOrder, shuffle, rotate, swap, reverse
 *   Search       binarySearch (list must be sorted first)
 *   Extremes     min, max
 *   Frequency    frequency, disjoint
 *   Filling      fill, nCopies, copy
 *   Views        unmodifiableList/Set/Map, synchronizedList/Set/Map
 *                singletonList, emptyList/Set/Map
 *   Bulk ops     addAll(collection, elements...)
 *
 * Key rules:
 *   • binarySearch requires the list to be sorted ascending by the same order
 *   • unmodifiable views wrap without copying — the underlying list can still be mutated
 *   • synchronized views are thread-safe for individual operations but NOT for compound ops
 */
public class CollectionAlgorithms {

    // -------------------------------------------------------------------------
    // 1. Sorting — natural order, custom Comparator, reverse
    // -------------------------------------------------------------------------

    /** Sorts a list of strings by length, then alphabetically for equal lengths. */
    static List<String> sortByLengthThenAlpha(List<String> words) {
        List<String> result = new ArrayList<>(words);
        result.sort(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
        return result;
    }

    /** Sorts records by multiple fields: department ASC, then salary DESC. */
    record Employee(String name, String dept, double salary) {}

    static List<Employee> sortEmployees(List<Employee> employees) {
        List<Employee> result = new ArrayList<>(employees);
        result.sort(Comparator.comparing(Employee::dept)
                              .thenComparingDouble(Employee::salary).reversed()
                              .thenComparing(Employee::name));
        return result;
    }

    // -------------------------------------------------------------------------
    // 2. Binary search
    //    Precondition: list must be sorted by the SAME order used for search.
    //    Returns index ≥ 0 if found; -(insertion point) - 1 if absent.
    // -------------------------------------------------------------------------

    /** Returns the index of target in the sorted list, or -1 if absent. */
    static int binarySearch(List<Integer> sorted, int target) {
        int idx = Collections.binarySearch(sorted, target);
        return idx >= 0 ? idx : -1;
    }

    /** Returns the insertion point where target would be inserted to keep order. */
    static int insertionPoint(List<Integer> sorted, int target) {
        int idx = Collections.binarySearch(sorted, target);
        return idx >= 0 ? idx : -(idx + 1);
    }

    // -------------------------------------------------------------------------
    // 3. Frequency and disjoint
    // -------------------------------------------------------------------------

    /** Returns how many times target appears in the collection. */
    static <T> int frequency(Collection<T> col, T target) {
        return Collections.frequency(col, target);
    }

    /** Returns true if the two collections share no elements. */
    static boolean disjoint(Collection<?> a, Collection<?> b) {
        return Collections.disjoint(a, b);
    }

    // -------------------------------------------------------------------------
    // 4. Unmodifiable views
    //    The view wraps the original without copying.
    //    Writes through the view throw UnsupportedOperationException.
    //    Writes to the ORIGINAL are still reflected in the view!
    // -------------------------------------------------------------------------

    /**
     * Returns a published (unmodifiable) view of the internal list.
     * Demonstrates why defensive copying is needed if you want true immutability.
     */
    static final class LiveScoreboard {
        private final List<Integer> scores = new ArrayList<>();
        private final List<Integer> view   = Collections.unmodifiableList(scores);

        void addScore(int s) { scores.add(s); }
        List<Integer> publicView() { return view; }   // callers can't write
    }

    // -------------------------------------------------------------------------
    // 5. Utility constructions
    // -------------------------------------------------------------------------

    /** Returns an immutable list of n copies of the given value. */
    static <T> List<T> repeat(T value, int n) {
        return Collections.nCopies(n, value);
    }

    /**
     * Fills every position in the list with the same value, in-place.
     * Returns the list for convenience.
     */
    static <T> List<T> fillWith(List<T> list, T value) {
        Collections.fill(list, value);
        return list;
    }

    /**
     * Returns a new sorted list of integers from [lo, hi] inclusive,
     * using Collections.addAll for bulk population.
     */
    static List<Integer> intRange(int lo, int hi) {
        List<Integer> list = new ArrayList<>(hi - lo + 1);
        for (int i = lo; i <= hi; i++) list.add(i);
        return list;
    }

    // -------------------------------------------------------------------------
    // 6. rotate / reverse / swap
    // -------------------------------------------------------------------------

    /** Returns a new list that is a right-rotation by k positions. */
    static <T> List<T> rotateRight(List<T> list, int k) {
        List<T> copy = new ArrayList<>(list);
        Collections.rotate(copy, k);
        return copy;
    }

    /** Returns the list reversed IN-PLACE; returns the same list for convenience. */
    static <T> List<T> reverseInPlace(List<T> list) {
        Collections.reverse(list);
        return list;
    }

    /**
     * Shuffles the list with a fixed seed so results are deterministic in tests.
     */
    static <T> List<T> deterministicShuffle(List<T> list, long seed) {
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy, new Random(seed));
        return copy;
    }

    // -------------------------------------------------------------------------
    // 7. min / max with Comparator
    // -------------------------------------------------------------------------

    /** Returns the employee with the highest salary. */
    static Optional<Employee> highestPaid(List<Employee> employees) {
        if (employees.isEmpty()) return Optional.empty();
        return Optional.of(Collections.max(employees,
            Comparator.comparingDouble(Employee::salary)));
    }

    static Optional<Employee> lowestPaid(List<Employee> employees) {
        if (employees.isEmpty()) return Optional.empty();
        return Optional.of(Collections.min(employees,
            Comparator.comparingDouble(Employee::salary)));
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void sortDemo() {
        System.out.println("=== Sorting ===");
        List<String> words = List.of("fig", "apple", "kiwi", "date", "banana");
        System.out.println("by length then alpha: " + sortByLengthThenAlpha(words));

        List<Employee> employees = List.of(
            new Employee("Alice", "Eng",  120_000),
            new Employee("Bob",   "HR",    80_000),
            new Employee("Carol", "Eng",  130_000),
            new Employee("Dave",  "HR",    90_000)
        );
        sortEmployees(employees).forEach(e ->
            System.out.printf("  %-6s %-4s %.0f%n", e.name(), e.dept(), e.salary()));
    }

    static void searchDemo() {
        System.out.println("\n=== Binary search ===");
        List<Integer> sorted = List.of(1, 3, 5, 7, 9, 11, 13);
        System.out.println("find 7:  index=" + binarySearch(new ArrayList<>(sorted), 7));
        System.out.println("find 6:  index=" + binarySearch(new ArrayList<>(sorted), 6));
        System.out.println("insert 6 at: " + insertionPoint(new ArrayList<>(sorted), 6));
        System.out.println("insert 0 at: " + insertionPoint(new ArrayList<>(sorted), 0));
        System.out.println("insert 14 at: " + insertionPoint(new ArrayList<>(sorted), 14));
    }

    static void frequencyDemo() {
        System.out.println("\n=== Frequency / disjoint ===");
        List<String> words = List.of("a", "b", "a", "c", "a", "b");
        System.out.println("freq(a): " + frequency(words, "a"));
        System.out.println("disjoint([1,2],[3,4]): " + disjoint(List.of(1,2), List.of(3,4)));
        System.out.println("disjoint([1,2],[2,3]): " + disjoint(List.of(1,2), List.of(2,3)));
    }

    static void viewsDemo() {
        System.out.println("\n=== Unmodifiable view (still reflects orignal changes!) ===");
        LiveScoreboard board = new LiveScoreboard();
        List<Integer> pub = board.publicView();
        board.addScore(10);
        board.addScore(20);
        System.out.println("view sees live changes: " + pub);
        try { pub.add(30); }
        catch (UnsupportedOperationException e) { System.out.println("write blocked: " + e.getClass().getSimpleName()); }
    }

    static void utilsDemo() {
        System.out.println("\n=== Utility operations ===");
        System.out.println("repeat(0,5): " + repeat(0, 5));
        System.out.println("rotateRight([1..5],2): " + rotateRight(List.of(1,2,3,4,5), 2));
        List<Integer> r = new ArrayList<>(List.of(1,2,3,4,5));
        System.out.println("reverseInPlace: " + reverseInPlace(r));
        System.out.println("shuffle(seed=42): " + deterministicShuffle(List.of(1,2,3,4,5), 42));
    }

    public static void main(String[] args) {
        sortDemo();
        searchDemo();
        frequencyDemo();
        viewsDemo();
        utilsDemo();
    }
}
