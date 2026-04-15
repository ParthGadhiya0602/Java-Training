package com.javatraining.methods;

import java.util.Arrays;

/**
 * TOPIC: Pass-by-value — Java's only argument-passing mechanism.
 *
 * Three distinct scenarios every Java developer must understand:
 *   1. Primitive argument    — caller NEVER sees changes inside method
 *   2. Object mutation       — caller SEES changes made to the object's state
 *   3. Reference reassignment — caller NEVER sees the reassignment
 */
public class PassByValueDemo {

    // =========================================================================
    // SCENARIO 1: Primitive — a copy of the value is passed
    // =========================================================================
    static void doubleIt(int x) {
        x = x * 2;   // modifies the LOCAL copy — original is untouched
    }

    // The classic "swap" method that doesn't work in Java for primitives:
    static void swap(int a, int b) {
        int temp = a;
        a = b;
        b = temp;
        // a and b are local copies — caller's variables are unchanged
    }

    // =========================================================================
    // SCENARIO 2: Object mutation — the reference copy points to the same object
    // =========================================================================
    static void appendItems(int[] arr) {
        // Mutating the object (arr[0] = ...) IS visible because both the
        // caller and this method hold references to the SAME array on the heap.
        for (int i = 0; i < arr.length; i++) {
            arr[i] *= 10;
        }
    }

    static void growList(java.util.List<String> list) {
        list.add("added by method");  // mutates the same list object
    }

    // =========================================================================
    // SCENARIO 3: Reference reassignment — never visible to caller
    // =========================================================================
    static void tryToReplace(int[] arr) {
        arr = new int[]{999, 888, 777};  // only changes the LOCAL reference
        // The caller's 'data' still points to the original array
    }

    static void tryToReplaceList(java.util.List<String> list) {
        list = new java.util.ArrayList<>();  // local reference reassigned
        list.add("this won't be seen");      // caller's list is unchanged
    }

    // =========================================================================
    // Practical: a method that needs to "return" two values
    // =========================================================================

    // Wrong attempt: can't modify int parameters
    static void minMaxWrong(int[] data, int min, int max) {
        min = data[0]; max = data[0];
        for (int n : data) {
            if (n < min) min = n;
            if (n > max) max = n;
        }
        // min and max are local — caller sees nothing
    }

    // Solution 1: use a result array (mutation IS visible)
    static void minMax(int[] data, int[] result) {
        result[0] = data[0];  // result[0] = min
        result[1] = data[0];  // result[1] = max
        for (int n : data) {
            if (n < result[0]) result[0] = n;
            if (n > result[1]) result[1] = n;
        }
    }

    // Solution 2 (preferred): return a record — clean, immutable, type-safe
    record MinMax(int min, int max) {}

    static MinMax minMaxClean(int[] data) {
        int min = data[0], max = data[0];
        for (int n : data) {
            if (n < min) min = n;
            if (n > max) max = n;
        }
        return new MinMax(min, max);
    }

    // =========================================================================
    // Defensive copying — when a method SHOULD NOT let callers mutate internals
    // =========================================================================
    static class ImmutableCalendar {
        private final int[] holidays;  // should never be changed from outside

        ImmutableCalendar(int[] holidayDays) {
            // Defensive copy: take a copy, not the caller's array
            this.holidays = Arrays.copyOf(holidayDays, holidayDays.length);
        }

        int[] getHolidays() {
            // Return a copy too — don't expose the internal array
            return Arrays.copyOf(holidays, holidays.length);
        }
    }

    public static void main(String[] args) {
        // ------- Scenario 1: primitives -------
        System.out.println("=== Primitive (no effect on caller) ===");
        int n = 10;
        doubleIt(n);
        System.out.println("n after doubleIt: " + n);  // still 10

        int x = 3, y = 7;
        swap(x, y);
        System.out.printf("x=%d y=%d after swap (unchanged)%n", x, y); // x=3, y=7

        // ------- Scenario 2: object mutation -------
        System.out.println("\n=== Object mutation (visible to caller) ===");
        int[] data = {1, 2, 3, 4, 5};
        appendItems(data);
        System.out.println("After appendItems: " + Arrays.toString(data)); // [10,20,30,40,50]

        var list = new java.util.ArrayList<String>();
        list.add("original");
        growList(list);
        System.out.println("After growList: " + list); // [original, added by method]

        // ------- Scenario 3: reference reassignment -------
        System.out.println("\n=== Reference reassignment (invisible to caller) ===");
        int[] arr = {1, 2, 3};
        tryToReplace(arr);
        System.out.println("After tryToReplace: " + Arrays.toString(arr)); // [1, 2, 3] unchanged

        var list2 = new java.util.ArrayList<String>();
        list2.add("original");
        tryToReplaceList(list2);
        System.out.println("After tryToReplaceList: " + list2); // [original]

        // ------- Returning multiple values -------
        System.out.println("\n=== Returning multiple values ===");
        int[] numbers = {5, 2, 8, 1, 9, 3};

        // Solution 1: out-array
        int[] result = new int[2];
        minMax(numbers, result);
        System.out.println("Via array: min=" + result[0] + " max=" + result[1]);

        // Solution 2: record (preferred)
        MinMax mm = minMaxClean(numbers);
        System.out.println("Via record: " + mm);  // MinMax[min=1, max=9]

        // ------- Defensive copy -------
        System.out.println("\n=== Defensive Copy ===");
        int[] myHolidays = {1, 15, 26, 78, 100};
        ImmutableCalendar cal = new ImmutableCalendar(myHolidays);
        myHolidays[0] = 999;             // try to tamper with internal state
        System.out.println("Cal first holiday: " + cal.getHolidays()[0]); // still 1

        int[] retrieved = cal.getHolidays();
        retrieved[0] = 888;              // try to tamper via getter
        System.out.println("Cal first holiday: " + cal.getHolidays()[0]); // still 1
    }
}
