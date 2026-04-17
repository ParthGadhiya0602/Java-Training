package com.javatraining.algorithms;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SortingAlgorithms")
class SortingAlgorithmsTest {

    // ── Shared test data and parameterised sort tests ─────────────────────────

    @FunctionalInterface interface SortFn { void sort(int[] arr); }

    static Stream<SortFn> allSorts() {
        return Stream.of(
            SortingAlgorithms::bubbleSort,
            SortingAlgorithms::selectionSort,
            SortingAlgorithms::insertionSort,
            SortingAlgorithms::mergeSort,
            SortingAlgorithms::quickSort,
            SortingAlgorithms::heapSort
        );
    }

    @ParameterizedTest(name = "sort[{index}]")
    @MethodSource("allSorts")
    void sorts_random_array(SortFn fn) {
        int[] arr = {5, 3, 8, 1, 9, 2, 7, 4, 6};
        fn.sort(arr);
        assertTrue(SortingAlgorithms.isSorted(arr));
    }

    @ParameterizedTest(name = "sort[{index}]")
    @MethodSource("allSorts")
    void sorts_already_sorted(SortFn fn) {
        int[] arr = {1, 2, 3, 4, 5};
        fn.sort(arr);
        assertTrue(SortingAlgorithms.isSorted(arr));
    }

    @ParameterizedTest(name = "sort[{index}]")
    @MethodSource("allSorts")
    void sorts_reverse_sorted(SortFn fn) {
        int[] arr = {9, 7, 5, 3, 1};
        fn.sort(arr);
        assertTrue(SortingAlgorithms.isSorted(arr));
    }

    @ParameterizedTest(name = "sort[{index}]")
    @MethodSource("allSorts")
    void sorts_single_element(SortFn fn) {
        int[] arr = {42};
        fn.sort(arr);
        assertArrayEquals(new int[]{42}, arr);
    }

    @ParameterizedTest(name = "sort[{index}]")
    @MethodSource("allSorts")
    void sorts_duplicates(SortFn fn) {
        int[] arr = {3, 1, 2, 1, 3, 2};
        fn.sort(arr);
        assertTrue(SortingAlgorithms.isSorted(arr));
    }

    @ParameterizedTest(name = "sort[{index}]")
    @MethodSource("allSorts")
    void sort_matches_arrays_sort(SortFn fn) {
        int[] arr     = {5, 3, 8, 1, 9, 2, 7, 4, 6};
        int[] expected = arr.clone();
        Arrays.sort(expected);
        fn.sort(arr);
        assertArrayEquals(expected, arr);
    }

    // ── Counting sort ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Counting sort")
    class CountingSort {
        @Test void sorts_non_negative_integers() {
            int[] arr = {4, 2, 2, 8, 3, 3, 1};
            SortingAlgorithms.countingSort(arr);
            assertTrue(SortingAlgorithms.isSorted(arr));
        }

        @Test void handles_zeros() {
            int[] arr = {0, 3, 0, 1};
            SortingAlgorithms.countingSort(arr);
            assertArrayEquals(new int[]{0, 0, 1, 3}, arr);
        }

        @Test void handles_empty() {
            int[] arr = {};
            assertDoesNotThrow(() -> SortingAlgorithms.countingSort(arr));
        }
    }

    // ── Generic insertion sort ────────────────────────────────────────────────

    @Nested
    @DisplayName("Generic insertion sort")
    class GenericSort {
        @Test void sorts_strings_by_length() {
            String[] arr = {"banana", "kiwi", "fig", "apple"};
            SortingAlgorithms.insertionSort(arr, Comparator.comparingInt(String::length));
            assertEquals("fig",    arr[0]);
            assertEquals("kiwi",   arr[1]);
            assertEquals("apple",  arr[2]);
            assertEquals("banana", arr[3]);
        }

        @Test void sorts_strings_naturally() {
            String[] arr = {"c", "a", "b"};
            SortingAlgorithms.insertionSort(arr, Comparator.naturalOrder());
            assertArrayEquals(new String[]{"a", "b", "c"}, arr);
        }
    }

    // ── isSorted helper ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("isSorted")
    class IsSortedTests {
        @Test void sorted_array_returns_true()   { assertTrue(SortingAlgorithms.isSorted(new int[]{1, 2, 3})); }
        @Test void unsorted_array_returns_false() { assertFalse(SortingAlgorithms.isSorted(new int[]{3, 1, 2})); }
        @Test void empty_array_returns_true()     { assertTrue(SortingAlgorithms.isSorted(new int[]{})); }
        @Test void single_element_returns_true()  { assertTrue(SortingAlgorithms.isSorted(new int[]{5})); }
    }
}
