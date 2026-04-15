package com.javatraining.methods;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecursionDemo — correctness of recursive algorithms")
class RecursionTest {

    @ParameterizedTest(name = "{0}! = {1}")
    @CsvSource({
        "0,  1",
        "1,  1",
        "5,  120",
        "10, 3628800",
    })
    void factorial(int n, long expected) {
        assertEquals(expected, RecursionDemo.factorial(n));
    }

    @Test
    @DisplayName("factorial throws for negative input")
    void factorialNegative() {
        assertThrows(IllegalArgumentException.class, () -> RecursionDemo.factorial(-1));
    }

    @Test
    @DisplayName("factorial and factorialIterative produce identical results")
    void factorialMatchesIterative() {
        for (int i = 0; i <= 15; i++) {
            assertEquals(RecursionDemo.factorialIterative(i), RecursionDemo.factorial(i),
                "Mismatch at n=" + i);
        }
    }

    @ParameterizedTest(name = "fib({0}) = {1}")
    @CsvSource({
        "0,  0",
        "1,  1",
        "2,  1",
        "10, 55",
        "20, 6765",
        "50, 12586269025",
    })
    void fibonacci(int n, long expected) {
        assertEquals(expected, RecursionDemo.fib(n));
        assertEquals(expected, RecursionDemo.fibDP(n));
    }

    @Test
    @DisplayName("binarySearch finds element in sorted array")
    void binarySearchFound() {
        int[] arr = {1, 3, 5, 7, 9, 11, 13};
        assertEquals(0, RecursionDemo.binarySearch(arr, 1));   // first
        assertEquals(6, RecursionDemo.binarySearch(arr, 13));  // last
        assertEquals(3, RecursionDemo.binarySearch(arr, 7));   // middle
    }

    @Test
    @DisplayName("binarySearch returns -1 when element not present")
    void binarySearchNotFound() {
        int[] arr = {1, 3, 5, 7, 9};
        assertEquals(-1, RecursionDemo.binarySearch(arr, 4));
        assertEquals(-1, RecursionDemo.binarySearch(arr, 0));
        assertEquals(-1, RecursionDemo.binarySearch(arr, 100));
    }

    @Test
    @DisplayName("mergeSort produces a sorted copy without modifying original")
    void mergeSort() {
        int[] original = {38, 27, 43, 3, 9, 82, 10};
        int[] sorted   = RecursionDemo.mergeSort(original);

        assertArrayEquals(new int[]{3, 9, 10, 27, 38, 43, 82}, sorted);
        // Original must be unchanged
        assertArrayEquals(new int[]{38, 27, 43, 3, 9, 82, 10}, original);
    }

    @Test
    @DisplayName("Tower of Hanoi uses exactly 2^n - 1 moves")
    void hanoiMoveCount() {
        for (int n = 1; n <= 6; n++) {
            RecursionDemo.hanoiMoves = 0;
            RecursionDemo.hanoi(n, "A", "C", "B");
            int expected = (1 << n) - 1;  // 2^n - 1
            assertEquals(expected, RecursionDemo.hanoiMoves,
                "n=" + n + " should take " + expected + " moves");
        }
    }

    @Test
    @DisplayName("Power set of size n has exactly 2^n subsets")
    void powerSetSize() {
        for (int n = 0; n <= 5; n++) {
            int[] set = new int[n];
            for (int i = 0; i < n; i++) set[i] = i + 1;
            int expected = 1 << n; // 2^n
            assertEquals(expected, RecursionDemo.powerSet(set).size(),
                "Power set of size " + n + " should have " + expected + " subsets");
        }
    }

    @Test
    @DisplayName("sumTailRec and sumIterative agree for small n")
    void tailRecVsIterative() {
        for (int n = 0; n <= 100; n++) {
            assertEquals(RecursionDemo.sumIterative(n),
                         RecursionDemo.sumTailRec(n, 0),
                         "Mismatch at n=" + n);
        }
    }
}
