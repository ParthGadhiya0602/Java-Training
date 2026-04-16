package com.javatraining.generics;

import java.util.*;
import java.util.function.*;

/**
 * TOPIC: Real-world generic patterns
 *
 * Pattern 1 — Result<T>
 *   A typed union of Success<T> and Failure<T>.  Avoids checked exceptions
 *   in pipeline code by making the error an explicit value.
 *   (Mirrors Rust's Result, Scala's Either, Haskell's Either.)
 *
 * Pattern 2 — Generic LRU Cache
 *   A fixed-capacity cache that evicts the least-recently-used entry.
 *   Uses LinkedHashMap with accessOrder=true.
 *
 * Pattern 3 — Type-safe heterogeneous container
 *   A map that can store values of different types in the SAME container
 *   while remaining fully type-safe.  Each entry's key carries its Class<T>
 *   token, which acts as the type guarantee.
 *   (Joshua Bloch, Effective Java Item 33.)
 *
 * Pattern 4 — Generic pipeline
 *   A chain of Function<I,O> steps where each step's output type is the
 *   next step's input type.  Built with a fluent API using bounded wildcards.
 */
public class GenericPatterns {

    // -------------------------------------------------------------------------
    // Pattern 1 — Result<T>
    // -------------------------------------------------------------------------
    sealed interface Result<T> permits Result.Success, Result.Failure {

        record Success<T>(T value) implements Result<T> {}
        record Failure<T>(String error, Throwable cause) implements Result<T> {
            Failure(String error) { this(error, null); }
        }

        static <T> Result<T> success(T value)  { return new Success<>(value); }
        static <T> Result<T> failure(String msg){ return new Failure<>(msg); }
        static <T> Result<T> failure(String msg, Throwable t){ return new Failure<>(msg, t); }

        /** Wraps a throwing supplier — any exception becomes a Failure. */
        static <T> Result<T> of(ThrowingSupplier<T> supplier) {
            try {
                return success(supplier.get());
            } catch (Exception e) {
                return failure(e.getMessage(), e);
            }
        }

        default boolean isSuccess() { return this instanceof Success<T>; }
        default boolean isFailure() { return this instanceof Failure<T>; }

        default T getOrThrow() {
            return switch (this) {
                case Success<T>  s -> s.value();
                case Failure<T>  f -> throw new RuntimeException(f.error(), f.cause());
            };
        }

        default T getOrElse(T fallback) {
            return switch (this) {
                case Success<T> s -> s.value();
                case Failure<T> f -> fallback;
            };
        }

        /** Transforms the value inside Success; Failure passes through unchanged. */
        default <U> Result<U> map(Function<T, U> fn) {
            return switch (this) {
                case Success<T>  s -> Result.of(() -> fn.apply(s.value()));
                case Failure<T>  f -> new Failure<>(f.error(), f.cause());
            };
        }

        /** Chains a Result-returning function (flatMap / bind). */
        default <U> Result<U> flatMap(Function<T, Result<U>> fn) {
            return switch (this) {
                case Success<T>  s -> fn.apply(s.value());
                case Failure<T>  f -> new Failure<>(f.error(), f.cause());
            };
        }

        @FunctionalInterface
        interface ThrowingSupplier<T> {
            T get() throws Exception;
        }
    }

    // -------------------------------------------------------------------------
    // Pattern 2 — Generic LRU Cache
    // -------------------------------------------------------------------------
    static final class LruCache<K, V> {
        private final int capacity;
        private final LinkedHashMap<K, V> map;

        LruCache(int capacity) {
            if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
            this.capacity = capacity;
            this.map = new LinkedHashMap<>(capacity, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > capacity;
                }
            };
        }

        /** Stores a key-value pair; evicts least-recently-used if over capacity. */
        void put(K key, V value) { map.put(key, value); }

        /** Returns the value, or empty if not present; marks entry as recently used. */
        Optional<V> get(K key)  { return Optional.ofNullable(map.get(key)); }

        boolean containsKey(K key) { return map.containsKey(key); }
        int     size()             { return map.size(); }

        /** Compute-and-cache: returns cached value or calls loader and caches result. */
        V computeIfAbsent(K key, Function<K, V> loader) {
            return map.computeIfAbsent(key, loader);
        }

        /** Returns an unmodifiable snapshot of the current entries (LRU first). */
        Map<K, V> snapshot() { return Collections.unmodifiableMap(new LinkedHashMap<>(map)); }
    }

    // -------------------------------------------------------------------------
    // Pattern 3 — Type-safe heterogeneous container
    // -------------------------------------------------------------------------
    static final class TypeSafeMap {
        private final Map<Class<?>, Object> store = new LinkedHashMap<>();

        /** Stores a value associated with its class token as the key. */
        <T> void put(Class<T> type, T value) {
            store.put(type, type.cast(value));
        }

        /** Retrieves the value for the given type, or empty if absent. */
        <T> Optional<T> get(Class<T> type) {
            return Optional.ofNullable(type.cast(store.get(type)));
        }

        <T> boolean contains(Class<T> type) { return store.containsKey(type); }
        int         size()                  { return store.size(); }

        /** Removes the value for the given type. */
        <T> void remove(Class<T> type) { store.remove(type); }
    }

    // -------------------------------------------------------------------------
    // Pattern 4 — Generic pipeline
    //    Pipeline<I,O> is a chain of transformation steps.
    //    andThen() extends the chain with a new step, tracking both input
    //    and output types.
    // -------------------------------------------------------------------------
    static final class Pipeline<I, O> {
        private final Function<I, O> fn;

        private Pipeline(Function<I, O> fn) { this.fn = fn; }

        static <T> Pipeline<T, T> identity() { return new Pipeline<>(Function.identity()); }
        static <I, O> Pipeline<I, O> of(Function<I, O> fn) { return new Pipeline<>(fn); }

        /** Appends a step; returns a new Pipeline whose output type is R. */
        <R> Pipeline<I, R> andThen(Function<O, R> next) {
            return new Pipeline<>(fn.andThen(next));
        }

        O execute(I input) { return fn.apply(input); }

        /** Executes the pipeline and wraps the result in Result<O>. */
        Result<O> executeSafe(I input) { return Result.of(() -> fn.apply(input)); }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void resultDemo() {
        System.out.println("=== Result<T> ===");

        Result<Integer> ok  = Result.success(42);
        Result<Integer> err = Result.failure("not found");

        System.out.println("ok.isSuccess:  " + ok.isSuccess());
        System.out.println("err.isFailure: " + err.isFailure());
        System.out.println("ok.getOrElse:  " + ok.getOrElse(-1));
        System.out.println("err.getOrElse: " + err.getOrElse(-1));

        // map chains over success
        Result<String> mapped = ok.map(n -> "value=" + n);
        System.out.println("mapped: " + ((Result.Success<String>) mapped).value());

        // map on failure passes through
        Result<String> failMapped = err.map(n -> "value=" + n);
        System.out.println("fail.map still failure: " + failMapped.isFailure());

        // Result.of captures exceptions
        Result<Integer> parsed = Result.of(() -> Integer.parseInt("abc"));
        System.out.println("of(parseInt(\"abc\")): " + ((Result.Failure<Integer>) parsed).error());

        // flatMap chaining
        Result<String> chain = Result.success("42")
            .flatMap(s -> Result.of(() -> Integer.parseInt(s)))
            .map(n -> n * 2)
            .map(n -> "answer=" + n);
        System.out.println("flatMap chain: " + chain.getOrElse("error"));
    }

    static void lruCacheDemo() {
        System.out.println("\n=== LruCache<K,V> ===");
        LruCache<String, Integer> cache = new LruCache<>(3);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        System.out.println("after 3 puts: " + cache.snapshot().keySet());

        cache.get("a");          // access "a" → moves to MRU
        cache.put("d", 4);       // evicts LRU ("b") because capacity=3
        System.out.println("after access(a)+put(d): " + cache.snapshot().keySet());
        System.out.println("b present: " + cache.containsKey("b"));
        System.out.println("a present: " + cache.containsKey("a"));

        // computeIfAbsent
        int val = cache.computeIfAbsent("e", k -> k.length() * 10);
        System.out.println("computeIfAbsent(e): " + val);
    }

    static void typeSafeMapDemo() {
        System.out.println("\n=== TypeSafeMap ===");
        TypeSafeMap ctx = new TypeSafeMap();
        ctx.put(String.class,  "hello");
        ctx.put(Integer.class, 42);
        ctx.put(Double.class,  3.14);

        System.out.println("String:  " + ctx.get(String.class).orElse("?"));
        System.out.println("Integer: " + ctx.get(Integer.class).orElse(-1));
        System.out.println("Double:  " + ctx.get(Double.class).orElse(0.0));
        System.out.println("Long:    " + ctx.get(Long.class).isEmpty());  // absent

        ctx.remove(Integer.class);
        System.out.println("after remove Integer, size: " + ctx.size());
    }

    static void pipelineDemo() {
        System.out.println("\n=== Pipeline<I,O> ===");

        Pipeline<String, Integer> wordCount =
            Pipeline.of(String::trim)
                    .andThen(s -> s.split("\\s+"))
                    .andThen(arr -> arr.length);

        System.out.println("word count: " + wordCount.execute("  hello world foo  "));

        Pipeline<String, String> normalize =
            Pipeline.of(String::trim)
                    .andThen(String::toLowerCase)
                    .andThen(s -> s.replaceAll("\\s+", "_"));

        System.out.println("normalize:  " + normalize.execute("  Hello World  "));

        // executeSafe wraps exceptions in Result
        Pipeline<String, Integer> parseNum = Pipeline.of(Integer::parseInt);
        System.out.println("safe(\"42\"):  " + parseNum.executeSafe("42").getOrElse(-1));
        System.out.println("safe(\"bad\"): " + parseNum.executeSafe("bad").isFailure());
    }

    public static void main(String[] args) {
        resultDemo();
        lruCacheDemo();
        typeSafeMapDemo();
        pipelineDemo();
    }
}
