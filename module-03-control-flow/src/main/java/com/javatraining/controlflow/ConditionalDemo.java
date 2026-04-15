package com.javatraining.controlflow;

/**
 * TOPIC: if/else — branching based on boolean conditions.
 *
 * Covers:
 * - Basic if/else/else-if chains
 * - The dangling else trap (why braces matter)
 * - Common boolean mistakes
 * - Ternary operator as an expression-level if/else
 */
public class ConditionalDemo {

    // -------------------------------------------------------------------------
    // Basic if/else/else-if
    // -------------------------------------------------------------------------
    static String classify(int score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Score must be 0–100, got: " + score);
        } else if (score >= 90) {
            return "A";
        } else if (score >= 80) {
            return "B";
        } else if (score >= 70) {
            return "C";
        } else if (score >= 60) {
            return "D";
        } else {
            return "F";
        }
        // Note: every if-else-if chain should end with an else.
        // Without it, the method could implicitly return nothing, which
        // the compiler catches here but is easy to miss in void methods.
    }

    // -------------------------------------------------------------------------
    // Dangling else — the brace pitfall
    // -------------------------------------------------------------------------
    static void danglingElseDemo() {
        int x = 10, y = 5;

        // This looks like the else belongs to if(x > 0) — it does NOT.
        // Java attaches else to the NEAREST if: if(y > 10)
        if (x > 0)
            if (y > 10)
                System.out.println("y > 10");
        else
            // This else belongs to if(y > 10), NOT if(x > 0)
            System.out.println("This prints when y <= 10, NOT when x <= 0");

        System.out.println("With x=10, y=5 → prints the else despite x being positive");

        // Correct version — always use braces
        if (x > 0) {
            if (y > 10) {
                System.out.println("y > 10");
            }
        } else {
            System.out.println("x <= 0");  // now correctly attached to if(x > 0)
        }
    }

    // -------------------------------------------------------------------------
    // Boolean expression traps
    // -------------------------------------------------------------------------
    static void booleanTraps() {
        // TRAP 1: Comparing String with == instead of equals()
        String status = new String("active");   // new object (not pool)

        if (status == "active") {
            // May print or may not, depending on JVM String pool behaviour
            System.out.println("== comparison: unreliable");
        }
        if (status.equals("active")) {
            System.out.println(".equals() comparison: always correct"); // reliable
        }

        // TRAP 2: Redundant boolean comparison
        boolean isValid = status.length() > 0;
        if (isValid == true) {                   // redundant — works but verbose
            System.out.println("isValid == true");
        }
        if (isValid) {                            // preferred — isValid IS boolean
            System.out.println("isValid (clean)");
        }

        // TRAP 3: Negation
        if (isValid == false) {                  // redundant
            System.out.println("invalid (verbose)");
        }
        if (!isValid) {                           // preferred
            System.out.println("invalid (clean)");
        }

        // TRAP 4: Using & instead of && (non-short-circuit)
        String input = null;
        // WRONG: NullPointerException because & evaluates BOTH sides
        // if (input != null & input.length() > 0) { ... }

        // CORRECT: short-circuit && skips right side if left is false
        if (input != null && input.length() > 0) {
            System.out.println("Non-null and non-empty: " + input);
        }
    }

    // -------------------------------------------------------------------------
    // Ternary as inline if/else expression
    // -------------------------------------------------------------------------
    static void ternaryDemo() {
        int temperature = 38;

        // Single value from a condition — ternary is clean here
        String feeling = temperature > 35 ? "hot" : "comfortable";
        System.out.println("Feeling: " + feeling);

        // Nested ternary — readable only up to 3 levels
        String category = temperature > 40 ? "extreme heat"
                        : temperature > 35 ? "very hot"
                        : temperature > 25 ? "warm"
                        : temperature > 15 ? "cool"
                        : "cold";
        System.out.println("Category: " + category);

        // Ternary in method arguments
        System.out.printf("Temperature is %s than 35°C%n",
            temperature > 35 ? "higher" : "lower or equal");

        // When NOT to use ternary:
        // Don't use it if the body needs side effects or multiple statements
        // if (condition) {
        //     doA(); doB(); log(); return x;
        // }
        // That should stay as if/else, not be forced into a ternary.
    }

    // -------------------------------------------------------------------------
    // Null-safe conditional patterns
    // -------------------------------------------------------------------------
    static String safeGreeting(String name) {
        // Pattern 1: null check first
        if (name == null) {
            return "Hello, stranger!";
        }
        return "Hello, " + name + "!";
    }

    static String safeGreetingOneliner(String name) {
        // Pattern 2: ternary
        return name != null ? "Hello, " + name + "!" : "Hello, stranger!";
    }

    static String modernSafeGreeting(String name) {
        // Pattern 3: Objects.requireNonNullElse (Java 9+)
        String resolved = java.util.Objects.requireNonNullElse(name, "stranger");
        return "Hello, " + resolved + "!";
    }

    public static void main(String[] args) {
        System.out.println("=== Grade Classification ===");
        int[] scores = {95, 85, 75, 65, 55};
        for (int score : scores) {
            System.out.printf("Score %d → Grade %s%n", score, classify(score));
        }

        System.out.println("\n=== Dangling Else Demo ===");
        danglingElseDemo();

        System.out.println("\n=== Boolean Traps ===");
        booleanTraps();

        System.out.println("\n=== Ternary Demo ===");
        ternaryDemo();

        System.out.println("\n=== Null-Safe Patterns ===");
        System.out.println(safeGreeting(null));
        System.out.println(safeGreeting("Parth"));
        System.out.println(modernSafeGreeting(null));
    }
}
