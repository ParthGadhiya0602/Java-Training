package com.javatraining.functional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Module 15 - Optional<T>
 *
 * Optional is a container that may or may not hold a non-null value.
 * It forces callers to handle the absent case explicitly, eliminating
 * NullPointerExceptions at API boundaries.
 *
 * Key rule: never use Optional.get() without isPresent(). Use the
 * transformation/consumption methods instead.
 */
public class OptionalDemo {

    // ── Creation ─────────────────────────────────────────────────────────────

    /** Returns Optional.empty() when input is null, Optional.of(value) otherwise. */
    public static Optional<String> wrap(String value) {
        return Optional.ofNullable(value);
    }

    /** Safe division - returns empty when divisor is zero. */
    public static Optional<Integer> safeDivide(int a, int b) {
        if (b == 0) return Optional.empty();
        return Optional.of(a / b);
    }

    // ── map / flatMap ─────────────────────────────────────────────────────────

    /**
     * map transforms the value if present, leaving empty untouched.
     * Returns the length of the string, or -1 as a sentinel via orElse.
     */
    public static int lengthOrMinus1(Optional<String> opt) {
        return opt.map(String::length).orElse(-1);
    }

    /**
     * flatMap avoids Optional<Optional<T>> when the mapping function
     * itself returns an Optional.
     */
    public static Optional<Integer> parseIntSafe(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** flatMap chains two Optional-returning operations. */
    public static Optional<Integer> lookupAndParse(Map<String, String> map, String key) {
        return Optional.ofNullable(map.get(key))       // Optional<String>
                       .flatMap(OptionalDemo::parseIntSafe); // Optional<Integer>
    }

    // ── filter ────────────────────────────────────────────────────────────────

    /** Keeps the value only if it satisfies the predicate; empty otherwise. */
    public static Optional<String> nonBlank(String s) {
        return Optional.ofNullable(s)
                       .filter(str -> !str.isBlank());
    }

    /** Returns value only if it is a positive even number. */
    public static Optional<Integer> positiveEven(int n) {
        return Optional.of(n)
                       .filter(v -> v > 0)
                       .filter(v -> v % 2 == 0);
    }

    // ── Terminal: orElse / orElseGet / orElseThrow / ifPresent ───────────────

    /**
     * orElse - eager: the argument is always evaluated even when value is present.
     * Use for cheap defaults (constants, literals).
     */
    public static String orElseDemo(Optional<String> opt, String defaultValue) {
        return opt.orElse(defaultValue);
    }

    /**
     * orElseGet - lazy: supplier called only when empty.
     * Prefer over orElse when default construction is expensive.
     */
    public static String orElseGetDemo(Optional<String> opt) {
        return opt.orElseGet(() -> "generated-default");
    }

    /** orElseThrow - throws when empty; no argument = NoSuchElementException. */
    public static String orElseThrowDemo(Optional<String> opt) {
        return opt.orElseThrow(() -> new IllegalStateException("value required but absent"));
    }

    /** ifPresentOrElse (Java 9+) - one branch for each case. */
    public static String describeOptional(Optional<String> opt) {
        StringBuilder sb = new StringBuilder();
        opt.ifPresentOrElse(
            v  -> sb.append("present: ").append(v),
            () -> sb.append("empty")
        );
        return sb.toString();
    }

    // ── or (Java 9+) ─────────────────────────────────────────────────────────

    /**
     * or - fallback to another Optional when this one is empty.
     * Unlike orElse, the result stays wrapped in Optional.
     */
    public static Optional<String> firstNonEmpty(Optional<String> primary,
                                                  Optional<String> fallback) {
        return primary.or(() -> fallback);
    }

    // ── stream (Java 9+) ─────────────────────────────────────────────────────

    /**
     * Optional.stream() emits 0 or 1 elements, enabling flatMap in stream
     * pipelines to skip empty Optionals.
     */
    public static List<Integer> parseAll(List<String> inputs) {
        return inputs.stream()
                     .map(OptionalDemo::parseIntSafe)   // Stream<Optional<Integer>>
                     .flatMap(Optional::stream)          // Stream<Integer> - skips empties
                     .collect(Collectors.toList());
    }

    // ── Chained pipeline ─────────────────────────────────────────────────────

    /**
     * Real-world pattern: lookup → validate → transform → default.
     * No null checks anywhere in the chain.
     */
    public static String resolveUsername(Map<String, String> profiles, String userId) {
        return Optional.ofNullable(userId)
                       .filter(id -> !id.isBlank())
                       .map(profiles::get)
                       .map(String::trim)
                       .filter(name -> !name.isEmpty())
                       .map(String::toLowerCase)
                       .orElse("anonymous");
    }
}
