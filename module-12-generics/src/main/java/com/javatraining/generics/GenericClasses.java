package com.javatraining.generics;

import java.util.*;
import java.util.function.Function;

/**
 * TOPIC: Generic classes and generic methods
 *
 * A generic class declares one or more type parameters in angle brackets.
 * Those parameters act as placeholders that callers fill in at the use site.
 *
 * Benefits:
 *   • Compile-time type safety — ClassCastException moves from runtime to compile
 *   • Eliminates manual casts
 *   • Enables one implementation to serve many types (parametric polymorphism)
 *
 * Naming conventions (by tradition):
 *   T — general type                 E — element (collections)
 *   K, V — key / value (maps)        R — return type
 *   A, B, C — distinct type params   N — number
 */
public class GenericClasses {

    // -------------------------------------------------------------------------
    // 1. Generic class — Pair<A, B>
    //    Holds two values of potentially different types.
    //    swap() shows a generic method that returns a new type.
    // -------------------------------------------------------------------------
    static final class Pair<A, B> {
        private final A first;
        private final B second;

        Pair(A first, B second) {
            this.first  = Objects.requireNonNull(first,  "first");
            this.second = Objects.requireNonNull(second, "second");
        }

        A first()  { return first; }
        B second() { return second; }

        /** Returns a new Pair with elements swapped. */
        Pair<B, A> swap() { return new Pair<>(second, first); }

        /** Maps both elements to a new Pair via independent functions. */
        <C, D> Pair<C, D> map(Function<A, C> f, Function<B, D> g) {
            return new Pair<>(f.apply(first), g.apply(second));
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pair<?,?> p)) return false;
            return first.equals(p.first) && second.equals(p.second);
        }

        @Override public int hashCode() { return Objects.hash(first, second); }
        @Override public String toString() { return "(" + first + ", " + second + ")"; }
    }

    // -------------------------------------------------------------------------
    // 2. Generic method — operates on a type parameter independent of the class
    //    The type parameter is declared on the METHOD, not the class.
    // -------------------------------------------------------------------------

    /** Returns the first element of the array. Illustrates a standalone generic method. */
    static <T> T first(T[] arr) {
        if (arr == null || arr.length == 0)
            throw new NoSuchElementException("array is empty");
        return arr[0];
    }

    /** Reverses a list in-place; returns the same list for convenience. */
    static <T> List<T> reversed(List<T> list) {
        List<T> copy = new ArrayList<>(list);
        Collections.reverse(copy);
        return copy;
    }

    /** Zips two lists into a list of Pairs. Stops at the shorter list. */
    static <A, B> List<Pair<A, B>> zip(List<A> as, List<B> bs) {
        int size = Math.min(as.size(), bs.size());
        List<Pair<A, B>> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(new Pair<>(as.get(i), bs.get(i)));
        return result;
    }

    // -------------------------------------------------------------------------
    // 3. Bounded type parameter — <T extends Comparable<T>>
    //    Constrains T to types that can be compared to themselves.
    //    Enables use of compareTo() inside the method body.
    // -------------------------------------------------------------------------

    /** Returns the larger of the two arguments. */
    static <T extends Comparable<T>> T max(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    /** Returns the minimum element in a non-empty list. */
    static <T extends Comparable<T>> T min(List<T> list) {
        if (list.isEmpty()) throw new NoSuchElementException("list is empty");
        T m = list.get(0);
        for (T t : list) if (t.compareTo(m) < 0) m = t;
        return m;
    }

    /** Returns the range [min, max] of a list as a Pair. */
    static <T extends Comparable<T>> Pair<T, T> range(List<T> list) {
        if (list.isEmpty()) throw new NoSuchElementException("list is empty");
        T lo = list.get(0), hi = list.get(0);
        for (T t : list) {
            if (t.compareTo(lo) < 0) lo = t;
            if (t.compareTo(hi) > 0) hi = t;
        }
        return new Pair<>(lo, hi);
    }

    // -------------------------------------------------------------------------
    // 4. Multiple bounds — <T extends Number & Comparable<T>>
    //    A type parameter can extend at most one class and multiple interfaces.
    //    The class must come first.
    // -------------------------------------------------------------------------

    /** Sums a list of numbers and returns a double. */
    static <T extends Number & Comparable<T>> double sum(List<T> list) {
        double total = 0;
        for (T t : list) total += t.doubleValue();
        return total;
    }

    /** Returns the median of a sorted (ascending) list. */
    static <T extends Number & Comparable<T>> double median(List<T> list) {
        if (list.isEmpty()) throw new NoSuchElementException("list is empty");
        List<T> sorted = new ArrayList<>(list);
        Collections.sort(sorted);
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 1) return sorted.get(mid).doubleValue();
        return (sorted.get(mid - 1).doubleValue() + sorted.get(mid).doubleValue()) / 2.0;
    }

    // -------------------------------------------------------------------------
    // 5. Generic class with a bounded type parameter
    //    SortedBag<T> keeps elements in natural order.
    // -------------------------------------------------------------------------
    static final class SortedBag<T extends Comparable<T>> {
        private final List<T> items = new ArrayList<>();

        /** Inserts value in sorted position. */
        void add(T value) {
            int i = Collections.binarySearch(items, value);
            items.add(i >= 0 ? i : -(i + 1), value);
        }

        void remove(T value) { items.remove(value); }

        T    min()              { return items.isEmpty() ? null : items.get(0); }
        T    max()              { return items.isEmpty() ? null : items.get(items.size() - 1); }
        int  size()             { return items.size(); }
        boolean contains(T v)   { return Collections.binarySearch(items, v) >= 0; }
        List<T> toList()        { return Collections.unmodifiableList(new ArrayList<>(items)); }

        @Override public String toString() { return items.toString(); }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void pairDemo() {
        System.out.println("=== Pair<A,B> ===");
        Pair<String, Integer> p = new Pair<>("Alice", 42);
        System.out.println(p);
        System.out.println("swapped: " + p.swap());
        System.out.println("mapped:  " + p.map(String::length, n -> n * 2));

        List<String> names  = List.of("Alice", "Bob", "Carol");
        List<Integer> scores = List.of(95, 87, 91);
        System.out.println("zipped:  " + zip(names, scores));
    }

    static void genericMethodsDemo() {
        System.out.println("\n=== Generic methods ===");
        Integer[] nums = {10, 20, 30};
        System.out.println("first: " + first(nums));
        System.out.println("reversed: " + reversed(List.of(1, 2, 3, 4)));
        System.out.println("max(3, 7): " + max(3, 7));
        System.out.println("max(\"apple\",\"mango\"): " + max("apple", "mango"));

        List<Integer> data = List.of(5, 1, 9, 3, 7);
        System.out.println("min: " + min(data));
        System.out.println("range: " + range(data));
    }

    static void multiBoundDemo() {
        System.out.println("\n=== Multiple bounds <T extends Number & Comparable<T>> ===");
        List<Integer> ints    = List.of(3, 1, 4, 1, 5, 9, 2, 6);
        List<Double>  doubles = List.of(1.5, 2.5, 3.0);
        System.out.println("sum(ints):    " + sum(ints));
        System.out.println("median(ints): " + median(ints));
        System.out.println("sum(doubles): " + sum(doubles));
    }

    static void sortedBagDemo() {
        System.out.println("\n=== SortedBag<T extends Comparable<T>> ===");
        SortedBag<Integer> bag = new SortedBag<>();
        bag.add(5); bag.add(2); bag.add(8); bag.add(1); bag.add(5);
        System.out.println("bag:  " + bag);
        System.out.println("min:  " + bag.min());
        System.out.println("max:  " + bag.max());
        System.out.println("has 5: " + bag.contains(5));
        bag.remove(5);
        System.out.println("after remove(5): " + bag);
    }

    public static void main(String[] args) {
        pairDemo();
        genericMethodsDemo();
        multiBoundDemo();
        sortedBagDemo();
    }
}
