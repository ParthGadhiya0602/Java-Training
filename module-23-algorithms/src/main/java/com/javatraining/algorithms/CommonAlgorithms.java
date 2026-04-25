package com.javatraining.algorithms;

import java.util.*;

/**
 * Module 23 - Common Algorithm Patterns
 *
 * Patterns covered:
 *   - Two pointers (opposite ends, same direction / sliding window)
 *   - Divide and conquer (max subarray, closest pair)
 *   - Dynamic programming (Fibonacci, LCS, knapsack, LIS, edit distance)
 *   - Greedy (activity selection, coin change with canonical coins)
 *   - Backtracking (permutations, subsets, N-queens count)
 *   - Graph traversal (BFS, DFS on adjacency list)
 *   - Bit manipulation utilities
 */
public class CommonAlgorithms {

    // ── Two pointers ──────────────────────────────────────────────────────────

    /** Returns true if sorted arr has two numbers summing to target. O(n). */
    public static boolean hasTwoSum(int[] arr, int target) {
        int lo = 0, hi = arr.length - 1;
        while (lo < hi) {
            int sum = arr[lo] + arr[hi];
            if      (sum == target) return true;
            else if (sum <  target) lo++;
            else                    hi--;
        }
        return false;
    }

    /** Returns max sum of a contiguous subarray of length k. O(n). */
    public static int maxSlidingWindow(int[] arr, int k) {
        if (arr.length == 0 || k <= 0) return 0;
        int sum = 0;
        for (int i = 0; i < k; i++) sum += arr[i];
        int max = sum;
        for (int i = k; i < arr.length; i++) {
            sum += arr[i] - arr[i - k];
            if (sum > max) max = sum;
        }
        return max;
    }

    /** Returns length of the longest substring without repeating characters. O(n). */
    public static int longestUniqueSubstring(String s) {
        int[] last = new int[128];
        Arrays.fill(last, -1);
        int maxLen = 0, start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (last[c] >= start) start = last[c] + 1;
            last[c] = i;
            maxLen = Math.max(maxLen, i - start + 1);
        }
        return maxLen;
    }

    // ── Divide and conquer ────────────────────────────────────────────────────

    /**
     * Kadane's algorithm: maximum subarray sum. O(n).
     * Returns 0 for empty arrays; handles all-negative arrays correctly.
     */
    public static int maxSubarraySum(int[] arr) {
        if (arr.length == 0) return 0;
        int maxSoFar = arr[0], maxEndingHere = arr[0];
        for (int i = 1; i < arr.length; i++) {
            maxEndingHere = Math.max(arr[i], maxEndingHere + arr[i]);
            maxSoFar      = Math.max(maxSoFar, maxEndingHere);
        }
        return maxSoFar;
    }

    /** Returns the number of inversions in arr using modified merge sort. O(n log n). */
    public static long countInversions(int[] arr) {
        int[] tmp = arr.clone();
        return mergeCount(tmp, 0, tmp.length - 1);
    }

    private static long mergeCount(int[] arr, int l, int r) {
        if (l >= r) return 0;
        int mid = l + (r - l) / 2;
        long count = mergeCount(arr, l, mid) + mergeCount(arr, mid + 1, r);
        // Merge and count cross-inversions
        int[] buf = new int[r - l + 1];
        int i = l, j = mid + 1, k = 0;
        while (i <= mid && j <= r) {
            if (arr[i] <= arr[j]) buf[k++] = arr[i++];
            else { count += (mid - i + 1); buf[k++] = arr[j++]; }
        }
        while (i <= mid) buf[k++] = arr[i++];
        while (j <= r)   buf[k++] = arr[j++];
        System.arraycopy(buf, 0, arr, l, buf.length);
        return count;
    }

    // ── Dynamic programming ───────────────────────────────────────────────────

    /** Bottom-up Fibonacci. O(n) time, O(1) space. */
    public static long fibonacci(int n) {
        if (n <= 1) return n;
        long a = 0, b = 1;
        for (int i = 2; i <= n; i++) { long c = a + b; a = b; b = c; }
        return b;
    }

    /**
     * Longest Common Subsequence length. O(m*n) time and space.
     * LCS("ABCBDAB", "BDCAB") = 4 ("BCAB").
     */
    public static int lcs(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                dp[i][j] = a.charAt(i-1) == b.charAt(j-1)
                    ? dp[i-1][j-1] + 1
                    : Math.max(dp[i-1][j], dp[i][j-1]);
        return dp[m][n];
    }

    /**
     * 0/1 Knapsack: max value with items of given weights/values, capacity limit.
     * O(n * capacity) time and space.
     */
    public static int knapsack(int[] weights, int[] values, int capacity) {
        int n = weights.length;
        int[][] dp = new int[n + 1][capacity + 1];
        for (int i = 1; i <= n; i++)
            for (int w = 0; w <= capacity; w++)
                dp[i][w] = weights[i-1] <= w
                    ? Math.max(dp[i-1][w], dp[i-1][w - weights[i-1]] + values[i-1])
                    : dp[i-1][w];
        return dp[n][capacity];
    }

    /**
     * Longest Increasing Subsequence length. O(n log n) using patience sorting.
     */
    public static int lis(int[] arr) {
        List<Integer> tails = new ArrayList<>();
        for (int x : arr) {
            int lo = 0, hi = tails.size();
            while (lo < hi) {
                int mid = lo + (hi - lo) / 2;
                if (tails.get(mid) < x) lo = mid + 1;
                else                     hi = mid;
            }
            if (lo == tails.size()) tails.add(x);
            else                    tails.set(lo, x);
        }
        return tails.size();
    }

    /**
     * Edit distance (Levenshtein): minimum insert/delete/replace operations
     * to convert s into t. O(m*n) time, O(min(m,n)) space.
     */
    public static int editDistance(String s, String t) {
        int m = s.length(), n = t.length();
        int[] prev = new int[n + 1], curr = new int[n + 1];
        for (int j = 0; j <= n; j++) prev[j] = j;
        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            for (int j = 1; j <= n; j++) {
                if (s.charAt(i-1) == t.charAt(j-1)) curr[j] = prev[j-1];
                else curr[j] = 1 + Math.min(prev[j-1], Math.min(prev[j], curr[j-1]));
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[n];
    }

    // ── Greedy ────────────────────────────────────────────────────────────────

    /**
     * Activity selection: given intervals [start, end], select maximum number
     * of non-overlapping activities. O(n log n).
     * Returns list of selected [start, end] pairs.
     */
    public static List<int[]> activitySelection(int[][] activities) {
        int[][] sorted = activities.clone();
        Arrays.sort(sorted, Comparator.comparingInt(a -> a[1]));  // sort by end time
        List<int[]> result = new ArrayList<>();
        int lastEnd = Integer.MIN_VALUE;
        for (int[] act : sorted) {
            if (act[0] >= lastEnd) { result.add(act); lastEnd = act[1]; }
        }
        return result;
    }

    /**
     * Minimum coins (canonical coin systems like US cents).
     * Greedy works for standard denominations; use DP for arbitrary coins.
     * Returns -1 if exact change is impossible.
     */
    public static int minCoinsGreedy(int[] denominations, int amount) {
        int[] coins = denominations.clone();
        Arrays.sort(coins);
        int count = 0;
        for (int i = coins.length - 1; i >= 0 && amount > 0; i--) {
            count  += amount / coins[i];
            amount %= coins[i];
        }
        return amount == 0 ? count : -1;
    }

    /** Coin change DP - works for any coin system. Returns -1 if impossible. */
    public static int minCoinsDp(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1);
        dp[0] = 0;
        for (int i = 1; i <= amount; i++)
            for (int coin : coins)
                if (coin <= i) dp[i] = Math.min(dp[i], dp[i - coin] + 1);
        return dp[amount] > amount ? -1 : dp[amount];
    }

    // ── Backtracking ──────────────────────────────────────────────────────────

    /** Returns all permutations of the given array. */
    public static List<int[]> permutations(int[] arr) {
        List<int[]> result = new ArrayList<>();
        permute(arr.clone(), 0, result);
        return result;
    }

    private static void permute(int[] arr, int start, List<int[]> result) {
        if (start == arr.length) { result.add(arr.clone()); return; }
        for (int i = start; i < arr.length; i++) {
            swap(arr, start, i);
            permute(arr, start + 1, result);
            swap(arr, start, i);
        }
    }

    private static void swap(int[] arr, int i, int j) {
        int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
    }

    /** Returns all subsets (power set) of the given array. */
    public static List<List<Integer>> subsets(int[] arr) {
        List<List<Integer>> result = new ArrayList<>();
        subsetsHelper(arr, 0, new ArrayList<>(), result);
        return result;
    }

    private static void subsetsHelper(int[] arr, int idx,
                                       List<Integer> current, List<List<Integer>> result) {
        result.add(new ArrayList<>(current));
        for (int i = idx; i < arr.length; i++) {
            current.add(arr[i]);
            subsetsHelper(arr, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    /** Returns the count of solutions to the N-Queens problem. */
    public static int nQueens(int n) {
        return nQueensHelper(n, 0, new boolean[n], new boolean[2 * n], new boolean[2 * n]);
    }

    private static int nQueensHelper(int n, int row,
                                      boolean[] cols, boolean[] diag1, boolean[] diag2) {
        if (row == n) return 1;
        int count = 0;
        for (int col = 0; col < n; col++) {
            int d1 = row - col + n, d2 = row + col;
            if (!cols[col] && !diag1[d1] && !diag2[d2]) {
                cols[col] = diag1[d1] = diag2[d2] = true;
                count += nQueensHelper(n, row + 1, cols, diag1, diag2);
                cols[col] = diag1[d1] = diag2[d2] = false;
            }
        }
        return count;
    }

    // ── Graph traversal (adjacency list) ─────────────────────────────────────

    /** BFS - returns nodes in visit order from start. */
    public static List<Integer> bfs(Map<Integer, List<Integer>> graph, int start) {
        List<Integer> order = new ArrayList<>();
        Set<Integer>  seen  = new HashSet<>();
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty()) {
            int node = queue.poll();
            order.add(node);
            List<Integer> neighbours = graph.getOrDefault(node, List.of());
            for (int nb : neighbours) {
                if (seen.add(nb)) queue.add(nb);
            }
        }
        return order;
    }

    /** DFS - returns nodes in visit order from start. */
    public static List<Integer> dfs(Map<Integer, List<Integer>> graph, int start) {
        List<Integer> order = new ArrayList<>();
        Set<Integer>  seen  = new HashSet<>();
        dfsHelper(graph, start, seen, order);
        return order;
    }

    private static void dfsHelper(Map<Integer, List<Integer>> graph,
                                   int node, Set<Integer> seen, List<Integer> order) {
        if (!seen.add(node)) return;
        order.add(node);
        for (int nb : graph.getOrDefault(node, List.of())) dfsHelper(graph, nb, seen, order);
    }

    /** Topological sort using DFS. Returns empty list if cycle detected. */
    public static List<Integer> topologicalSort(Map<Integer, List<Integer>> dag,
                                                 Set<Integer> allNodes) {
        Deque<Integer>  result = new ArrayDeque<>();
        Set<Integer>    visited = new HashSet<>(), inStack = new HashSet<>();
        for (int node : allNodes) {
            if (!visited.contains(node)) {
                if (!topoHelper(dag, node, visited, inStack, result)) return List.of();
            }
        }
        return new ArrayList<>(result);
    }

    private static boolean topoHelper(Map<Integer, List<Integer>> dag, int node,
                                       Set<Integer> visited, Set<Integer> inStack,
                                       Deque<Integer> result) {
        visited.add(node);
        inStack.add(node);
        for (int nb : dag.getOrDefault(node, List.of())) {
            if (inStack.contains(nb)) return false;  // cycle
            if (!visited.contains(nb))
                if (!topoHelper(dag, nb, visited, inStack, result)) return false;
        }
        inStack.remove(node);
        result.addFirst(node);
        return true;
    }

    // ── Bit manipulation ──────────────────────────────────────────────────────

    public static boolean isPowerOfTwo(int n) { return n > 0 && (n & (n - 1)) == 0; }

    /** Counts set bits using Brian Kernighan's algorithm. */
    public static int countBits(int n) {
        int count = 0;
        while (n != 0) { n &= n - 1; count++; }
        return count;
    }

    /** Returns true if bit at position pos (0 = LSB) is set. */
    public static boolean isBitSet(int n, int pos) { return (n & (1 << pos)) != 0; }

    /** Returns n with bit at pos set. */
    public static int setBit(int n, int pos)   { return n | (1 << pos); }

    /** Returns n with bit at pos cleared. */
    public static int clearBit(int n, int pos) { return n & ~(1 << pos); }

    /** Returns n with bit at pos toggled. */
    public static int toggleBit(int n, int pos){ return n ^ (1 << pos); }

    /** XOR trick: in an array where every element appears twice except one, find the lone element. */
    public static int singleNumber(int[] arr) {
        int result = 0;
        for (int v : arr) result ^= v;
        return result;
    }
}
