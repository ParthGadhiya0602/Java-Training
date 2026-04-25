package com.javatraining.arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Arrays - operations, sorting, copying, gotchas")
class ArraysTest {

    @Test
    @DisplayName("Default int[] elements are 0")
    void defaultValues() {
        int[] arr = new int[5];
        for (int val : arr) assertEquals(0, val);
    }

    @Test
    @DisplayName("Arrays.sort sorts in ascending order")
    void sort() {
        int[] data = {5, 2, 8, 1, 9, 3};
        Arrays.sort(data);
        assertArrayEquals(new int[]{1, 2, 3, 5, 8, 9}, data);
    }

    @Test
    @DisplayName("Arrays.binarySearch returns correct index in sorted array")
    void binarySearch() {
        int[] sorted = {1, 2, 3, 5, 8, 9};
        assertEquals(2, Arrays.binarySearch(sorted, 3));
        assertTrue(Arrays.binarySearch(sorted, 7) < 0, "Missing element should return negative");
    }

    @Test
    @DisplayName("Arrays.copyOf truncates or pads with zeros")
    void copyOf() {
        int[] src = {1, 2, 3, 4, 5};
        assertArrayEquals(new int[]{1, 2, 3},          Arrays.copyOf(src, 3));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 0}, Arrays.copyOf(src, 6));
    }

    @Test
    @DisplayName("Arrays.copyOfRange returns the specified slice")
    void copyOfRange() {
        int[] src = {10, 20, 30, 40, 50};
        assertArrayEquals(new int[]{20, 30, 40}, Arrays.copyOfRange(src, 1, 4));
    }

    @Test
    @DisplayName("Shallow copy: cloning object array shares references")
    void shallowCopySharesObjects() {
        StringBuilder[] orig = {new StringBuilder("hello"), new StringBuilder("world")};
        StringBuilder[] copy = orig.clone();

        // Same objects - mutation via copy is visible through orig
        copy[0].append("!");
        assertEquals("hello!", orig[0].toString());

        // Reassigning in copy does NOT affect orig
        copy[1] = new StringBuilder("new");
        assertEquals("world", orig[1].toString());
    }

    @Test
    @DisplayName("Arrays.asList returns fixed-size list - add() throws")
    void asListIsFixedSize() {
        String[] arr = {"a", "b", "c"};
        List<String> fixedList = Arrays.asList(arr);

        // set() is allowed
        fixedList.set(0, "A");
        assertEquals("A", arr[0], "asList is backed by the array");

        // add() throws
        assertThrows(UnsupportedOperationException.class, () -> fixedList.add("d"));
    }

    @Test
    @DisplayName("Arrays.deepEquals compares 2D arrays by content")
    void deepEquals() {
        int[][] a = {{1, 2}, {3, 4}};
        int[][] b = {{1, 2}, {3, 4}};
        assertFalse(Arrays.equals(a, b),    "equals() is shallow - compares references");
        assertTrue(Arrays.deepEquals(a, b), "deepEquals() compares content recursively");
    }

    @Test
    @DisplayName("Pascal's triangle row n has n+1 elements summing to 2^n")
    void pascalsTriangle() {
        int[][] pascal = new int[5][];
        for (int i = 0; i < pascal.length; i++) {
            pascal[i] = new int[i + 1];
            pascal[i][0] = pascal[i][i] = 1;
            for (int j = 1; j < i; j++)
                pascal[i][j] = pascal[i-1][j-1] + pascal[i-1][j];
        }
        for (int i = 0; i < pascal.length; i++) {
            int sum = Arrays.stream(pascal[i]).sum();
            assertEquals(1 << i, sum, "Row " + i + " should sum to 2^" + i);
        }
    }
}
