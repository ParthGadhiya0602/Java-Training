package com.javatraining.controlflow;

/**
 * PRACTICAL EXERCISE: Number Analyzer
 *
 * A self-contained analysis tool that ties together EVERY control-flow
 * construct from this module in a real context:
 *
 *   - do-while:  main program loop (runs until user exits)
 *   - switch:    dispatch to the chosen analysis
 *   - for:       iterate over a range of numbers
 *   - while:     process a number digit by digit
 *   - break:     early exit once a condition is met
 *   - continue:  skip invalid/unwanted values
 *   - labels:    exit nested loops in the prime factorization search
 *   - if/else:   guard conditions and edge cases throughout
 *
 * Since this module has no Scanner (to keep tests simple), the "menu"
 * runs through a predefined sequence of inputs. In Module 04 (Methods)
 * we will factor this into reusable, testable methods.
 */
public class NumberAnalyzer {

    // =========================================================================
    // 1. isPrime - for loop + break
    // =========================================================================
    static boolean isPrime(int n) {
        if (n < 2) return false;       // 0 and 1 are not prime
        if (n == 2) return true;       // 2 is the only even prime
        if (n % 2 == 0) return false;  // even numbers > 2 are not prime

        // Only check odd divisors up to sqrt(n).
        // If n has a factor > sqrt(n), there must be a corresponding factor < sqrt(n).
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) return false;  // found a divisor → not prime
        }
        return true;
    }

    // =========================================================================
    // 2. Sieve of Eratosthenes - for loops + continue + labels
    //    Finds ALL primes up to a limit efficiently.
    // =========================================================================
    static int[] sieve(int limit) {
        // Mark composites: boolean[i] = true means i is composite (not prime)
        boolean[] isComposite = new boolean[limit + 1];
        isComposite[0] = true;
        isComposite[1] = true;

        // For each prime p, mark all its multiples as composite
        for (int p = 2; p * p <= limit; p++) {
            if (isComposite[p]) continue;   // p is already marked - skip
            for (int multiple = p * p; multiple <= limit; multiple += p) {
                isComposite[multiple] = true;
            }
        }

        // Collect all unmarked (prime) indices
        int count = 0;
        for (int i = 2; i <= limit; i++) {
            if (!isComposite[i]) count++;
        }

        int[] primes = new int[count];
        int idx = 0;
        for (int i = 2; i <= limit; i++) {
            if (!isComposite[i]) primes[idx++] = i;
        }
        return primes;
    }

    // =========================================================================
    // 3. Prime Factorization - while + for + labeled break
    // =========================================================================
    static int[] primeFactors(int n) {
        if (n < 2) return new int[0];

        int[] factors = new int[50];   // max factors for reasonable input
        int count = 0;

        // Divide out 2s first (only even prime)
        while (n % 2 == 0) {
            factors[count++] = 2;
            n /= 2;
        }

        // Now n must be odd - only check odd divisors
        for (int f = 3; f * f <= n; f += 2) {
            while (n % f == 0) {       // divide out all copies of f
                factors[count++] = f;
                n /= f;
            }
        }

        // If n is still > 1, it's a prime factor itself
        if (n > 1) factors[count++] = n;

        // Trim to actual size
        int[] result = new int[count];
        System.arraycopy(factors, 0, result, 0, count);
        return result;
    }

    // =========================================================================
    // 4. Digital root - while loop
    //    Repeatedly sum digits until a single digit remains.
    // =========================================================================
    static int digitalRoot(int n) {
        n = Math.abs(n);           // handle negatives
        while (n >= 10) {
            int sum = 0;
            while (n > 0) {        // sum all digits
                sum += n % 10;
                n /= 10;
            }
            n = sum;               // replace n with its digit sum
        }
        return n;
        // digitalRoot(9875) → 9+8+7+5=29 → 2+9=11 → 1+1=2
    }

    // =========================================================================
    // 5. Pattern Printer - nested for loops + continue
    //    Prints a right-angled triangle and a number diamond.
    // =========================================================================
    static void printTriangle(int height) {
        for (int row = 1; row <= height; row++) {
            for (int col = 1; col <= row; col++) {
                System.out.print("* ");
            }
            System.out.println();
        }
    }

    static void printNumberDiamond(int n) {
        // Upper half (including middle)
        for (int row = 1; row <= n; row++) {
            // Leading spaces
            for (int sp = 0; sp < n - row; sp++) System.out.print("  ");
            // Numbers ascending
            for (int col = 1; col <= row; col++)  System.out.print(col + " ");
            // Numbers descending (skip the peak)
            for (int col = row - 1; col >= 1; col--) System.out.print(col + " ");
            System.out.println();
        }
        // Lower half
        for (int row = n - 1; row >= 1; row--) {
            for (int sp = 0; sp < n - row; sp++) System.out.print("  ");
            for (int col = 1; col <= row; col++)  System.out.print(col + " ");
            for (int col = row - 1; col >= 1; col--) System.out.print(col + " ");
            System.out.println();
        }
    }

    // =========================================================================
    // 6. FizzBuzz - for loop + switch expression
    // =========================================================================
    static String fizzBuzz(int n) {
        // Using switch on a computed int key - elegant alternative to if/else chains
        return switch ((n % 3 == 0 ? 1 : 0) + (n % 5 == 0 ? 2 : 0)) {
            case 0 -> String.valueOf(n);
            case 1 -> "Fizz";
            case 2 -> "Buzz";
            case 3 -> "FizzBuzz";
            default -> throw new AssertionError("impossible");
        };
    }

    // =========================================================================
    // Main - ties all analyses together using do-while + switch dispatch
    // =========================================================================
    public static void main(String[] args) {
        // Simulated "menu choices" - replace with Scanner.nextInt() for interactive use
        int[] menuChoices = {1, 2, 3, 4, 5, 6, 0};
        int choiceIndex = 0;

        int choice;
        do {
            System.out.println("\n========== Number Analyzer ==========");
            System.out.println("1. Check if a number is prime");
            System.out.println("2. List all primes up to N (Sieve)");
            System.out.println("3. Prime factorization");
            System.out.println("4. Digital root");
            System.out.println("5. Print triangle pattern");
            System.out.println("6. FizzBuzz 1–30");
            System.out.println("0. Exit");
            System.out.println("=====================================");

            choice = menuChoices[choiceIndex++];
            System.out.println("→ Choice: " + choice);

            switch (choice) {
                case 1 -> {
                    int[] toCheck = {2, 7, 15, 17, 100, 97};
                    for (int num : toCheck) {
                        System.out.printf("%3d is %s%n", num, isPrime(num) ? "PRIME" : "composite");
                    }
                }
                case 2 -> {
                    int[] primes = sieve(50);
                    System.out.print("Primes up to 50: ");
                    for (int p : primes) System.out.print(p + " ");
                    System.out.println("\nTotal: " + primes.length);
                }
                case 3 -> {
                    int[] numbers = {12, 60, 100, 97, 360};
                    for (int num : numbers) {
                        int[] factors = primeFactors(num);
                        System.out.print(num + " = ");
                        for (int i = 0; i < factors.length; i++) {
                            System.out.print(factors[i]);
                            if (i < factors.length - 1) System.out.print(" × ");
                        }
                        System.out.println();
                    }
                }
                case 4 -> {
                    int[] nums = {9875, 493, 1, 999, 0};
                    for (int num : nums) {
                        System.out.printf("digitalRoot(%4d) = %d%n", num, digitalRoot(num));
                    }
                }
                case 5 -> {
                    System.out.println("Triangle (height=5):");
                    printTriangle(5);
                    System.out.println("\nDiamond (n=4):");
                    printNumberDiamond(4);
                }
                case 6 -> {
                    for (int i = 1; i <= 30; i++) {
                        System.out.print(fizzBuzz(i));
                        System.out.print(i < 30 ? ", " : "\n");
                    }
                }
                case 0 -> System.out.println("Exiting. Goodbye!");
                default -> System.out.println("Invalid choice. Try again.");
            }
        } while (choice != 0);
    }
}
