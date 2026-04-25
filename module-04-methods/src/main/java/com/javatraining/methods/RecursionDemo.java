package com.javatraining.methods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TOPIC: Recursion - base case, recursive case, call stack, memoization.
 *
 * Covers:
 * - Factorial: simplest recursion (base + recursive case)
 * - Fibonacci: naive O(2^n) vs memoized O(n) - why memoization matters
 * - Binary search: divide-and-conquer, why (low+high)/2 overflows
 * - Merge sort: recursive sort - split, sort, merge
 * - Tower of Hanoi: classic puzzle with elegant recursive solution
 * - Power set: exponential output, shows recursion depth management
 * - Tail-recursive simulation: converting to loop when stack depth is a concern
 */
public class RecursionDemo {

    // =========================================================================
    // 1. FACTORIAL - simplest example: n! = n × (n-1)!
    // =========================================================================
    static long factorial(int n) {
        if (n < 0)  throw new IllegalArgumentException("n must be >= 0");
        if (n == 0) return 1;                 // base case: 0! = 1
        return (long) n * factorial(n - 1);   // recursive case
    }

    // Iterative equivalent - same result, no stack frames
    static long factorialIterative(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) result *= i;
        return result;
    }

    // =========================================================================
    // 2. FIBONACCI - naive vs memoized
    //    Demonstrates why naive recursion can be exponential and how
    //    memoization fixes it with the same recursive structure.
    // =========================================================================

    // Naive: O(2^n) - fib(40) makes ~2 billion calls
    static long fibNaive(int n) {
        if (n <= 1) return n;                          // base cases: fib(0)=0, fib(1)=1
        return fibNaive(n - 1) + fibNaive(n - 2);     // two recursive calls (problem!)
    }
    /*
     Call tree for fib(5):
                        fib(5)
                       /      \
                   fib(4)     fib(3)
                  /    \      /    \
              fib(3) fib(2) fib(2) fib(1)
              / \    / \    / \
          fib(2) fib(1) ...

     fib(3) is computed TWICE, fib(2) THREE times - exponential redundancy.
    */

    // Memoized: O(n) - cache results, never recompute
    static long fibMemo(int n, long[] cache) {
        if (n <= 1) return n;
        if (cache[n] != 0) return cache[n];        // already computed - reuse
        cache[n] = fibMemo(n - 1, cache) + fibMemo(n - 2, cache);
        return cache[n];
    }

    // Public API that hides the cache
    static long fib(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        return fibMemo(n, new long[n + 1]);
    }

    // Bottom-up dynamic programming - O(n) time, O(1) space (best approach)
    static long fibDP(int n) {
        if (n <= 1) return n;
        long prev2 = 0, prev1 = 1;
        for (int i = 2; i <= n; i++) {
            long curr = prev1 + prev2;
            prev2 = prev1;
            prev1 = curr;
        }
        return prev1;
    }

    // =========================================================================
    // 3. BINARY SEARCH - divide and conquer on a sorted array
    // =========================================================================
    static int binarySearch(int[] arr, int target, int low, int high) {
        if (low > high) return -1;                    // base case: not found

        // IMPORTANT: use low + (high - low) / 2, NOT (low + high) / 2
        // (low + high) can overflow int when both are large positive values
        int mid = low + (high - low) / 2;

        if (arr[mid] == target) return mid;           // base case: found
        if (arr[mid] < target)
            return binarySearch(arr, target, mid + 1, high); // search right half
        else
            return binarySearch(arr, target, low, mid - 1);  // search left half
    }

    static int binarySearch(int[] arr, int target) {
        return binarySearch(arr, target, 0, arr.length - 1);
    }

    // =========================================================================
    // 4. MERGE SORT - recursive divide-and-conquer sorting O(n log n)
    // =========================================================================
    static void mergeSort(int[] arr, int left, int right) {
        if (left >= right) return;      // base case: 0 or 1 elements - already sorted

        int mid = left + (right - left) / 2;
        mergeSort(arr, left, mid);      // sort left half
        mergeSort(arr, mid + 1, right); // sort right half
        merge(arr, left, mid, right);   // merge the two sorted halves
    }

    private static void merge(int[] arr, int left, int mid, int right) {
        // Copy both halves into a temporary array
        int[] temp = Arrays.copyOfRange(arr, left, right + 1);
        int leftIdx  = 0;
        int rightIdx = mid - left + 1;
        int mergeIdx = left;

        // Merge by picking the smaller element from each half
        while (leftIdx <= mid - left && rightIdx <= right - left) {
            if (temp[leftIdx] <= temp[rightIdx])
                arr[mergeIdx++] = temp[leftIdx++];
            else
                arr[mergeIdx++] = temp[rightIdx++];
        }
        // Copy any remaining elements from the left half
        while (leftIdx <= mid - left)   arr[mergeIdx++] = temp[leftIdx++];
        // Copy any remaining elements from the right half
        while (rightIdx <= right - left) arr[mergeIdx++] = temp[rightIdx++];
    }

    static int[] mergeSort(int[] arr) {
        int[] copy = Arrays.copyOf(arr, arr.length);
        mergeSort(copy, 0, copy.length - 1);
        return copy;
    }

    // =========================================================================
    // 5. TOWER OF HANOI
    //    Move n disks from 'from' peg to 'to' peg using 'via' as auxiliary.
    //    Minimum moves required: 2^n - 1
    // =========================================================================
    static int hanoiMoves = 0; // count moves for verification

    static void hanoi(int n, String from, String to, String via) {
        if (n == 0) return;                    // base case: no disk to move
        hanoi(n - 1, from, via, to);           // move top n-1 disks out of the way
        System.out.printf("  Move disk %d: %s → %s%n", n, from, to);
        hanoiMoves++;
        hanoi(n - 1, via, to, from);           // move n-1 disks to destination
    }
    /*
     Why it works for n=3 (7 moves = 2^3 - 1):
     hanoi(3, A, C, B)
       hanoi(2, A, B, C)         move top 2 from A to B (using C)
         hanoi(1, A, C, B)       move top 1 from A to C
           Move disk 1: A → C
         Move disk 2: A → B
         hanoi(1, C, B, A)       move disk 1 from C to B
           Move disk 1: C → B
       Move disk 3: A → C
       hanoi(2, B, C, A)         move 2 disks from B to C (using A)
         ...
    */

    // =========================================================================
    // 6. POWER SET - all subsets of a set (2^n subsets)
    //    Classic example of building results through recursion
    // =========================================================================
    static List<List<Integer>> powerSet(int[] set) {
        List<List<Integer>> result = new ArrayList<>();
        generateSubsets(set, 0, new ArrayList<>(), result);
        return result;
    }

    private static void generateSubsets(int[] set, int index,
                                        List<Integer> current,
                                        List<List<Integer>> result) {
        if (index == set.length) {
            result.add(new ArrayList<>(current)); // base case: add current subset
            return;
        }
        // Choice 1: EXCLUDE set[index] from current subset
        generateSubsets(set, index + 1, current, result);

        // Choice 2: INCLUDE set[index] in current subset
        current.add(set[index]);
        generateSubsets(set, index + 1, current, result);
        current.remove(current.size() - 1);  // backtrack: restore state
    }

    // =========================================================================
    // 7. TAIL RECURSION → ITERATION conversion
    //    Java does NOT optimize tail calls, so deep tail recursion still
    //    risks StackOverflow. Always convert to a loop for large inputs.
    // =========================================================================

    // Tail-recursive sum - Java won't optimize this
    static long sumTailRec(int n, long accumulator) {
        if (n == 0) return accumulator;        // base case
        return sumTailRec(n - 1, accumulator + n); // tail call (last operation)
    }

    // Iterative equivalent - safe for any n
    static long sumIterative(int n) {
        long acc = 0;
        while (n > 0) { acc += n--; }
        return acc;
    }
    // Both compute 1+2+3+...+n, but iterative is safe for n=100_000.
    // sumTailRec(100_000, 0) would throw StackOverflowError.

    // =========================================================================
    // Main
    // =========================================================================
    public static void main(String[] args) {
        System.out.println("=== Factorial ===");
        for (int i = 0; i <= 10; i++)
            System.out.printf("%2d! = %d%n", i, factorial(i));

        System.out.println("\n=== Fibonacci: naive vs memoized vs DP ===");
        System.out.println("fib(10)  naive=" + fibNaive(10)
            + " memo=" + fib(10) + " dp=" + fibDP(10));
        System.out.println("fib(50)  dp=" + fibDP(50) + " (naive would take forever)");

        System.out.println("\n=== Binary Search ===");
        int[] sorted = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19};
        System.out.println("Search 11 → index " + binarySearch(sorted, 11)); // 5
        System.out.println("Search 6  → index " + binarySearch(sorted, 6));  // -1
        System.out.println("Search 1  → index " + binarySearch(sorted, 1));  // 0

        System.out.println("\n=== Merge Sort ===");
        int[] unsorted = {38, 27, 43, 3, 9, 82, 10};
        System.out.println("Before: " + Arrays.toString(unsorted));
        System.out.println("After:  " + Arrays.toString(mergeSort(unsorted)));

        System.out.println("\n=== Tower of Hanoi (n=3) ===");
        hanoiMoves = 0;
        hanoi(3, "A", "C", "B");
        System.out.println("Total moves: " + hanoiMoves + " (2^3 - 1 = 7)");

        System.out.println("\n=== Power Set of {1,2,3} ===");
        List<List<Integer>> subsets = powerSet(new int[]{1, 2, 3});
        System.out.println("Total subsets: " + subsets.size() + " (2^3 = 8)");
        subsets.forEach(System.out::println);

        System.out.println("\n=== Tail Recursion vs Iteration ===");
        System.out.println("sumTailRec(100, 0) = " + sumTailRec(100, 0));
        System.out.println("sumIterative(100)  = " + sumIterative(100));
        System.out.println("sumIterative(1_000_000) = " + sumIterative(1_000_000));
        // sumTailRec(1_000_000, 0) → StackOverflowError (Java doesn't optimize tail calls)
    }
}
