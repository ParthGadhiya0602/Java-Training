package com.javatraining.controlflow;

import java.util.List;

/**
 * TOPIC: All loop types — for, for-each, while, do-while — and loop control
 * with break, continue, and labels.
 *
 * Every example is chosen to show a case where that specific loop is the
 * natural choice, not interchangeable with the others.
 */
public class LoopDemo {

    // -------------------------------------------------------------------------
    // for loop — known iteration count
    // -------------------------------------------------------------------------
    static void forLoopExamples() {
        // Classic: iterate over an index range
        System.out.print("0 to 4: ");
        for (int i = 0; i < 5; i++) {
            System.out.print(i + " ");
        }
        System.out.println();

        // Reverse iteration — natural for countdown, reverse traversal
        System.out.print("Countdown: ");
        for (int i = 5; i >= 0; i--) {
            System.out.print(i + " ");
        }
        System.out.println();

        // Step size > 1
        System.out.print("Evens 0-20: ");
        for (int i = 0; i <= 20; i += 2) {
            System.out.print(i + " ");
        }
        System.out.println();

        // Modifying array elements — must use index-based for (not for-each)
        int[] arr = {1, 2, 3, 4, 5};
        for (int i = 0; i < arr.length; i++) {
            arr[i] *= 2;            // double each element in-place
        }
        System.out.print("Doubled: ");
        for (int n : arr) System.out.print(n + " ");
        System.out.println();

        // Nested for loops — multiplication table
        System.out.println("3x3 multiplication table:");
        for (int i = 1; i <= 3; i++) {
            for (int j = 1; j <= 3; j++) {
                System.out.printf("%4d", i * j);
            }
            System.out.println();
        }
    }

    // -------------------------------------------------------------------------
    // Enhanced for-each — iterating collections and arrays (no index needed)
    // -------------------------------------------------------------------------
    static void forEachExamples() {
        List<String> cities = List.of("Mumbai", "Delhi", "Bengaluru", "Chennai");

        // for-each is cleaner than: for (int i = 0; i < cities.size(); i++)
        for (String city : cities) {
            System.out.println("City: " + city);
        }

        // Limitation: for-each gives a COPY of each element, not a reference
        // to the slot. You cannot modify primitive arrays with for-each.
        int[] numbers = {10, 20, 30};
        for (int n : numbers) {
            n = n * 10;           // modifies the local copy 'n', not numbers[]
        }
        System.out.println("numbers[0] is still: " + numbers[0]); // 10 (unchanged)
    }

    // -------------------------------------------------------------------------
    // while loop — iteration count unknown, check condition before each round
    // -------------------------------------------------------------------------
    static void whileExamples() {
        // Collatz sequence: keep applying rules until you reach 1
        // Nobody knows in advance how many steps any given number will take.
        int n = 27;
        int steps = 0;
        System.out.print("Collatz from 27: ");
        while (n != 1) {
            System.out.print(n + " ");
            n = (n % 2 == 0) ? n / 2 : 3 * n + 1;
            steps++;
        }
        System.out.println("1  (" + steps + " steps)");

        // Reading digits of a number from right to left
        int number = 98765;
        System.out.print("Digits of " + number + " (right to left): ");
        while (number > 0) {
            System.out.print(number % 10 + " ");   // last digit
            number /= 10;                           // remove last digit
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // do-while loop — body must run at least once
    // -------------------------------------------------------------------------
    static void doWhileExample() {
        // Simulate a menu-driven interaction (without real user input)
        // The menu must show at least once before any choice is made.
        int[] simulatedInputs = {2, 3, 1, 0};  // simulating user input sequence
        int inputIndex = 0;

        int choice;
        do {
            // Show menu
            System.out.println("\n--- Menu ---");
            System.out.println("1. Square a number");
            System.out.println("2. Cube a number");
            System.out.println("3. Factorial");
            System.out.println("0. Exit");

            choice = simulatedInputs[inputIndex++]; // simulate user picking a choice
            System.out.println("User chose: " + choice);

            switch (choice) {
                case 1 -> System.out.println("5² = " + (5 * 5));
                case 2 -> System.out.println("4³ = " + (4 * 4 * 4));
                case 3 -> System.out.println("6! = " + factorial(6));
                case 0 -> System.out.println("Goodbye!");
                default -> System.out.println("Invalid choice.");
            }
        } while (choice != 0);
        // The menu always shows at least once — even if choice==0 on first input
    }

    static long factorial(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) result *= i;
        return result;
    }

    // -------------------------------------------------------------------------
    // break — exit the loop early
    // -------------------------------------------------------------------------
    static void breakExample() {
        int[] data = {3, 7, 1, 9, -4, 6, 2, -8, 5};

        // Linear search — stop as soon as we find what we need
        int target = -4;
        int foundAt = -1;

        for (int i = 0; i < data.length; i++) {
            if (data[i] == target) {
                foundAt = i;
                break;      // no point checking the rest
            }
        }
        System.out.println("Found " + target + " at index: " + foundAt);

        // break in while — process a stream until a sentinel value
        int sum = 0;
        int[] stream = {5, 3, 8, 0, 2, 7};  // 0 is the sentinel: "stop here"
        int idx = 0;
        while (idx < stream.length) {
            if (stream[idx] == 0) break;   // stop at sentinel
            sum += stream[idx++];
        }
        System.out.println("Sum before sentinel: " + sum); // 5+3+8 = 16
    }

    // -------------------------------------------------------------------------
    // continue — skip this iteration, move to the next
    // -------------------------------------------------------------------------
    static void continueExample() {
        // Skip invalid entries and process only valid ones
        int[] readings = {42, -1, 87, -999, 65, -3, 91};

        int total = 0;
        int validCount = 0;

        for (int r : readings) {
            if (r < 0) {
                System.out.println("Skipping invalid reading: " + r);
                continue;   // jump to next iteration, skip everything below
            }
            total += r;
            validCount++;
        }

        System.out.printf("Valid readings: %d, Average: %.1f%n",
            validCount, (double) total / validCount);

        // continue in a for loop jumps to the UPDATE step (i++), not the top
        System.out.print("Skip multiples of 3: ");
        for (int i = 0; i <= 15; i++) {
            if (i % 3 == 0) continue;   // jumps to i++
            System.out.print(i + " ");
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Labels — target break/continue at a specific outer loop
    // -------------------------------------------------------------------------
    static void labelExamples() {
        // PROBLEM: Find first (row, col) pair in a matrix where value > threshold.
        // Plain break only exits the inner loop — outer keeps running.
        int[][] matrix = {
            {1,  3,  5},
            {7,  9,  2},
            {11, 4,  6}
        };
        int threshold = 8;

        // Using a label to break out of BOTH loops at once
        int foundRow = -1, foundCol = -1;

        search:                              // label on the outer loop
        for (int row = 0; row < matrix.length; row++) {
            for (int col = 0; col < matrix[row].length; col++) {
                if (matrix[row][col] > threshold) {
                    foundRow = row;
                    foundCol = col;
                    break search;            // exits BOTH loops immediately
                }
            }
        }
        System.out.printf("First value > %d: matrix[%d][%d] = %d%n",
            threshold, foundRow, foundCol, matrix[foundRow][foundCol]);

        // continue with a label — skip to the next row when a condition is met
        System.out.println("Rows that contain no negative numbers:");
        rowLoop:
        for (int row = 0; row < matrix.length; row++) {
            for (int col = 0; col < matrix[row].length; col++) {
                if (matrix[row][col] < 0) {
                    continue rowLoop;   // skip this row entirely, check next row
                }
            }
            System.out.println("  Row " + row + " is all-positive");
        }

        // NOTE: labels are a code smell if overused.
        // Often a better design is to extract the inner loop into a method:
        //   if (findInRow(matrix[row], threshold)) { ... break; }
        // That's cleaner, testable, and self-documenting.
    }

    public static void main(String[] args) {
        System.out.println("=== for Loop ===");
        forLoopExamples();

        System.out.println("\n=== for-each Loop ===");
        forEachExamples();

        System.out.println("\n=== while Loop ===");
        whileExamples();

        System.out.println("\n=== do-while Loop ===");
        doWhileExample();

        System.out.println("\n=== break ===");
        breakExample();

        System.out.println("\n=== continue ===");
        continueExample();

        System.out.println("\n=== Labels ===");
        labelExamples();
    }
}
