package com.javatraining.concurrency;

import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ForkJoinDemo")
class ForkJoinDemoTest {

    @Nested
    @DisplayName("Parallel sum")
    class Sum {
        @Test void small_array_sum() {
            long[] arr = {1, 2, 3, 4, 5};
            assertEquals(15L, ForkJoinDemo.parallelSum(arr));
        }

        @Test void large_array_sum_matches_sequential() {
            long[] arr = new long[100_000];
            Arrays.fill(arr, 1L);
            assertEquals(100_000L, ForkJoinDemo.parallelSum(arr));
        }

        @Test void single_element_sum() {
            assertEquals(42L, ForkJoinDemo.parallelSum(new long[]{42}));
        }

        @Test void async_sum_matches_sync() throws Exception {
            long[] arr = new long[50_000];
            Arrays.fill(arr, 2L);
            assertEquals(100_000L, ForkJoinDemo.asyncSum(arr));
        }
    }

    @Nested
    @DisplayName("Parallel max")
    class Max {
        @Test void finds_max_in_unsorted_array() {
            long[] arr = {3, 1, 4, 1, 5, 9, 2, 6};
            assertEquals(9L, ForkJoinDemo.parallelMax(arr));
        }

        @Test void large_array_max() {
            long[] arr = new long[100_000];
            Arrays.fill(arr, 1L);
            arr[77_777] = 999L;
            assertEquals(999L, ForkJoinDemo.parallelMax(arr));
        }

        @Test void single_element_max() {
            assertEquals(7L, ForkJoinDemo.parallelMax(new long[]{7}));
        }
    }

    @Nested
    @DisplayName("Parallel sort")
    class Sort {
        @Test void sorts_array_correctly() {
            int[] arr = {5, 3, 8, 1, 9, 2, 7, 4, 6};
            int[] sorted = ForkJoinDemo.parallelSort(arr);
            int[] expected = arr.clone();
            Arrays.sort(expected);
            assertArrayEquals(expected, sorted);
        }

        @Test void sort_already_sorted_array() {
            int[] arr = {1, 2, 3, 4, 5};
            assertArrayEquals(arr, ForkJoinDemo.parallelSort(arr));
        }

        @Test void sort_reverse_array() {
            int[] arr = {5, 4, 3, 2, 1};
            assertArrayEquals(new int[]{1, 2, 3, 4, 5}, ForkJoinDemo.parallelSort(arr));
        }

        @Test void original_array_not_modified() {
            int[] arr = {3, 1, 2};
            int[] original = arr.clone();
            ForkJoinDemo.parallelSort(arr);
            assertArrayEquals(original, arr);
        }
    }

    @Nested
    @DisplayName("Parallel Fibonacci")
    class Fib {
        @Test void fib_base_cases() {
            assertEquals(0L, ForkJoinDemo.parallelFib(0));
            assertEquals(1L, ForkJoinDemo.parallelFib(1));
        }

        @Test void fib_known_values() {
            assertEquals(55L,   ForkJoinDemo.parallelFib(10));
            assertEquals(6765L, ForkJoinDemo.parallelFib(20));
        }
    }

    @Nested
    @DisplayName("Pool stats")
    class Stats {
        @Test void pool_stats_returns_expected_keys() throws Exception {
            Map<String, Long> stats = ForkJoinDemo.poolStats(2);
            assertTrue(stats.containsKey("parallelism"));
            assertTrue(stats.containsKey("stealCount"));
            assertEquals(2L, stats.get("parallelism"));
        }
    }
}
