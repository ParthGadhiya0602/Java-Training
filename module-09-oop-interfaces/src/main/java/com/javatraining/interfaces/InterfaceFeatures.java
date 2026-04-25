package com.javatraining.interfaces;

import java.time.Instant;
import java.util.*;

/**
 * TOPIC: Interface features - default, static, private methods;
 *        multiple implementation; diamond default resolution.
 *
 * Evolution of interfaces:
 *   Java 7-  : abstract methods + constants only
 *   Java 8   : + default methods, + static methods
 *   Java 9   : + private methods, + private static methods
 *
 * Key rules:
 *   • default method  - provides an implementation; subclass may override
 *   • static method   - belongs to the interface type; NOT inherited
 *   • private method  - only callable from within the interface; not part of API
 *   • Constants       - implicitly public static final
 */
public class InterfaceFeatures {

    // -------------------------------------------------------------------------
    // 1. Validator interface - default + static + private methods
    // -------------------------------------------------------------------------
    interface Validator<T> {

        // Abstract method - every implementor must provide this
        boolean isValid(T value);

        // default: passes through value or throws - avoids null boilerplate
        default T validated(T value) {
            if (!isValid(value))
                throw new IllegalArgumentException("Validation failed for: " + value);
            return value;
        }

        // default: negate - flip the condition
        default Validator<T> negate() {
            return value -> !this.isValid(value);
        }

        // default: compose two validators with AND semantics
        default Validator<T> and(Validator<T> other) {
            return value -> this.isValid(value) && other.isValid(value);
        }

        // default: compose two validators with OR semantics
        default Validator<T> or(Validator<T> other) {
            return value -> this.isValid(value) || other.isValid(value);
        }

        // static factory - build common validators without boilerplate
        static Validator<String> nonBlank() {
            return s -> s != null && !s.isBlank();
        }

        static Validator<String> minLength(int min) {
            return s -> s != null && s.length() >= min;
        }

        static Validator<String> maxLength(int max) {
            return s -> s != null && s.length() <= max;
        }

        static Validator<Integer> positive() {
            return n -> n != null && n > 0;
        }

        static Validator<Integer> range(int lo, int hi) {
            // private helper called from static - combines two validators
            return inRange(lo, hi);
        }

        // private static helper - not visible outside the interface
        private static Validator<Integer> inRange(int lo, int hi) {
            return n -> n != null && n >= lo && n <= hi;
        }
    }

    // -------------------------------------------------------------------------
    // 2. Auditable + Loggable - demonstrates diamond default resolution
    // -------------------------------------------------------------------------
    interface Auditable {
        default String auditInfo() {
            return "[AUDIT] action by " + actorName() + " at " + Instant.now();
        }

        // private helper shared by default methods in this interface
        private String actorName() { return "system"; }

        static Auditable noOp() { return new Auditable() {}; }  // anonymous - Auditable has no SAM
    }

    interface Loggable {
        default void log(String message) {
            System.out.println("[LOG " + logLevel() + "] " + message);
        }

        default String logLevel() { return "INFO"; }
    }

    // Implements both; must resolve the diamond if both have same-signature defaults.
    // Here they don't conflict (different signatures) - just showing multiple impl.
    static class AuditService implements Auditable, Loggable {

        private final String name;

        AuditService(String name) { this.name = name; }

        // Override logLevel from Loggable
        @Override
        public String logLevel() { return "DEBUG"; }

        void process(String input) {
            log("Processing: " + input);  // uses Loggable.log (with overridden logLevel)
            String audit = auditInfo();   // uses Auditable.auditInfo
            log(audit);
        }
    }

    // -------------------------------------------------------------------------
    // 3. Diamond default conflict - forced resolution
    // -------------------------------------------------------------------------
    interface Left {
        default String greet() { return "Hello from Left"; }
    }

    interface Right {
        default String greet() { return "Hello from Right"; }
    }

    // Both Left and Right declare greet() - MUST override to resolve
    static class Middle implements Left, Right {
        @Override
        public String greet() {
            // Choose one explicitly:
            return Left.super.greet() + " & " + Right.super.greet();
        }
    }

    // -------------------------------------------------------------------------
    // 4. Comparable + Printable - multiple interface, real use
    // -------------------------------------------------------------------------
    interface Printable {
        String format();

        default void print() {
            System.out.println(format());
        }
    }

    static final class Product implements Comparable<Product>, Printable {
        private final String name;
        private final double price;
        private final int    stock;

        Product(String name, double price, int stock) {
            this.name  = name;
            this.price = price;
            this.stock = stock;
        }

        String name()    { return name;  }
        double price()   { return price; }
        int    stock()   { return stock; }

        @Override
        public int compareTo(Product other) {
            return Double.compare(this.price, other.price);
        }

        @Override
        public String format() {
            return String.format("%-20s ₹%8.2f  stock=%d", name, price, stock);
        }

        @Override public String toString() { return format(); }
    }

    // -------------------------------------------------------------------------
    // 5. Iterable custom implementation - interface with only one abstract method
    //    but built-in Java integration (for-each loop)
    // -------------------------------------------------------------------------
    static class NumberRange implements Iterable<Integer> {
        private final int start;
        private final int end;
        private final int step;

        NumberRange(int start, int end, int step) {
            if (step <= 0) throw new IllegalArgumentException("step must be > 0");
            this.start = start;
            this.end   = end;
            this.step  = step;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<>() {
                private int current = start;

                @Override public boolean hasNext() { return current <= end; }
                @Override public Integer next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    int val = current;
                    current += step;
                    return val;
                }
            };
        }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void validatorDemo() {
        System.out.println("=== Validator (default / static / private methods) ===");

        Validator<String> nonBlank  = Validator.nonBlank();
        Validator<String> minLen5   = Validator.minLength(5);
        Validator<String> maxLen20  = Validator.maxLength(20);

        // Compose with and()
        Validator<String> nameValidator = nonBlank.and(minLen5).and(maxLen20);

        String[] names = { "Alice", "Jo", "", "AlexanderTheVeryVeryLongNameThatExceedsLimit" };
        for (String n : names) {
            System.out.printf("  %-45s → %s%n", "\"" + n + "\"",
                nameValidator.isValid(n) ? "OK" : "INVALID");
        }

        // validated() throws on invalid
        try {
            nameValidator.validated("Jo");
        } catch (IllegalArgumentException e) {
            System.out.println("  Caught: " + e.getMessage());
        }

        // Integer range
        Validator<Integer> age = Validator.range(0, 120);
        System.out.println("  age 25 valid: " + age.isValid(25));
        System.out.println("  age 200 valid: " + age.isValid(200));

        // negate
        Validator<Integer> notPositive = Validator.<Integer>positive().negate();
        System.out.println("  notPositive(-1): " + notPositive.isValid(-1)); // true
        System.out.println("  notPositive(5):  " + notPositive.isValid(5));  // false
    }

    static void diamondDemo() {
        System.out.println("\n=== Diamond Default Resolution ===");
        Middle m = new Middle();
        System.out.println(m.greet());
    }

    static void multipleInterfaceDemo() {
        System.out.println("\n=== Multiple Interface (Comparable + Printable) ===");
        List<Product> products = new ArrayList<>(List.of(
            new Product("Laptop",     75_000, 10),
            new Product("Mouse",         500, 50),
            new Product("Keyboard",    2_500, 30),
            new Product("Monitor",    25_000,  8)
        ));

        // Comparable
        Collections.sort(products);
        System.out.println("Sorted by price:");
        products.forEach(Product::print);  // uses Printable.print()

        // Min/max
        System.out.println("Cheapest: " + Collections.min(products).name());
        System.out.println("Costliest: " + Collections.max(products).name());
    }

    static void iterableDemo() {
        System.out.println("\n=== Custom Iterable (for-each integration) ===");
        NumberRange evens = new NumberRange(2, 20, 2);
        System.out.print("Even 2-20: ");
        for (int n : evens) System.out.print(n + " ");
        System.out.println();

        NumberRange fives = new NumberRange(0, 50, 5);
        System.out.print("Fives 0-50: ");
        for (int n : fives) System.out.print(n + " ");
        System.out.println();
    }

    public static void main(String[] args) {
        validatorDemo();
        diamondDemo();
        multipleInterfaceDemo();
        iterableDemo();
    }
}
