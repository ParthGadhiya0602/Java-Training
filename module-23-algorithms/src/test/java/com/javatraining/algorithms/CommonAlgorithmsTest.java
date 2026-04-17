package com.javatraining.algorithms;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommonAlgorithms")
class CommonAlgorithmsTest {

    @Nested
    @DisplayName("Two pointers")
    class TwoPointers {
        @Test void two_sum_found() {
            assertTrue(CommonAlgorithms.hasTwoSum(new int[]{1, 2, 3, 4, 5}, 6));
        }

        @Test void two_sum_not_found() {
            assertFalse(CommonAlgorithms.hasTwoSum(new int[]{1, 2, 3}, 10));
        }

        @Test void sliding_window_max() {
            assertEquals(14, CommonAlgorithms.maxSlidingWindow(new int[]{1,3,2,5,9,1,3}, 2));
        }

        @Test void sliding_window_full_array() {
            assertEquals(10, CommonAlgorithms.maxSlidingWindow(new int[]{1,2,3,4}, 4));
        }

        @Test void longest_unique_substring_abcabcbb() {
            assertEquals(3, CommonAlgorithms.longestUniqueSubstring("abcabcbb"));
        }

        @Test void longest_unique_substring_all_same() {
            assertEquals(1, CommonAlgorithms.longestUniqueSubstring("aaaa"));
        }

        @Test void longest_unique_substring_all_distinct() {
            assertEquals(5, CommonAlgorithms.longestUniqueSubstring("abcde"));
        }
    }

    @Nested
    @DisplayName("Divide and conquer")
    class DivideAndConquer {
        @Test void max_subarray_sum_mixed() {
            assertEquals(6, CommonAlgorithms.maxSubarraySum(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}));
        }

        @Test void max_subarray_all_negative() {
            assertEquals(-1, CommonAlgorithms.maxSubarraySum(new int[]{-3, -1, -2}));
        }

        @Test void max_subarray_single() {
            assertEquals(5, CommonAlgorithms.maxSubarraySum(new int[]{5}));
        }

        @Test void inversion_count_sorted() {
            assertEquals(0, CommonAlgorithms.countInversions(new int[]{1, 2, 3, 4}));
        }

        @Test void inversion_count_reverse_sorted() {
            assertEquals(6, CommonAlgorithms.countInversions(new int[]{4, 3, 2, 1}));
        }

        @Test void inversion_count_example() {
            assertEquals(3, CommonAlgorithms.countInversions(new int[]{2, 4, 1, 3, 5}));
        }
    }

    @Nested
    @DisplayName("Dynamic programming")
    class DynamicProgramming {
        @Test void fibonacci_base_cases() {
            assertEquals(0, CommonAlgorithms.fibonacci(0));
            assertEquals(1, CommonAlgorithms.fibonacci(1));
        }

        @Test void fibonacci_tenth() {
            assertEquals(55, CommonAlgorithms.fibonacci(10));
        }

        @Test void lcs_example() {
            assertEquals(4, CommonAlgorithms.lcs("ABCBDAB", "BDCAB"));
        }

        @Test void lcs_identical_strings() {
            assertEquals(5, CommonAlgorithms.lcs("hello", "hello"));
        }

        @Test void lcs_no_common() {
            assertEquals(0, CommonAlgorithms.lcs("abc", "xyz"));
        }

        @Test void knapsack_example() {
            // weights {1,3,4,5}, values {1,4,5,7}, capacity 7
            // best: items with weight 3+4=7, value 4+5=9
            assertEquals(9, CommonAlgorithms.knapsack(
                new int[]{1, 3, 4, 5},
                new int[]{1, 4, 5, 7}, 7));
        }

        @Test void lis_increasing() {
            assertEquals(4, CommonAlgorithms.lis(new int[]{10, 9, 2, 5, 3, 7, 101, 18}));
        }

        @Test void lis_fully_sorted() {
            assertEquals(5, CommonAlgorithms.lis(new int[]{1, 2, 3, 4, 5}));
        }

        @Test void edit_distance_same_string() {
            assertEquals(0, CommonAlgorithms.editDistance("abc", "abc"));
        }

        @Test void edit_distance_insertions() {
            assertEquals(3, CommonAlgorithms.editDistance("", "abc"));
        }

        @Test void edit_distance_example() {
            assertEquals(3, CommonAlgorithms.editDistance("kitten", "sitting"));
        }
    }

    @Nested
    @DisplayName("Greedy")
    class Greedy {
        @Test void activity_selection_count() {
            int[][] acts = {{1,4},{3,5},{0,6},{5,7},{3,9},{5,9},{6,10},{8,11},{8,12},{2,14},{12,16}};
            assertEquals(4, CommonAlgorithms.activitySelection(acts).size());
        }

        @Test void activity_selection_non_overlapping() {
            List<int[]> selected = CommonAlgorithms.activitySelection(
                new int[][]{{0,2},{1,3},{3,5}});
            assertEquals(2, selected.size());
        }

        @Test void min_coins_greedy_exact() {
            // 30 = 25 + 5 → 2 coins
            assertEquals(2, CommonAlgorithms.minCoinsGreedy(new int[]{1, 5, 10, 25}, 30));
        }

        @Test void min_coins_dp_canonical() {
            // 30 = 25 + 5 → 2 coins
            assertEquals(2, CommonAlgorithms.minCoinsDp(new int[]{1, 5, 10, 25}, 30));
        }

        @Test void min_coins_dp_non_canonical() {
            // Greedy fails for {1,3,4} and amount=6 but DP finds 2 coins (3+3)
            assertEquals(2, CommonAlgorithms.minCoinsDp(new int[]{1, 3, 4}, 6));
        }

        @Test void min_coins_impossible() {
            assertEquals(-1, CommonAlgorithms.minCoinsDp(new int[]{2}, 3));
        }
    }

    @Nested
    @DisplayName("Backtracking")
    class Backtracking {
        @Test void permutations_count() {
            assertEquals(6, CommonAlgorithms.permutations(new int[]{1, 2, 3}).size());
        }

        @Test void permutations_single() {
            assertEquals(1, CommonAlgorithms.permutations(new int[]{1}).size());
        }

        @Test void subsets_count() {
            // 2^3 = 8 subsets
            assertEquals(8, CommonAlgorithms.subsets(new int[]{1, 2, 3}).size());
        }

        @Test void subsets_includes_empty() {
            assertTrue(CommonAlgorithms.subsets(new int[]{1, 2}).stream()
                .anyMatch(List::isEmpty));
        }

        @Test void n_queens_4() {
            assertEquals(2, CommonAlgorithms.nQueens(4));
        }

        @Test void n_queens_8() {
            assertEquals(92, CommonAlgorithms.nQueens(8));
        }
    }

    @Nested
    @DisplayName("Graph traversal")
    class GraphTraversal {
        private Map<Integer, List<Integer>> graph() {
            return Map.of(
                1, List.of(2, 3),
                2, List.of(4),
                3, List.of(4, 5),
                4, List.of(),
                5, List.of()
            );
        }

        @Test void bfs_visits_all_nodes() {
            List<Integer> order = CommonAlgorithms.bfs(graph(), 1);
            assertEquals(5, order.size());
            assertEquals(1, order.get(0));
        }

        @Test void dfs_visits_all_nodes() {
            List<Integer> order = CommonAlgorithms.dfs(graph(), 1);
            assertEquals(5, order.size());
            assertEquals(1, order.get(0));
        }

        @Test void topological_sort_respects_order() {
            var dag = Map.of(
                1, List.of(3),
                2, List.of(3),
                3, List.of(4),
                4, List.<Integer>of()
            );
            List<Integer> topo = CommonAlgorithms.topologicalSort(dag, Set.of(1, 2, 3, 4));
            // 3 must come after both 1 and 2; 4 must come after 3
            assertTrue(topo.indexOf(3) > topo.indexOf(1));
            assertTrue(topo.indexOf(4) > topo.indexOf(3));
        }
    }

    @Nested
    @DisplayName("Bit manipulation")
    class BitManipulation {
        @Test void is_power_of_two() {
            assertTrue(CommonAlgorithms.isPowerOfTwo(1));
            assertTrue(CommonAlgorithms.isPowerOfTwo(16));
            assertFalse(CommonAlgorithms.isPowerOfTwo(6));
            assertFalse(CommonAlgorithms.isPowerOfTwo(0));
        }

        @Test void count_bits() {
            assertEquals(0, CommonAlgorithms.countBits(0));
            assertEquals(1, CommonAlgorithms.countBits(8));
            assertEquals(4, CommonAlgorithms.countBits(15));
        }

        @Test void set_clear_toggle() {
            assertEquals(5, CommonAlgorithms.setBit(4, 0));    // 100 | 001 = 101
            assertEquals(4, CommonAlgorithms.clearBit(5, 0));  // 101 & ~001 = 100
            assertEquals(5, CommonAlgorithms.toggleBit(4, 0)); // 100 ^ 001 = 101
        }

        @Test void is_bit_set() {
            assertTrue(CommonAlgorithms.isBitSet(5, 0));  // 101, bit 0 set
            assertFalse(CommonAlgorithms.isBitSet(4, 0)); // 100, bit 0 not set
        }

        @Test void single_number_xor() {
            assertEquals(4, CommonAlgorithms.singleNumber(new int[]{2, 3, 2, 4, 3}));
        }
    }
}
