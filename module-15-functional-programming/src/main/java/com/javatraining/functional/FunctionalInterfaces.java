package com.javatraining.functional;

import java.util.List;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Module 15 — Functional Interfaces and Composition
 *
 * java.util.function provides four core families:
 *
 *   Function<T,R>      T -> R              transform
 *   Predicate<T>       T -> boolean        test
 *   Consumer<T>        T -> void           side-effect
 *   Supplier<T>        () -> T             produce
 *
 * Each has two-arity (BiFunction, BiPredicate, BiConsumer) and primitive
 * specialisations (IntFunction, LongPredicate, DoubleConsumer, etc.).
 *
 * Composition methods let you build complex behaviour from simple parts
 * without writing new classes or lambdas.
 */
public class FunctionalInterfaces {

    // ── Function<T,R> composition ─────────────────────────────────────────────

    /**
     * andThen: apply f, then g on the result.
     * compose: apply g first, then f on the result (reverse of andThen).
     *
     * f.andThen(g)  ≡  x -> g.apply(f.apply(x))
     * f.compose(g)  ≡  x -> f.apply(g.apply(x))
     */
    public static Function<String, String> trimThenUpper() {
        Function<String, String> trim  = String::trim;
        Function<String, String> upper = String::toUpperCase;
        return trim.andThen(upper);
    }

    public static Function<Integer, Integer> squareThenDouble() {
        Function<Integer, Integer> square   = x -> x * x;
        Function<Integer, Integer> doubleFn = x -> x * 2;
        return square.andThen(doubleFn);
    }

    public static <T> Function<T, T> identityFn() {
        return Function.identity();
    }

    // ── Predicate<T> composition ──────────────────────────────────────────────

    /**
     * and  — short-circuit &&
     * or   — short-circuit ||
     * negate — !
     */
    public static Predicate<String> nonBlankAndLong(int minLen) {
        Predicate<String> nonBlank    = Predicate.not(String::isBlank);
        Predicate<String> longEnough  = s -> s.length() >= minLen;
        return nonBlank.and(longEnough);
    }

    public static Predicate<Integer> zeroOrNegative() {
        Predicate<Integer> isZero     = n -> n == 0;
        Predicate<Integer> isNegative = n -> n < 0;
        return isZero.or(isNegative);
    }

    public static Predicate<String> isNotEmpty() {
        return ((Predicate<String>) String::isEmpty).negate();
    }

    public static List<String> filterWords(List<String> words, int minLen) {
        return words.stream()
                    .filter(nonBlankAndLong(minLen))
                    .collect(Collectors.toList());
    }

    // ── Consumer<T> composition ───────────────────────────────────────────────

    /**
     * Consumer.andThen chains consumers: c1 runs first, then c2, on the same value.
     */
    public static Consumer<String> logAndStore(List<String> store) {
        Consumer<String> log    = s -> System.out.println("[LOG] " + s);
        Consumer<String> storeC = store::add;
        return log.andThen(storeC);
    }

    // ── Supplier<T> ───────────────────────────────────────────────────────────

    /** Lazy default: supplier is only called when value is null. */
    public static String getOrCompute(String value, Supplier<String> defaultSupplier) {
        return value != null ? value : defaultSupplier.get();
    }

    // ── BiFunction<T,U,R> ────────────────────────────────────────────────────

    /** BiFunction then andThen to post-process the result. */
    public static BiFunction<String, Integer, String> repeatAndUpperCase() {
        BiFunction<String, Integer, String> repeat = String::repeat;
        return repeat.andThen(String::toUpperCase);
    }

    // ── UnaryOperator / BinaryOperator ────────────────────────────────────────

    public static UnaryOperator<String> wrapInBrackets() {
        return s -> "[" + s + "]";
    }

    public static BinaryOperator<Integer> safeDivide() {
        return (a, b) -> b == 0 ? 0 : a / b;
    }

    // ── Primitive specialisations ────────────────────────────────────────────

    /** IntUnaryOperator — avoids boxing overhead for int -> int. */
    public static IntUnaryOperator clamp(int min, int max) {
        return n -> Math.max(min, Math.min(max, n));
    }

    public static IntPredicate isEven() {
        return n -> n % 2 == 0;
    }

    public static ToIntFunction<String> stringLength() {
        return String::length;
    }

    // ── Custom @FunctionalInterface ───────────────────────────────────────────

    /**
     * Any interface with exactly one abstract method is a functional interface
     * and can be implemented with a lambda. Default methods don't count.
     */
    @FunctionalInterface
    public interface Transformer<A, B> {
        B transform(A input);

        default <C> Transformer<A, C> andThen(Transformer<B, C> after) {
            return input -> after.transform(this.transform(input));
        }
    }

    public static Transformer<String, Integer> wordCount() {
        return s -> s.trim().isEmpty() ? 0 : s.trim().split("\\s+").length;
    }

    public static Transformer<String, String> truncate(int maxLen) {
        return s -> s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
