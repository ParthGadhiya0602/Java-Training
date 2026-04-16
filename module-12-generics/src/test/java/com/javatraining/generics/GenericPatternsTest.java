package com.javatraining.generics;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GenericPatternsTest {

    // ── Result<T> ─────────────────────────────────────────────────────────────

    @Nested
    class ResultTests {

        @Test
        void success_isSuccess() {
            assertTrue(GenericPatterns.Result.success("ok").isSuccess());
            assertFalse(GenericPatterns.Result.success("ok").isFailure());
        }

        @Test
        void failure_isFailure() {
            assertTrue(GenericPatterns.Result.failure("err").isFailure());
            assertFalse(GenericPatterns.Result.failure("err").isSuccess());
        }

        @Test
        void getOrThrow_on_success() {
            assertEquals(42, GenericPatterns.Result.success(42).getOrThrow());
        }

        @Test
        void getOrThrow_on_failure_throws() {
            assertThrows(RuntimeException.class,
                () -> GenericPatterns.Result.failure("oops").getOrThrow());
        }

        @Test
        void getOrElse_returns_value_on_success() {
            assertEquals("hi", GenericPatterns.Result.success("hi").getOrElse("default"));
        }

        @Test
        void getOrElse_returns_fallback_on_failure() {
            assertEquals("default",
                GenericPatterns.Result.failure("err").getOrElse("default"));
        }

        @Test
        void map_transforms_success() {
            var result = GenericPatterns.Result.success(5).map(n -> n * 2);
            assertTrue(result.isSuccess());
            assertEquals(10, result.getOrThrow());
        }

        @Test
        void map_on_failure_passes_through() {
            var result = GenericPatterns.Result.<Integer>failure("err").map(n -> n * 2);
            assertTrue(result.isFailure());
        }

        @Test
        void of_captures_exception_as_failure() {
            var result = GenericPatterns.Result.of(() -> Integer.parseInt("abc"));
            assertTrue(result.isFailure());
        }

        @Test
        void of_wraps_successful_result() {
            var result = GenericPatterns.Result.of(() -> Integer.parseInt("42"));
            assertEquals(42, result.getOrThrow());
        }

        @Test
        void flatMap_chains_successes() {
            var result = GenericPatterns.Result.success("10")
                .flatMap(s -> GenericPatterns.Result.of(() -> Integer.parseInt(s)))
                .map(n -> n + 5);
            assertEquals(15, result.getOrThrow());
        }

        @Test
        void flatMap_short_circuits_on_failure() {
            var result = GenericPatterns.Result.<String>failure("first error")
                .flatMap(s -> GenericPatterns.Result.success(s.length()));
            assertTrue(result.isFailure());
        }
    }

    // ── LruCache ──────────────────────────────────────────────────────────────

    @Nested
    class LruCacheTests {

        @Test
        void get_returns_present_value() {
            GenericPatterns.LruCache<String, Integer> cache = new GenericPatterns.LruCache<>(5);
            cache.put("a", 1);
            assertEquals(Optional.of(1), cache.get("a"));
        }

        @Test
        void get_absent_returns_empty() {
            GenericPatterns.LruCache<String, Integer> cache = new GenericPatterns.LruCache<>(5);
            assertTrue(cache.get("missing").isEmpty());
        }

        @Test
        void evicts_lru_when_over_capacity() {
            GenericPatterns.LruCache<String, Integer> cache = new GenericPatterns.LruCache<>(2);
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3);  // evicts "a" (least recently used)
            assertFalse(cache.containsKey("a"));
            assertTrue(cache.containsKey("b"));
            assertTrue(cache.containsKey("c"));
        }

        @Test
        void access_updates_recency() {
            GenericPatterns.LruCache<String, Integer> cache = new GenericPatterns.LruCache<>(2);
            cache.put("a", 1);
            cache.put("b", 2);
            cache.get("a");    // "a" is now most recently used
            cache.put("c", 3); // evicts "b" (LRU), not "a"
            assertTrue(cache.containsKey("a"));
            assertFalse(cache.containsKey("b"));
        }

        @Test
        void computeIfAbsent_caches_result() {
            GenericPatterns.LruCache<String, Integer> cache = new GenericPatterns.LruCache<>(5);
            int result = cache.computeIfAbsent("key", String::length);
            assertEquals(3, result);
            assertEquals(Optional.of(3), cache.get("key"));
        }

        @Test
        void zero_capacity_throws() {
            assertThrows(IllegalArgumentException.class,
                () -> new GenericPatterns.LruCache<>(0));
        }
    }

    // ── TypeSafeMap ───────────────────────────────────────────────────────────

    @Nested
    class TypeSafeMapTests {

        @Test
        void put_and_get_typed_value() {
            GenericPatterns.TypeSafeMap map = new GenericPatterns.TypeSafeMap();
            map.put(String.class, "hello");
            assertEquals(Optional.of("hello"), map.get(String.class));
        }

        @Test
        void get_absent_type_returns_empty() {
            GenericPatterns.TypeSafeMap map = new GenericPatterns.TypeSafeMap();
            assertTrue(map.get(String.class).isEmpty());
        }

        @Test
        void stores_multiple_types() {
            GenericPatterns.TypeSafeMap map = new GenericPatterns.TypeSafeMap();
            map.put(String.class,  "text");
            map.put(Integer.class, 42);
            map.put(Double.class,  3.14);
            assertEquals(3, map.size());
            assertEquals(Optional.of("text"), map.get(String.class));
            assertEquals(Optional.of(42),     map.get(Integer.class));
        }

        @Test
        void contains_present_type()  {
            GenericPatterns.TypeSafeMap map = new GenericPatterns.TypeSafeMap();
            map.put(Long.class, 1L);
            assertTrue(map.contains(Long.class));
        }

        @Test
        void remove_deletes_entry() {
            GenericPatterns.TypeSafeMap map = new GenericPatterns.TypeSafeMap();
            map.put(String.class, "x");
            map.remove(String.class);
            assertFalse(map.contains(String.class));
            assertEquals(0, map.size());
        }
    }

    // ── Pipeline ──────────────────────────────────────────────────────────────

    @Nested
    class PipelineTests {

        @Test
        void single_step_pipeline() {
            GenericPatterns.Pipeline<String, Integer> p =
                GenericPatterns.Pipeline.of(String::length);
            assertEquals(5, p.execute("hello"));
        }

        @Test
        void chained_steps() {
            GenericPatterns.Pipeline<String, String> p =
                GenericPatterns.Pipeline.of(String::trim)
                    .andThen(String::toUpperCase);
            assertEquals("HELLO", p.execute("  hello  "));
        }

        @Test
        void multi_step_pipeline() {
            GenericPatterns.Pipeline<String, Integer> wordCount =
                GenericPatterns.Pipeline.of(String::trim)
                    .andThen(s -> s.split("\\s+"))
                    .andThen(arr -> arr.length);
            assertEquals(3, wordCount.execute("  a b c  "));
        }

        @Test
        void executeSafe_wraps_success_in_result() {
            GenericPatterns.Pipeline<String, Integer> p =
                GenericPatterns.Pipeline.of(Integer::parseInt);
            assertTrue(p.executeSafe("42").isSuccess());
            assertEquals(42, p.executeSafe("42").getOrThrow());
        }

        @Test
        void executeSafe_wraps_exception_in_failure() {
            GenericPatterns.Pipeline<String, Integer> p =
                GenericPatterns.Pipeline.of(Integer::parseInt);
            assertTrue(p.executeSafe("not-a-number").isFailure());
        }

        @Test
        void identity_pipeline_returns_input() {
            GenericPatterns.Pipeline<String, String> p = GenericPatterns.Pipeline.identity();
            assertEquals("same", p.execute("same"));
        }
    }
}
