package com.javatraining.functional;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Module 15 - Functional Patterns
 *
 * Higher-order functions, currying, memoization, and the Strategy/Decorator
 * patterns expressed through functional interfaces rather than class hierarchies.
 */
public class FunctionalPatterns {

    // ── Higher-order functions ────────────────────────────────────────────────

    /**
     * applyTwice: applies f to x, then applies f to the result.
     * Demonstrates a function that accepts and returns a function.
     */
    public static <T> UnaryOperator<T> applyTwice(UnaryOperator<T> f) {
        return x -> f.apply(f.apply(x));
    }

    /**
     * Compose a list of functions left-to-right into a single function.
     * Empty list returns identity.
     */
    public static <T> UnaryOperator<T> pipeline(List<UnaryOperator<T>> steps) {
        return steps.stream()
                    .reduce(UnaryOperator.identity(),
                            (f, g) -> x -> g.apply(f.apply(x)));
    }

    // ── Currying ─────────────────────────────────────────────────────────────

    /**
     * Convert a two-argument function into a chain of single-argument functions.
     * BiFunction<A,B,C>  →  Function<A, Function<B, C>>
     */
    public static <A, B, C> Function<A, Function<B, C>> curry(BiFunction<A, B, C> f) {
        return a -> b -> f.apply(a, b);
    }

    /** Fix the first argument; return a Function for the second. */
    public static <A, B, C> Function<B, C> partial(BiFunction<A, B, C> f, A firstArg) {
        return b -> f.apply(firstArg, b);
    }

    /** Practical curried adder - returns a reusable Function<Integer,Integer>. */
    public static Function<Integer, Integer> adder(int addend) {
        return partial(Integer::sum, addend);
    }

    // ── Memoization ──────────────────────────────────────────────────────────

    /**
     * Wrap a Function with a HashMap cache so each unique input is computed
     * at most once. Not thread-safe - use ConcurrentHashMap for shared caches.
     */
    public static <T, R> Function<T, R> memoize(Function<T, R> fn) {
        Map<T, R> cache = new HashMap<>();
        return input -> cache.computeIfAbsent(input, fn);
    }

    /**
     * Memoized Fibonacci.
     * Uses explicit get/put rather than computeIfAbsent to avoid
     * ConcurrentModificationException from recursive map modification (Java 9+).
     */
    public static long fibMemo(int n) {
        Map<Integer, Long> cache = new HashMap<>();
        return fibHelper(n, cache);
    }

    private static long fibHelper(int n, Map<Integer, Long> cache) {
        if (n <= 1) return n;
        Long cached = cache.get(n);
        if (cached != null) return cached;
        long result = fibHelper(n - 1, cache) + fibHelper(n - 2, cache);
        cache.put(n, result);
        return result;
    }

    // ── Strategy pattern via functional interface ─────────────────────────────

    public record Order(String id, double amount, String customerTier) {}

    public static final Function<Order, Double> NO_DISCOUNT         = o -> o.amount();
    public static final Function<Order, Double> TEN_PCT_DISCOUNT    = o -> o.amount() * 0.90;
    public static final Function<Order, Double> TWENTY_PCT_DISCOUNT = o -> o.amount() * 0.80;

    public static Function<Order, Double> discountStrategy(String tier) {
        return switch (tier) {
            case "GOLD"   -> TWENTY_PCT_DISCOUNT;
            case "SILVER" -> TEN_PCT_DISCOUNT;
            default       -> NO_DISCOUNT;
        };
    }

    public static double applyDiscount(Order order) {
        return discountStrategy(order.customerTier()).apply(order);
    }

    // ── Decorator pattern via function composition ────────────────────────────

    /** Wrap a function with logging before/after without changing it. */
    public static <T, R> Function<T, R> withLogging(Function<T, R> fn, String label) {
        return input -> {
            System.out.println("[" + label + "] input=" + input);
            R result = fn.apply(input);
            System.out.println("[" + label + "] output=" + result);
            return result;
        };
    }

    /** Wrap a function with retry on RuntimeException (up to maxAttempts). */
    public static <T, R> Function<T, R> withRetry(Function<T, R> fn, int maxAttempts) {
        return input -> {
            RuntimeException last = null;
            for (int i = 0; i < maxAttempts; i++) {
                try { return fn.apply(input); }
                catch (RuntimeException e) { last = e; }
            }
            throw last;
        };
    }

    // ── Validation combinator ─────────────────────────────────────────────────

    /**
     * Collects all validation failures rather than short-circuiting on the first.
     * Returns empty list on success, list of error messages on failure.
     */
    @FunctionalInterface
    public interface Validator<T> {
        List<String> validate(T value);

        default Validator<T> and(Validator<T> other) {
            return value -> {
                List<String> errors = new ArrayList<>(this.validate(value));
                errors.addAll(other.validate(value));
                return errors;
            };
        }
    }

    public static Validator<String> minLength(int min) {
        return s -> s.length() >= min
                ? Collections.emptyList()
                : List.of("must be at least " + min + " characters");
    }

    public static Validator<String> noSpaces() {
        return s -> !s.contains(" ")
                ? Collections.emptyList()
                : List.of("must not contain spaces");
    }

    public static Validator<String> passwordValidator() {
        return minLength(8).and(noSpaces());
    }

    // ── Lazy evaluation with Supplier ────────────────────────────────────────

    /**
     * Lazy<T> wraps a Supplier and caches its result after the first call.
     * Equivalent to a lazy field initializer.
     */
    public static class Lazy<T> {
        private final Supplier<T> supplier;
        private T value;
        private boolean computed = false;

        public Lazy(Supplier<T> supplier) { this.supplier = supplier; }

        public T get() {
            if (!computed) {
                value = supplier.get();
                computed = true;
            }
            return value;
        }

        public static <T> Lazy<T> of(Supplier<T> supplier) {
            return new Lazy<>(supplier);
        }
    }
}
