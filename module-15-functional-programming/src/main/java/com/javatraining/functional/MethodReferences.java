package com.javatraining.functional;

import java.util.Comparator;
import java.util.List;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Module 15 — Method References
 *
 * A method reference is a compact lambda that delegates directly to an
 * existing method.  Four kinds:
 *
 *   Kind                  Syntax                     Equivalent lambda
 *   ──────────────────────────────────────────────────────────────────
 *   Static                ClassName::staticMethod    x -> ClassName.staticMethod(x)
 *   Bound instance        instance::method           x -> instance.method(x)
 *   Unbound instance      ClassName::method          (obj,x) -> obj.method(x)
 *   Constructor           ClassName::new             args -> new ClassName(args)
 */
public class MethodReferences {

    // ── Static method reference ───────────────────────────────────────────────

    public static int doubleIt(int n) { return n * 2; }

    /** IntUnaryOperator backed by a static method reference. */
    public static IntUnaryOperator staticRef() {
        return MethodReferences::doubleIt;        // n -> MethodReferences.doubleIt(n)
    }

    /** Comparator backed by Integer.compare (static). */
    public static Comparator<Integer> naturalOrderComparator() {
        return Integer::compare;                  // (a, b) -> Integer.compare(a, b)
    }

    /** Parse a list of strings to integers using a static method reference. */
    public static List<Integer> parseInts(List<String> strings) {
        return strings.stream()
                      .map(Integer::parseInt)     // static: String -> Integer
                      .collect(Collectors.toList());
    }

    // ── Bound instance method reference ──────────────────────────────────────

    /**
     * Bound: the receiver is captured at reference-creation time.
     * Returns a Supplier that calls sb.toString() on the captured instance.
     */
    public static Supplier<String> boundToStringRef(StringBuilder sb) {
        return sb::toString;                      // () -> sb.toString()
    }

    /** Bound: System.out is the captured receiver. */
    public static Consumer<String> printlnRef() {
        return System.out::println;               // s -> System.out.println(s)
    }

    // ── Unbound instance method reference ────────────────────────────────────

    /**
     * Unbound: receiver is supplied at call time as the first argument.
     * Function<String, String> calls toUpperCase() on whatever String is passed.
     */
    public static Function<String, String> toUpperCaseRef() {
        return String::toUpperCase;               // s -> s.toUpperCase()
    }

    /** Unbound: sort strings by length using String::length. */
    public static List<String> sortByLength(List<String> items) {
        return items.stream()
                    .sorted(Comparator.comparingInt(String::length))
                    .collect(Collectors.toList());
    }

    /**
     * Unbound BiFunction — receiver is first arg, method param is second.
     * (s, sub) -> s.contains(sub)
     */
    public static BiFunction<String, String, Boolean> containsRef() {
        return String::contains;
    }

    // ── Constructor reference ─────────────────────────────────────────────────

    /** Supplier<StringBuilder> — zero-arg constructor reference. */
    public static Supplier<StringBuilder> sbSupplier() {
        return StringBuilder::new;                // () -> new StringBuilder()
    }

    /** Function<String, StringBuilder> — one-arg constructor reference. */
    public static Function<String, StringBuilder> sbFromString() {
        return StringBuilder::new;                // s -> new StringBuilder(s)
    }

    /** IntFunction<int[]> — array constructor reference. */
    public static IntFunction<int[]> intArrayFactory() {
        return int[]::new;                        // n -> new int[n]
    }

    // ── Custom record for composition demo ───────────────────────────────────

    public record Person(String name, int age) {}

    /** Sort by age ascending, then name — method refs throughout. */
    public static List<Person> sortPeople(List<Person> people) {
        return people.stream()
                     .sorted(Comparator.comparingInt(Person::age)
                                       .thenComparing(Person::name))
                     .collect(Collectors.toList());
    }

    // ── Pipeline combining all four kinds ────────────────────────────────────

    /**
     * Parse non-blank strings to integers, double each, collect sorted.
     * Uses static, unbound, and bound method references in one pipeline.
     */
    private static boolean isNumeric(String s) {
        try { Integer.parseInt(s); return true; }
        catch (NumberFormatException e) { return false; }
    }

    public static List<Integer> processNumbers(List<String> raw) {
        return raw.stream()
                  .filter(Predicate.not(String::isBlank))   // unbound
                  .map(String::trim)                         // unbound
                  .filter(MethodReferences::isNumeric)       // static — skip non-numbers
                  .map(Integer::parseInt)                    // static
                  .map(MethodReferences::doubleIt)           // static (this class)
                  .sorted(Integer::compare)                  // static
                  .collect(Collectors.toList());
    }
}
