package com.javatraining.algorithms;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Module 23 - Sorting Algorithms
 *
 * Algorithm     | Best      | Average   | Worst     | Space  | Stable
 * --------------|-----------|-----------|-----------|--------|-------
 * Bubble sort   | O(n)      | O(n²)     | O(n²)     | O(1)   | yes
 * Selection sort| O(n²)     | O(n²)     | O(n²)     | O(1)   | no
 * Insertion sort| O(n)      | O(n²)     | O(n²)     | O(1)   | yes
 * Merge sort    | O(n log n)| O(n log n)| O(n log n)| O(n)   | yes
 * Quick sort    | O(n log n)| O(n log n)| O(n²)     | O(log n)| no
 * Heap sort     | O(n log n)| O(n log n)| O(n log n)| O(1)   | no
 * Counting sort | O(n+k)    | O(n+k)    | O(n+k)    | O(k)   | yes
 *
 * Java's Arrays.sort() uses:
 *   - Dual-pivot Quicksort for primitives
 *   - TimSort (merge + insertion) for objects - stable, O(n log n)
 */
public class SortingAlgorithms {

    // ── Bubble sort ───────────────────────────────────────────────────────────

    /**
     * Repeatedly swaps adjacent out-of-order elements.
     * Optimised: exits early if no swaps occurred (already sorted).
     * O(n²) average, O(n) best (already sorted), O(1) space, stable.
     */
    public static void bubbleSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                if (arr[j] > arr[j + 1]) {
                    int tmp = arr[j]; arr[j] = arr[j + 1]; arr[j + 1] = tmp;
                    swapped = true;
                }
            }
            if (!swapped) break;   // early exit if already sorted
        }
    }

    // ── Selection sort ────────────────────────────────────────────────────────

    /**
     * Finds the minimum of the unsorted portion and swaps it to the front.
     * Always O(n²) - no early exit possible.
     * O(1) space, NOT stable.
     */
    public static void selectionSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (arr[j] < arr[minIdx]) minIdx = j;
            }
            int tmp = arr[i]; arr[i] = arr[minIdx]; arr[minIdx] = tmp;
        }
    }

    // ── Insertion sort ────────────────────────────────────────────────────────

    /**
     * Builds a sorted prefix by inserting each element into its correct position.
     * Excellent for small or nearly-sorted arrays (used as TimSort's base case).
     * O(n²) average, O(n) best, O(1) space, stable.
     */
    public static void insertionSort(int[] arr) {
        for (int i = 1; i < arr.length; i++) {
            int key = arr[i];
            int j = i - 1;
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }
    }

    // ── Merge sort ────────────────────────────────────────────────────────────

    /**
     * Divide-and-conquer: split in half, recursively sort, merge.
     * O(n log n) all cases, O(n) space, stable.
     * Preferred for linked lists and when stability matters.
     */
    public static void mergeSort(int[] arr) {
        if (arr.length <= 1) return;
        mergeSortHelper(arr, 0, arr.length - 1);
    }

    private static void mergeSortHelper(int[] arr, int left, int right) {
        if (left >= right) return;
        int mid = left + (right - left) / 2;
        mergeSortHelper(arr, left, mid);
        mergeSortHelper(arr, mid + 1, right);
        merge(arr, left, mid, right);
    }

    private static void merge(int[] arr, int left, int mid, int right) {
        int[] tmp = Arrays.copyOfRange(arr, left, right + 1);
        int i = 0, j = mid - left + 1, k = left;
        while (i <= mid - left && j <= right - left) {
            if (tmp[i] <= tmp[j]) arr[k++] = tmp[i++];
            else                  arr[k++] = tmp[j++];
        }
        while (i <= mid - left)  arr[k++] = tmp[i++];
        while (j <= right - left) arr[k++] = tmp[j++];
    }

    // ── Quick sort ────────────────────────────────────────────────────────────

    /**
     * Divide-and-conquer: partition around pivot, recursively sort partitions.
     * Uses median-of-three pivot selection to avoid worst-case on sorted input.
     * O(n log n) average, O(n²) worst, O(log n) stack space, NOT stable.
     */
    public static void quickSort(int[] arr) {
        if (arr.length <= 1) return;
        quickSortHelper(arr, 0, arr.length - 1);
    }

    private static void quickSortHelper(int[] arr, int low, int high) {
        if (low >= high) return;
        int pivotIdx = partition(arr, low, high);
        quickSortHelper(arr, low, pivotIdx - 1);
        quickSortHelper(arr, pivotIdx + 1, high);
    }

    private static int partition(int[] arr, int low, int high) {
        // Median-of-three pivot
        int mid = low + (high - low) / 2;
        if (arr[mid] < arr[low]) swap(arr, mid, low);
        if (arr[high] < arr[low]) swap(arr, high, low);
        if (arr[mid] < arr[high]) swap(arr, mid, high);
        int pivot = arr[high];

        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (arr[j] <= pivot) swap(arr, ++i, j);
        }
        swap(arr, i + 1, high);
        return i + 1;
    }

    private static void swap(int[] arr, int i, int j) {
        int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
    }

    // ── Heap sort ─────────────────────────────────────────────────────────────

    /**
     * Builds a max-heap, then repeatedly extracts the maximum.
     * O(n log n) all cases, O(1) space, NOT stable.
     */
    public static void heapSort(int[] arr) {
        int n = arr.length;
        // Build max-heap
        for (int i = n / 2 - 1; i >= 0; i--) heapify(arr, n, i);
        // Extract elements from heap
        for (int i = n - 1; i > 0; i--) {
            swap(arr, 0, i);
            heapify(arr, i, 0);
        }
    }

    private static void heapify(int[] arr, int n, int i) {
        int largest = i;
        int left  = 2 * i + 1;
        int right = 2 * i + 2;
        if (left  < n && arr[left]  > arr[largest]) largest = left;
        if (right < n && arr[right] > arr[largest]) largest = right;
        if (largest != i) {
            swap(arr, i, largest);
            heapify(arr, n, largest);
        }
    }

    // ── Counting sort ─────────────────────────────────────────────────────────

    /**
     * Non-comparison sort for non-negative integers in a known range.
     * O(n + k) where k = max value. Stable.
     */
    public static void countingSort(int[] arr) {
        if (arr.length == 0) return;
        int max = arr[0];
        for (int v : arr) if (v > max) max = v;

        int[] count = new int[max + 1];
        for (int v : arr) count[v]++;
        // Convert counts to prefix sums (stable sort positions)
        for (int i = 1; i <= max; i++) count[i] += count[i - 1];

        int[] output = new int[arr.length];
        for (int i = arr.length - 1; i >= 0; i--) {
            output[--count[arr[i]]] = arr[i];
        }
        System.arraycopy(output, 0, arr, 0, arr.length);
    }

    // ── Generic sort with Comparator ──────────────────────────────────────────

    /** Insertion sort for generic arrays using a Comparator. */
    public static <T> void insertionSort(T[] arr, Comparator<T> cmp) {
        for (int i = 1; i < arr.length; i++) {
            T key = arr[i];
            int j = i - 1;
            while (j >= 0 && cmp.compare(arr[j], key) > 0) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }
    }

    // ── Stability check helper ────────────────────────────────────────────────

    /**
     * Returns true if arr is sorted in non-decreasing order.
     */
    public static boolean isSorted(int[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i] > arr[i + 1]) return false;
        }
        return true;
    }
}
