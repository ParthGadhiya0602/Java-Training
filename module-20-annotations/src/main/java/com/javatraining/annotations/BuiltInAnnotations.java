package com.javatraining.annotations;

import java.util.List;

/**
 * Module 20 - Built-in Java Annotations
 *
 * Java ships several annotation types used by the compiler and JVM:
 *
 * Compiler directives:
 *   @Override          - method overrides a superclass/interface method
 *   @Deprecated        - element should no longer be used
 *   @SuppressWarnings  - suppress named compiler warnings
 *   @FunctionalInterface - interface must have exactly one abstract method
 *   @SafeVarargs       - suppress heap-pollution warning for varargs generics
 *
 * Tool / documentation:
 *   @Documented   - appears in Javadoc (meta-annotation)
 *   @Inherited    - subclasses inherit the annotation (meta-annotation)
 *   @Repeatable   - same annotation can appear multiple times (meta-annotation)
 */
public class BuiltInAnnotations {

    // ── @Override ─────────────────────────────────────────────────────────────

    /**
     * @Override tells the compiler "this method must override something".
     * Without it, a typo in the method name silently creates a new method
     * instead of overriding - a common source of bugs.
     */
    public static class Animal {
        public String sound() { return "..."; }
        public String toString() { return "Animal"; }
    }

    public static class Dog extends Animal {
        @Override
        public String sound() { return "woof"; }  // compiler verifies override

        @Override
        public String toString() { return "Dog"; }
    }

    // ── @Deprecated ──────────────────────────────────────────────────────────

    /**
     * @Deprecated marks code that should no longer be used.
     * Since Java 9, @Deprecated has two optional elements:
     *   since   - version when it was deprecated
     *   forRemoval - whether it will be removed in a future release
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public static String legacyFormat(int value) {
        return "value=" + value;
    }

    public static String modernFormat(int value) {
        return String.format("value=%d", value);
    }

    // ── @SuppressWarnings ────────────────────────────────────────────────────

    /**
     * Common warning names:
     *   "unchecked"      - unchecked cast with generics
     *   "deprecation"    - using deprecated API
     *   "rawtypes"       - raw type usage (no type parameter)
     *   "unused"         - unused variable or method
     *   "serial"         - missing serialVersionUID
     *   "all"            - suppress everything (use sparingly)
     */
    @SuppressWarnings("deprecation")
    public static String callLegacy(int v) {
        return legacyFormat(v);   // calling deprecated method; warning suppressed
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> castToList(Object obj) {
        return (List<T>) obj;     // unchecked cast; warning suppressed
    }

    // ── @FunctionalInterface ──────────────────────────────────────────────────

    /**
     * @FunctionalInterface is both documentation and a compiler check.
     * The compiler rejects the interface if it has more (or zero) abstract methods.
     */
    @FunctionalInterface
    public interface Transformer<T, R> {
        R transform(T input);

        // Default and static methods are allowed - they don't count as abstract.
        default Transformer<T, R> andLog() {
            return input -> {
                R result = this.transform(input);
                return result;
            };
        }

        static <T> Transformer<T, T> identity() {
            return t -> t;
        }
    }

    // ── @SafeVarargs ──────────────────────────────────────────────────────────

    /**
     * Generic varargs creates a "heap pollution" warning because Java creates
     * an Object[] instead of T[]. @SafeVarargs suppresses it when you can
     * guarantee the method doesn't pollute the heap (i.e., doesn't store
     * anything via the varargs array).
     * Requires final, static, or private method (or constructors).
     */
    @SafeVarargs
    public static <T> List<T> listOf(T... items) {
        return List.of(items);    // safe: List.of doesn't store to the array
    }

    // ── Interaction demo ──────────────────────────────────────────────────────

    /** Shows @Override, @Deprecated, and @SuppressWarnings on the same class. */
    public static class LegacyWidget {
        private final String label;

        public LegacyWidget(String label) { this.label = label; }

        @Deprecated(since = "3.0")
        public String getLabel() { return label; }

        /** Modern replacement. */
        public String label() { return label; }

        @Override
        public String toString() { return "LegacyWidget[" + label + "]"; }
    }

    @SuppressWarnings("deprecation")
    public static String useLegacyWidget(String name) {
        LegacyWidget w = new LegacyWidget(name);
        return w.getLabel();
    }
}
