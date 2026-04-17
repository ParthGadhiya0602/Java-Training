package com.javatraining.algorithms;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SearchAlgorithms")
class SearchAlgorithmsTest {

    @Nested
    @DisplayName("Linear search")
    class LinearSearch {
        @Test void finds_element()         { assertEquals(2, SearchAlgorithms.linearSearch(new int[]{1,2,3,4}, 3)); }
        @Test void not_found_returns_neg1() { assertEquals(-1, SearchAlgorithms.linearSearch(new int[]{1,2,3}, 9)); }
        @Test void empty_array()           { assertEquals(-1, SearchAlgorithms.linearSearch(new int[]{}, 1)); }
        @Test void generic_list_found()    { assertEquals(1, SearchAlgorithms.linearSearch(List.of("a","b","c"), "b")); }
        @Test void generic_list_not_found(){ assertEquals(-1, SearchAlgorithms.linearSearch(List.of("a","b"), "z")); }
    }

    @Nested
    @DisplayName("Binary search (standard)")
    class BinarySearch {
        int[] sorted = {1, 3, 5, 7, 9, 11, 13};

        @Test void finds_existing_element() { assertTrue(SearchAlgorithms.binarySearch(sorted, 7) >= 0); }
        @Test void not_found_returns_neg1() { assertEquals(-1, SearchAlgorithms.binarySearch(sorted, 6)); }
        @Test void finds_first_element()    { assertEquals(0, SearchAlgorithms.binarySearch(sorted, 1)); }
        @Test void finds_last_element()     { assertEquals(6, SearchAlgorithms.binarySearch(sorted, 13)); }

        @Test void recursive_finds_element() {
            assertTrue(SearchAlgorithms.binarySearchRecursive(sorted, 9) >= 0);
        }
        @Test void recursive_not_found() {
            assertEquals(-1, SearchAlgorithms.binarySearchRecursive(sorted, 4));
        }
    }

    @Nested
    @DisplayName("Binary search bounds")
    class Bounds {
        int[] arr = {1, 2, 2, 2, 3, 4};

        @Test void left_bound_first_occurrence()   { assertEquals(1, SearchAlgorithms.leftBound(arr, 2)); }
        @Test void right_bound_last_occurrence()   { assertEquals(3, SearchAlgorithms.rightBound(arr, 2)); }
        @Test void left_bound_not_found()          { assertEquals(-1, SearchAlgorithms.leftBound(arr, 9)); }
        @Test void right_bound_not_found()         { assertEquals(-1, SearchAlgorithms.rightBound(arr, 9)); }
        @Test void lower_bound_exact_match()       { assertEquals(1, SearchAlgorithms.lowerBound(arr, 2)); }
        @Test void lower_bound_between_elements()  { assertEquals(4, SearchAlgorithms.lowerBound(arr, 3)); }
        @Test void lower_bound_past_end()          { assertEquals(6, SearchAlgorithms.lowerBound(arr, 9)); }
        @Test void upper_bound_after_last_match()  { assertEquals(4, SearchAlgorithms.upperBound(arr, 2)); }
        @Test void upper_bound_past_end()          { assertEquals(6, SearchAlgorithms.upperBound(arr, 9)); }
        @Test void count_occurrences()             { assertEquals(3, SearchAlgorithms.countOccurrences(arr, 2)); }
        @Test void count_not_present()             { assertEquals(0, SearchAlgorithms.countOccurrences(arr, 9)); }
    }

    @Nested
    @DisplayName("Exponential search")
    class ExponentialSearch {
        int[] arr = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19};

        @Test void finds_element()         { assertTrue(SearchAlgorithms.exponentialSearch(arr, 11) >= 0); }
        @Test void not_found()             { assertEquals(-1, SearchAlgorithms.exponentialSearch(arr, 4)); }
        @Test void finds_first_element()   { assertEquals(0, SearchAlgorithms.exponentialSearch(arr, 1)); }
    }

    @Nested
    @DisplayName("Peak finding")
    class PeakFinding {
        @Test void peak_in_middle() {
            int idx = SearchAlgorithms.findPeak(new int[]{1, 3, 5, 4, 2});
            assertEquals(2, idx);
        }

        @Test void peak_at_end() {
            int idx = SearchAlgorithms.findPeak(new int[]{1, 2, 3});
            assertEquals(2, idx);
        }

        @Test void single_element_is_peak() {
            assertEquals(0, SearchAlgorithms.findPeak(new int[]{42}));
        }
    }

    @Nested
    @DisplayName("Search in rotated array")
    class RotatedSearch {
        int[] arr = {4, 5, 6, 7, 0, 1, 2};

        @Test void finds_element_in_left_part()  { assertEquals(2, SearchAlgorithms.searchRotated(arr, 6)); }
        @Test void finds_element_in_right_part() { assertTrue(SearchAlgorithms.searchRotated(arr, 0) >= 0); }
        @Test void not_found_returns_neg1()      { assertEquals(-1, SearchAlgorithms.searchRotated(arr, 3)); }
        @Test void finds_pivot_element()         { assertEquals(0, SearchAlgorithms.searchRotated(arr, 4)); }
    }

    @Nested
    @DisplayName("2D matrix search")
    class MatrixSearch {
        int[][] matrix = {
            {1,  4,  7, 11},
            {2,  5,  8, 12},
            {3,  6,  9, 16},
            {10, 13, 14, 17}
        };

        @Test void finds_existing_element() {
            Optional<int[]> r = SearchAlgorithms.searchMatrix(matrix, 5);
            assertTrue(r.isPresent());
            assertEquals(5, matrix[r.get()[0]][r.get()[1]]);
        }

        @Test void not_found_returns_empty() {
            assertTrue(SearchAlgorithms.searchMatrix(matrix, 100).isEmpty());
        }

        @Test void finds_corner_element() {
            assertTrue(SearchAlgorithms.searchMatrix(matrix, 1).isPresent());
        }
    }
}
