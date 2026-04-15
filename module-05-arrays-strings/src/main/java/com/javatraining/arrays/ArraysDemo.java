package com.javatraining.arrays;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * TOPIC: Arrays — declaration, initialization, copying, sorting, and the
 * Arrays utility class. Includes object arrays and shallow-copy gotcha.
 */
public class ArraysDemo {

    // -------------------------------------------------------------------------
    // Declaration, initialization, default values
    // -------------------------------------------------------------------------
    static void declarationAndDefaults() {
        // All elements default to 0 for int[]
        int[] scores = new int[5];
        System.out.println("Default int[]:    " + Arrays.toString(scores));    // [0,0,0,0,0]

        // Default false for boolean[]
        boolean[] flags = new boolean[3];
        System.out.println("Default boolean[]:" + Arrays.toString(flags));    // [false,false,false]

        // Default null for String[]
        String[] names = new String[3];
        System.out.println("Default String[]: " + Arrays.toString(names));    // [null,null,null]

        // Literal initialization
        int[] primes = {2, 3, 5, 7, 11, 13};
        System.out.println("Primes: " + Arrays.toString(primes));

        // Length property
        System.out.println("Length: " + primes.length);                       // 6
        System.out.println("Last:   " + primes[primes.length - 1]);           // 13
    }

    // -------------------------------------------------------------------------
    // Array operations: fill, copy, sort, search
    // -------------------------------------------------------------------------
    static void operations() {
        // Fill
        int[] data = new int[6];
        Arrays.fill(data, 7);
        System.out.println("Filled:          " + Arrays.toString(data));      // [7,7,7,7,7,7]
        Arrays.fill(data, 2, 5, 0);   // fill range [2,5)
        System.out.println("Partial fill:    " + Arrays.toString(data));      // [7,7,0,0,0,7]

        // Sort
        int[] unsorted = {38, 27, 43, 3, 9, 82, 10};
        Arrays.sort(unsorted);
        System.out.println("Sorted:          " + Arrays.toString(unsorted)); // [3,9,10,27,38,43,82]

        // Binary search (array must be sorted first)
        int idx = Arrays.binarySearch(unsorted, 27);
        System.out.println("Index of 27:     " + idx);                       // 3
        int missing = Arrays.binarySearch(unsorted, 50);
        System.out.println("Index of 50:     " + missing);                   // negative

        // Copy variants
        int[] src = {1, 2, 3, 4, 5};
        int[] copy1 = Arrays.copyOf(src, 3);           // truncate: [1,2,3]
        int[] copy2 = Arrays.copyOf(src, 8);           // pad:      [1,2,3,4,5,0,0,0]
        int[] copy3 = Arrays.copyOfRange(src, 1, 4);   // slice:    [2,3,4]
        int[] copy4 = src.clone();                      // full:     [1,2,3,4,5]

        System.out.println("copyOf(3):       " + Arrays.toString(copy1));
        System.out.println("copyOf(8):       " + Arrays.toString(copy2));
        System.out.println("copyOfRange:     " + Arrays.toString(copy3));
        System.out.println("clone():         " + Arrays.toString(copy4));
    }

    // -------------------------------------------------------------------------
    // Sorting objects with a Comparator
    // -------------------------------------------------------------------------
    record Employee(String name, int age, double salary) {}

    static void objectSorting() {
        Employee[] employees = {
            new Employee("Charlie", 35, 75_000),
            new Employee("Alice",   28, 95_000),
            new Employee("Bob",     42, 85_000),
            new Employee("Diana",   31, 92_000)
        };

        // Sort by name (natural order)
        Arrays.sort(employees, Comparator.comparing(Employee::name));
        System.out.println("\nSorted by name:");
        for (Employee e : employees) System.out.println("  " + e);

        // Sort by salary descending
        Arrays.sort(employees, Comparator.comparingDouble(Employee::salary).reversed());
        System.out.println("\nSorted by salary (desc):");
        for (Employee e : employees) System.out.println("  " + e);

        // Sort by age then name (chained comparators)
        Arrays.sort(employees,
            Comparator.comparingInt(Employee::age).thenComparing(Employee::name));
        System.out.println("\nSorted by age then name:");
        for (Employee e : employees) System.out.println("  " + e);
    }

    // -------------------------------------------------------------------------
    // 2D arrays and jagged arrays
    // -------------------------------------------------------------------------
    static void multiDimensional() {
        // 3×3 matrix
        int[][] matrix = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        System.out.println("\n3×3 matrix (deepToString): " + Arrays.deepToString(matrix));

        // Matrix traversal
        System.out.println("Matrix:");
        for (int[] row : matrix) {
            for (int val : row)
                System.out.printf("%4d", val);
            System.out.println();
        }

        // Jagged array: Pascal's triangle (5 rows)
        int[][] pascal = new int[5][];
        for (int i = 0; i < pascal.length; i++) {
            pascal[i] = new int[i + 1];
            pascal[i][0] = 1;
            pascal[i][i] = 1;
            for (int j = 1; j < i; j++)
                pascal[i][j] = pascal[i-1][j-1] + pascal[i-1][j];
        }
        System.out.println("\nPascal's Triangle:");
        for (int[] row : pascal)
            System.out.println("  " + Arrays.toString(row));
    }

    // -------------------------------------------------------------------------
    // Shallow copy gotcha with object arrays
    // -------------------------------------------------------------------------
    static void shallowCopyGotcha() {
        // StringBuilder is mutable — copying the array does NOT copy the objects
        StringBuilder[] originals = {
            new StringBuilder("alpha"),
            new StringBuilder("beta"),
            new StringBuilder("gamma")
        };
        StringBuilder[] copy = originals.clone();  // shallow: same SB objects

        copy[0].append("_modified");  // modifies the SHARED object
        System.out.println("\nShallow copy gotcha:");
        System.out.println("originals[0] = " + originals[0]); // "alpha_modified" — affected!
        System.out.println("copy[0]      = " + copy[0]);      // "alpha_modified"

        // But reassigning an element in the copy does NOT affect the original array
        copy[1] = new StringBuilder("replaced");
        System.out.println("originals[1] = " + originals[1]); // "beta"    — unaffected
        System.out.println("copy[1]      = " + copy[1]);      // "replaced"
    }

    // -------------------------------------------------------------------------
    // Arrays.asList() trap — fixed-size list
    // -------------------------------------------------------------------------
    static void asListTrap() {
        String[] arr = {"a", "b", "c"};
        List<String> fixedList = Arrays.asList(arr);  // backed by the array

        fixedList.set(0, "A");              // OK — set() is supported
        System.out.println("\nAfter set(0, A): " + fixedList);  // [A, b, c]
        System.out.println("arr[0] also changed: " + arr[0]);   // "A" — backed by same array!

        try {
            fixedList.add("d");             // THROWS UnsupportedOperationException
        } catch (UnsupportedOperationException e) {
            System.out.println("add() threw: UnsupportedOperationException");
        }

        // Correct way to get a mutable list from an array
        List<String> mutable = new java.util.ArrayList<>(Arrays.asList(arr));
        mutable.add("d");                   // OK — independent copy
        System.out.println("Mutable list: " + mutable);
    }

    public static void main(String[] args) {
        System.out.println("=== Declaration & Defaults ===");
        declarationAndDefaults();

        System.out.println("\n=== Operations ===");
        operations();

        System.out.println("\n=== Object Sorting ===");
        objectSorting();

        System.out.println("\n=== Multi-Dimensional ===");
        multiDimensional();

        System.out.println("\n=== Shallow Copy Gotcha ===");
        shallowCopyGotcha();

        System.out.println("\n=== Arrays.asList() Trap ===");
        asListTrap();
    }
}
