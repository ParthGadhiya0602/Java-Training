package com.javatraining.generics;

import java.util.*;

/**
 * TOPIC: Wildcards and the PECS rule
 *
 * A wildcard '?' stands for "some unknown type".
 *
 *   ? (unbounded)     — any type; you can read as Object, cannot write
 *   ? extends T       — some subtype of T (upper-bounded); safe to READ as T
 *   ? super T         — some supertype of T (lower-bounded); safe to WRITE T
 *
 * PECS — Producer Extends, Consumer Super
 * ────────────────────────────────────────
 *   If a parameter PRODUCES values you will read   → ? extends T
 *   If a parameter CONSUMES values you will write  → ? super T
 *   If a parameter does both                       → T (exact type)
 *
 * Examples from the JDK:
 *   Collections.copy(List<? super T> dest, List<? extends T> src)
 *   Collections.sort(List<T>, Comparator<? super T>)
 */
public class Wildcards {

    // -------------------------------------------------------------------------
    // 1. Unbounded wildcard — List<?>
    //    Use when you only need operations that work on any List regardless
    //    of element type (e.g., size, isEmpty, printing).
    //    You CANNOT add anything except null.
    // -------------------------------------------------------------------------

    /** Returns the number of null elements in any list. */
    static int countNulls(List<?> list) {
        int count = 0;
        for (Object o : list) if (o == null) count++;
        return count;
    }

    /** Prints every element to stdout using Object.toString(). */
    static void printAll(List<?> list) {
        list.forEach(e -> System.out.print(e + " "));
        System.out.println();
    }

    /** Returns true if the two lists have the same size. */
    static boolean sameSize(List<?> a, List<?> b) {
        return a.size() == b.size();
    }

    // -------------------------------------------------------------------------
    // 2. Upper-bounded wildcard — List<? extends Number>
    //    The list PRODUCES numbers; we can safely READ each element as Number.
    //    We CANNOT add (except null) because we don't know the exact subtype.
    // -------------------------------------------------------------------------

    /** Sums all numbers in a list of any Number subtype (Integer, Double, …). */
    static double sumNumbers(List<? extends Number> list) {
        double total = 0;
        for (Number n : list) total += n.doubleValue();   // safe: every element IS-A Number
        return total;
    }

    /** Returns the maximum element from a list of Comparables. */
    static <T extends Comparable<T>> T maxOf(List<? extends T> list) {
        if (list.isEmpty()) throw new NoSuchElementException("list is empty");
        T best = list.get(0);
        for (T t : list) if (t.compareTo(best) > 0) best = t;
        return best;
    }

    /** Copies all elements from src into a new ArrayList. */
    static <T> List<T> copyOf(List<? extends T> src) {
        return new ArrayList<>(src);
    }

    // -------------------------------------------------------------------------
    // 3. Lower-bounded wildcard — List<? super Integer>
    //    The list CONSUMES integers; we can safely WRITE Integer values.
    //    We can only read elements as Object (unknown exact supertype).
    // -------------------------------------------------------------------------

    /** Fills dst with the integers [from, to] inclusive. */
    static void fillRange(List<? super Integer> dst, int from, int to) {
        for (int i = from; i <= to; i++) dst.add(i);   // safe: Integer IS-A ? super Integer
    }

    /** Appends all elements from src into dst. Classic PECS signature. */
    static <T> void copy(List<? extends T> src, List<? super T> dst) {
        dst.addAll(src);
    }

    // -------------------------------------------------------------------------
    // 4. PECS in action — a Stack that uses both wildcards
    // -------------------------------------------------------------------------
    static final class Stack<T> {
        private final Deque<T> data = new ArrayDeque<>();

        void push(T value)  { data.push(value); }
        T    pop()          { return data.pop(); }
        T    peek()         { return data.peek(); }
        boolean isEmpty()   { return data.isEmpty(); }
        int     size()      { return data.size(); }

        /** Pushes all elements from the producer list onto this stack (PECS: extends). */
        void pushAll(List<? extends T> producer) {
            for (T t : producer) push(t);
        }

        /** Drains this stack into the consumer list (PECS: super). */
        void drainTo(List<? super T> consumer) {
            while (!isEmpty()) consumer.add(pop());
        }

        /** Returns a snapshot list (top of stack at index 0). */
        List<T> toList() { return new ArrayList<>(data); }
    }

    // -------------------------------------------------------------------------
    // 5. Wildcard capture — helper method pattern
    //    Swap two elements inside a List<?>; the compiler needs a named type
    //    to allow the assignment, so we delegate to a private <T> helper.
    // -------------------------------------------------------------------------

    /** Swaps the elements at positions i and j in any typed list. */
    static void swap(List<?> list, int i, int j) {
        swapHelper(list, i, j);
    }

    private static <T> void swapHelper(List<T> list, int i, int j) {
        T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void unboundedDemo() {
        System.out.println("=== Unbounded wildcard List<?> ===");
        List<String>  strings  = Arrays.asList("a", null, "b", null, "c");
        List<Integer> integers = List.of(1, 2, 3);

        System.out.println("nulls in strings: " + countNulls(strings));
        System.out.print ("printAll integers: "); printAll(integers);
        System.out.println("sameSize:         " + sameSize(strings, integers));
    }

    static void upperBoundedDemo() {
        System.out.println("\n=== Upper-bounded ? extends Number ===");
        List<Integer> ints    = List.of(1, 2, 3, 4, 5);
        List<Double>  doubles = List.of(1.5, 2.5, 3.0);

        System.out.println("sum(ints):    " + sumNumbers(ints));
        System.out.println("sum(doubles): " + sumNumbers(doubles));
        System.out.println("max(ints):    " + maxOf(ints));

        List<Number> dest = new ArrayList<>();
        copy(ints, dest);    // Integer IS-A Number → ? extends Number satisfies ? super Number
        System.out.println("copy to List<Number>: " + dest);
    }

    static void lowerBoundedDemo() {
        System.out.println("\n=== Lower-bounded ? super Integer ===");
        List<Integer> exactInts   = new ArrayList<>();
        List<Number>  numberList  = new ArrayList<>();
        List<Object>  objectList  = new ArrayList<>();

        fillRange(exactInts,  1, 3);
        fillRange(numberList, 4, 6);
        fillRange(objectList, 7, 9);

        System.out.println("List<Integer>: " + exactInts);
        System.out.println("List<Number>:  " + numberList);
        System.out.println("List<Object>:  " + objectList);
    }

    static void pecsDemo() {
        System.out.println("\n=== PECS — Stack.pushAll / drainTo ===");
        Stack<Number> stack = new Stack<>();

        List<Integer> ints    = List.of(1, 2, 3);
        List<Double>  doubles = List.of(4.5, 5.5);

        stack.pushAll(ints);    // List<Integer> satisfies List<? extends Number>
        stack.pushAll(doubles); // List<Double>  satisfies List<? extends Number>
        System.out.println("stack after pushAll: " + stack.toList());

        List<Object> sink = new ArrayList<>();
        stack.drainTo(sink);    // List<Object> satisfies List<? super Number>
        System.out.println("drained to List<Object>: " + sink);
    }

    static void swapDemo() {
        System.out.println("\n=== Wildcard capture — swap ===");
        List<String> words = new ArrayList<>(List.of("apple", "banana", "cherry"));
        swap(words, 0, 2);
        System.out.println("after swap(0,2): " + words);
    }

    public static void main(String[] args) {
        unboundedDemo();
        upperBoundedDemo();
        lowerBoundedDemo();
        pecsDemo();
        swapDemo();
    }
}
