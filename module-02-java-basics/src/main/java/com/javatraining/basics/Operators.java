package com.javatraining.basics;

/**
 * TOPIC: All Java operators — arithmetic, relational, logical, bitwise,
 * shift, assignment, ternary, and instanceof (with pattern matching).
 *
 * KEY INSIGHT: Operator precedence causes more bugs than most people realize.
 * When in doubt, use parentheses. Also: integer division truncates, not rounds.
 */
public class Operators {

    // -------------------------------------------------------------------------
    // ARITHMETIC OPERATORS
    // -------------------------------------------------------------------------
    static void arithmetic() {
        int a = 10, b = 3;

        System.out.println(a + b);   // 13  — addition
        System.out.println(a - b);   // 7   — subtraction
        System.out.println(a * b);   // 30  — multiplication
        System.out.println(a / b);   // 3   — integer division: TRUNCATES toward zero
        System.out.println(a % b);   // 1   — remainder (modulo)

        // Integer division gotcha: always truncates, never rounds
        System.out.println(7 / 2);   // 3, not 3.5
        System.out.println(-7 / 2);  // -3, not -4 (truncates toward zero)

        // To get floating-point division, at least one operand must be a double/float
        System.out.println((double) a / b);  // 3.3333...
        System.out.println(a / (double) b);  // 3.3333...
        System.out.println(a / 3.0);         // 3.3333...

        // Floating point is NOT precise — never use == for float/double
        double result = 0.1 + 0.2;
        System.out.println(result);            // 0.30000000000000004 (!)
        System.out.println(result == 0.3);     // false (!)
        // Correct comparison: check if difference is within a tolerance
        double epsilon = 1e-9;
        System.out.println(Math.abs(result - 0.3) < epsilon); // true

        // Pre/post increment: the difference matters in expressions
        int x = 5;
        int preIncrement  = ++x; // x becomes 6 FIRST, then assigns 6
        int postIncrement = x++; // assigns 6 FIRST, then x becomes 7
        System.out.println("x=" + x + " pre=" + preIncrement + " post=" + postIncrement);
        // x=7, pre=6, post=6
    }

    // -------------------------------------------------------------------------
    // RELATIONAL & EQUALITY OPERATORS
    // -------------------------------------------------------------------------
    static void relational() {
        int a = 5, b = 10;
        System.out.println(a < b);   // true
        System.out.println(a > b);   // false
        System.out.println(a <= 5);  // true
        System.out.println(a >= 5);  // true
        System.out.println(a == b);  // false
        System.out.println(a != b);  // true
    }

    // -------------------------------------------------------------------------
    // LOGICAL OPERATORS — short-circuit evaluation is critical to understand
    // -------------------------------------------------------------------------
    static void logical() {
        // && (AND) and || (OR) short-circuit: the right side is NOT evaluated
        // if the result is already determined from the left side.
        int x = 0;

        // Short-circuit AND: if left is false, right is skipped
        boolean r1 = (x != 0) && (10 / x > 1); // NO division by zero — skipped!
        System.out.println(r1); // false

        // Short-circuit OR: if left is true, right is skipped
        boolean r2 = (x == 0) || (10 / x > 1); // right skipped
        System.out.println(r2); // true

        // Non-short-circuit (bitwise): & and | always evaluate both sides
        // Use & and | only for bitwise operations on integers, not boolean logic
        // boolean bad = (x != 0) & (10 / x > 1); // throws ArithmeticException!

        // Logical NOT
        System.out.println(!true);  // false
        System.out.println(!false); // true

        // XOR (exclusive or): true if exactly one operand is true
        System.out.println(true  ^ true);  // false
        System.out.println(true  ^ false); // true
        System.out.println(false ^ false); // false
    }

    // -------------------------------------------------------------------------
    // BITWISE & SHIFT OPERATORS — used in flags, performance-critical code, crypto
    // -------------------------------------------------------------------------
    static void bitwise() {
        int a = 0b1010;  // 10 in binary: bits 1 and 3 set
        int b = 0b1100;  // 12 in binary: bits 2 and 3 set

        System.out.println(Integer.toBinaryString(a & b));  // 1000 = 8  (AND)
        System.out.println(Integer.toBinaryString(a | b));  // 1110 = 14 (OR)
        System.out.println(Integer.toBinaryString(a ^ b));  // 0110 = 6  (XOR)
        System.out.println(Integer.toBinaryString(~a));     // ...11110101 = -11 (NOT)

        // Shift operators: much faster than multiplication/division by powers of 2
        int n = 1;
        System.out.println(n << 3);  // 8  — left shift = multiply by 2^3
        System.out.println(8 >> 2);  // 2  — right shift (signed) = divide by 2^2
        System.out.println(-1 >> 1); // -1 — preserves sign bit
        System.out.println(-1 >>> 1);// 2147483647 — unsigned right shift (zero fill)

        // Real-world use: checking and setting flags using a bitmask
        int permissions = 0b000; // no permissions
        int READ    = 0b001;     // bit 0
        int WRITE   = 0b010;     // bit 1
        int EXECUTE = 0b100;     // bit 2

        permissions |= READ | WRITE;   // grant read + write
        System.out.println(Integer.toBinaryString(permissions)); // 011

        boolean canRead    = (permissions & READ)    != 0; // true
        boolean canExecute = (permissions & EXECUTE) != 0; // false

        permissions &= ~WRITE; // revoke write (clear bit 1)
        System.out.println(Integer.toBinaryString(permissions)); // 001
    }

    // -------------------------------------------------------------------------
    // ASSIGNMENT OPERATORS — compound assignments
    // -------------------------------------------------------------------------
    static void assignment() {
        int x = 10;
        x += 5;  // x = x + 5  → 15
        x -= 3;  // x = x - 3  → 12
        x *= 2;  // x = x * 2  → 24
        x /= 4;  // x = x / 4  → 6
        x %= 4;  // x = x % 4  → 2
        x <<= 1; // x = x << 1 → 4
        x >>= 1; // x = x >> 1 → 2
        x &= 3;  // x = x & 3  → 2
        x |= 1;  // x = x | 1  → 3
        System.out.println(x); // 3
    }

    // -------------------------------------------------------------------------
    // TERNARY OPERATOR — concise if/else for expressions
    // -------------------------------------------------------------------------
    static void ternary() {
        int score = 75;
        String grade = score >= 90 ? "A"
                     : score >= 80 ? "B"
                     : score >= 70 ? "C"
                     : "F";
        System.out.println(grade); // C

        // Ternary is an expression (has a value), not a statement.
        // This matters when assigning or passing as an argument.
        int abs = score >= 0 ? score : -score; // inline absolute value
    }

    // -------------------------------------------------------------------------
    // instanceof — type checking, and pattern matching (Java 16+)
    // -------------------------------------------------------------------------
    static void instanceofDemo() {
        Object obj = "Hello, Java!";

        // Old style — check then cast (two operations, boilerplate)
        if (obj instanceof String) {
            String s = (String) obj; // redundant cast
            System.out.println(s.length());
        }

        // Pattern matching (Java 16+) — check, bind, and cast in one expression
        if (obj instanceof String s) {
            // 's' is in scope and already typed as String
            System.out.println(s.toUpperCase());
        }

        // Pattern matching with guard condition (Java 21)
        Object value = 42;
        if (value instanceof Integer i && i > 10) {
            System.out.println("Integer greater than 10: " + i);
        }
    }

    // -------------------------------------------------------------------------
    // OPERATOR PRECEDENCE — where bugs hide
    // -------------------------------------------------------------------------
    static void precedence() {
        // Higher precedence binds tighter (like * before +)
        int result = 2 + 3 * 4;       // 14, not 20 (multiplication first)
        int result2 = (2 + 3) * 4;    // 20

        // Conditional with assignment — a classic trap
        boolean a = true, b = false;
        boolean trap = a || b && !a;   // reads as: a || (b && (!a)) = true
        boolean clear = (a || b) && !a; // false

        System.out.println("trap=" + trap + " clear=" + clear);

        // Post-increment in expressions
        int x = 3;
        int y = x++ + ++x; // x++ returns 3 (then x=4), ++x returns 5 (x=5)
        // y = 3 + 5 = 8, x = 5
        System.out.println("y=" + y + " x=" + x);
        // Avoid this. Write it on separate lines instead.
    }

    public static void main(String[] args) {
        System.out.println("=== Arithmetic ===");
        arithmetic();
        System.out.println("\n=== Relational ===");
        relational();
        System.out.println("\n=== Logical ===");
        logical();
        System.out.println("\n=== Bitwise ===");
        bitwise();
        System.out.println("\n=== Assignment ===");
        assignment();
        System.out.println("\n=== Ternary ===");
        ternary();
        System.out.println("\n=== instanceof ===");
        instanceofDemo();
        System.out.println("\n=== Precedence ===");
        precedence();
    }
}
