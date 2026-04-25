package com.javatraining.algorithms;

import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;

/**
 * Module 23 - Search Algorithms
 *
 * Algorithm        | Time         | Space | Prerequisite
 * -----------------|--------------|-------|-------------
 * Linear search    | O(n)         | O(1)  | none
 * Binary search    | O(log n)     | O(1)  | sorted array
 * Binary search    | O(log n)     | O(log n) recursive (call stack)
 * Ternary search   | O(log₃ n)   | O(1)  | sorted, unimodal
 * Exponential srch | O(log n)     | O(1)  | sorted, unbounded range
 *
 * Binary search variants:
 *   Standard        - returns any matching index
 *   Left bound      - returns first (leftmost) index equal to target
 *   Right bound     - returns last (rightmost) index equal to target
 *   Lower bound     - returns first index >= target (like C++ lower_bound)
 *   Upper bound     - returns first index > target  (like C++ upper_bound)
 */
public class SearchAlgorithms {

    // ── Linear search ─────────────────────────────────────────────────────────

    /** Returns the index of the first occurrence of target, or -1 if not found. */
    public static int linearSearch(int[] arr, int target) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == target) return i;
        }
        return -1;
    }

    /** Generic linear search using equals(). */
    public static <T> int linearSearch(List<T> list, T target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(target)) return i;
        }
        return -1;
    }

    // ── Binary search - standard ──────────────────────────────────────────────

    /**
     * Returns any index where arr[i] == target, or -1 if not found.
     * Array must be sorted in ascending order.
     */
    public static int binarySearch(int[] arr, int target) {
        int lo = 0, hi = arr.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;   // avoids overflow vs (lo+hi)/2
            if      (arr[mid] == target) return mid;
            else if (arr[mid] <  target) lo = mid + 1;
            else                         hi = mid - 1;
        }
        return -1;
    }

    /** Recursive binary search. */
    public static int binarySearchRecursive(int[] arr, int target) {
        return bsHelper(arr, target, 0, arr.length - 1);
    }

    private static int bsHelper(int[] arr, int target, int lo, int hi) {
        if (lo > hi) return -1;
        int mid = lo + (hi - lo) / 2;
        if      (arr[mid] == target) return mid;
        else if (arr[mid] <  target) return bsHelper(arr, target, mid + 1, hi);
        else                         return bsHelper(arr, target, lo, mid - 1);
    }

    // ── Binary search - bounds ────────────────────────────────────────────────

    /**
     * Left bound: index of the first occurrence of target, or -1.
     * E.g. [1,2,2,2,3] with target 2 → index 1.
     */
    public static int leftBound(int[] arr, int target) {
        int lo = 0, hi = arr.length - 1, result = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid] == target) { result = mid; hi = mid - 1; }
            else if (arr[mid] < target) lo = mid + 1;
            else                        hi = mid - 1;
        }
        return result;
    }

    /**
     * Right bound: index of the last occurrence of target, or -1.
     * E.g. [1,2,2,2,3] with target 2 → index 3.
     */
    public static int rightBound(int[] arr, int target) {
        int lo = 0, hi = arr.length - 1, result = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid] == target) { result = mid; lo = mid + 1; }
            else if (arr[mid] < target) lo = mid + 1;
            else                        hi = mid - 1;
        }
        return result;
    }

    /**
     * Lower bound: first index i such that arr[i] >= target.
     * Returns arr.length if all elements are < target.
     */
    public static int lowerBound(int[] arr, int target) {
        int lo = 0, hi = arr.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid] < target) lo = mid + 1;
            else                   hi = mid;
        }
        return lo;
    }

    /**
     * Upper bound: first index i such that arr[i] > target.
     * Returns arr.length if all elements are <= target.
     */
    public static int upperBound(int[] arr, int target) {
        int lo = 0, hi = arr.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid] <= target) lo = mid + 1;
            else                    hi = mid;
        }
        return lo;
    }

    /** Returns the count of occurrences of target in a sorted array. */
    public static int countOccurrences(int[] arr, int target) {
        int lo = leftBound(arr, target);
        if (lo == -1) return 0;
        return rightBound(arr, target) - lo + 1;
    }

    // ── Binary search on answer ───────────────────────────────────────────────

    /**
     * "Binary search on the answer" pattern:
     * Find the minimum value in [lo, hi] satisfying a monotone predicate.
     * predicate(x) = false, false, ..., true, true, true
     * Returns the first x for which predicate(x) is true.
     */
    public static long binarySearchOnAnswer(long lo, long hi, IntPredicate predicate) {
        while (lo < hi) {
            long mid = lo + (hi - lo) / 2;
            if (predicate.test((int) mid)) hi = mid;
            else                           lo = mid + 1;
        }
        return lo;
    }

    // ── Exponential search ────────────────────────────────────────────────────

    /**
     * Finds the range where target might be by doubling the index,
     * then binary searches that range.
     * O(log n) - useful when the array size is unknown or very large.
     */
    public static int exponentialSearch(int[] arr, int target) {
        if (arr.length == 0) return -1;
        if (arr[0] == target) return 0;
        int i = 1;
        while (i < arr.length && arr[i] <= target) i *= 2;
        // Binary search in [i/2, min(i, n-1)]
        int lo = i / 2, hi = Math.min(i, arr.length - 1);
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if      (arr[mid] == target) return mid;
            else if (arr[mid] <  target) lo = mid + 1;
            else                         hi = mid - 1;
        }
        return -1;
    }

    // ── Peak finding ──────────────────────────────────────────────────────────

    /**
     * Finds any peak element: arr[i] >= arr[i-1] and arr[i] >= arr[i+1].
     * Uses binary search - O(log n). Assumes arr is not empty.
     * A peak always exists (the maximum is always a peak).
     */
    public static int findPeak(int[] arr) {
        int lo = 0, hi = arr.length - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid] < arr[mid + 1]) lo = mid + 1;  // peak is to the right
            else                          hi = mid;       // peak is here or to the left
        }
        return lo;   // index of a peak element
    }

    // ── Search in rotated sorted array ───────────────────────────────────────

    /**
     * Searches target in a sorted array that has been rotated at some unknown pivot.
     * E.g. [4,5,6,1,2,3]. O(log n).
     */
    public static int searchRotated(int[] arr, int target) {
        int lo = 0, hi = arr.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid] == target) return mid;
            // Left half is sorted
            if (arr[lo] <= arr[mid]) {
                if (arr[lo] <= target && target < arr[mid]) hi = mid - 1;
                else                                         lo = mid + 1;
            } else {
                // Right half is sorted
                if (arr[mid] < target && target <= arr[hi]) lo = mid + 1;
                else                                         hi = mid - 1;
            }
        }
        return -1;
    }

    // ── 2D search ─────────────────────────────────────────────────────────────

    /**
     * Searches in a matrix where each row and column is sorted ascending.
     * Starts from top-right corner: O(m + n).
     */
    public static Optional<int[]> searchMatrix(int[][] matrix, int target) {
        if (matrix.length == 0 || matrix[0].length == 0) return Optional.empty();
        int row = 0, col = matrix[0].length - 1;
        while (row < matrix.length && col >= 0) {
            if      (matrix[row][col] == target) return Optional.of(new int[]{row, col});
            else if (matrix[row][col] >  target) col--;
            else                                  row++;
        }
        return Optional.empty();
    }
}
