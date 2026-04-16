package com.javatraining.nested;

import java.util.*;
import java.util.function.*;

/**
 * TOPIC: Local classes and anonymous classes
 *
 * LOCAL CLASS — defined inside a method body.
 *   • Has a name; can be instantiated multiple times inside that method.
 *   • Captures effectively-final local variables from the enclosing scope.
 *   • Good for: a reusable helper that needs both a name and local context.
 *
 * ANONYMOUS CLASS — instantiated inline without a declared name.
 *   • Exactly one use site; the class body follows the 'new' expression.
 *   • Captures effectively-final local variables.
 *   • Good for: single-use implementations, especially pre-Java-8 listeners.
 *
 * LAMBDA — anonymous function (no class boilerplate).
 *   • Only works for @FunctionalInterface (exactly one abstract method).
 *   • Cannot store additional state in fields; cannot reference 'this' as itself.
 *   • Preferred over anonymous class whenever the interface is functional.
 *
 * Decision rule
 * ─────────────
 *  Multiple abstract methods?          → anonymous class required
 *  Need internal fields / state?       → anonymous class (or named private class)
 *  Need super / this reference?        → anonymous class
 *  Single abstract method, stateless?  → use a lambda
 */
public class LocalAndAnonymous {

    // -------------------------------------------------------------------------
    // 1. Local class — reusable within one method, captures local variables
    // -------------------------------------------------------------------------

    /**
     * Returns a list of lines from {@code text} that satisfy the given
     * predicate, with an optional prefix on each retained line.
     * The local class LineFilter is instantiated once but used twice to
     * illustrate that local classes can be instantiated multiple times.
     */
    static List<String> filteredLines(String text, Predicate<String> keep, String prefix) {
        String[] lines = text.split("\n");

        // Local class — only visible inside this method body
        class LineFilter {
            private final String prefix;

            LineFilter(String p) { this.prefix = p; }

            String apply(String line) {
                return prefix.isEmpty() ? line : prefix + line;
            }
        }

        LineFilter noPrefix     = new LineFilter("");
        LineFilter labelledFilter = new LineFilter(prefix);

        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (keep.test(line)) {
                // use labelledFilter if prefix given, else noPrefix
                result.add(prefix.isEmpty()
                    ? noPrefix.apply(line)
                    : labelledFilter.apply(line));
            }
        }
        return result;
    }

    /**
     * Builds a simple string formatter using a local class.
     * Demonstrates capturing an effectively-final local (width).
     */
    static List<String> formatTable(List<String[]> rows, int colWidth) {
        // colWidth is effectively final — captured by the local class
        class RowFormatter {
            String format(String[] cells) {
                StringBuilder sb = new StringBuilder("|");
                for (String cell : cells) {
                    String padded = String.format("%-" + colWidth + "s", cell);
                    sb.append(" ").append(padded).append(" |");
                }
                return sb.toString();
            }

            String separator(int cols) {
                return "+" + ("-".repeat(colWidth + 2) + "+").repeat(cols);
            }
        }

        RowFormatter fmt = new RowFormatter();
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            if (i == 0) lines.add(fmt.separator(rows.get(0).length));
            lines.add(fmt.format(rows.get(i)));
            lines.add(fmt.separator(rows.get(i).length));
        }
        return lines;
    }

    // -------------------------------------------------------------------------
    // 2. Anonymous class — one-shot implementation inline
    // -------------------------------------------------------------------------

    /**
     * Returns a Comparator that sorts strings by length, then alphabetically.
     * This is the OLD (pre-Java-8) style; kept for illustration.
     * Note: Comparator IS a functional interface → lambda preferred today.
     */
    static Comparator<String> lengthThenAlpha_anonymous() {
        return new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                int cmp = Integer.compare(a.length(), b.length());
                return cmp != 0 ? cmp : a.compareTo(b);
            }
        };
    }

    /** Modern equivalent — lambda is cleaner when there is one abstract method. */
    static Comparator<String> lengthThenAlpha_lambda() {
        return Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder());
    }

    // -------------------------------------------------------------------------
    // 3. When anonymous class is REQUIRED (multi-method interface)
    //    Iterable has only one abstract method (iterator()), so a lambda
    //    could work in theory, but we show an anonymous class here for a
    //    two-abstract-method scenario using a custom interface.
    // -------------------------------------------------------------------------

    /** A two-method interface — NOT a functional interface; lambda cannot implement it. */
    interface Describable {
        String describe();
        String shortLabel();
    }

    /**
     * Returns a Describable for the given product name + price.
     * Lambda is impossible here (two abstract methods) → anonymous class required.
     */
    static Describable productDescribable(String name, double price) {
        // price and name are effectively final
        return new Describable() {
            @Override
            public String describe() {
                return String.format("Product: %s at ₹%.2f", name, price);
            }

            @Override
            public String shortLabel() {
                return name.substring(0, Math.min(name.length(), 8)).toUpperCase();
            }
        };
    }

    // -------------------------------------------------------------------------
    // 4. Anonymous class with internal state — another case where lambda fails
    // -------------------------------------------------------------------------

    /**
     * Returns a Runnable that counts down from n and prints each tick.
     * It needs an internal counter field → anonymous class with a field.
     * Lambda has no way to declare fields.
     */
    static Runnable countdownRunnable(int from) {
        return new Runnable() {
            private int remaining = from;   // internal field — impossible with lambda

            @Override
            public void run() {
                if (remaining > 0) {
                    System.out.println("  tick " + remaining--);
                } else {
                    System.out.println("  done");
                }
            }
        };
    }

    // -------------------------------------------------------------------------
    // 5. Side-by-side: anonymous class vs lambda for a functional interface
    // -------------------------------------------------------------------------

    @FunctionalInterface
    interface StringTransform {
        String apply(String s);
    }

    /** Anonymous class version — works but verbose. */
    static StringTransform shout_anonymous() {
        return new StringTransform() {
            @Override
            public String apply(String s) {
                return s.toUpperCase() + "!!!";
            }
        };
    }

    /** Lambda version — preferred; same behaviour, far less ceremony. */
    static StringTransform shout_lambda() {
        return s -> s.toUpperCase() + "!!!";
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void localClassDemo() {
        System.out.println("=== Local class — filteredLines ===");
        String text = "apple\nbanana\navocado\nblueberry\ncherry";
        List<String> aWords = filteredLines(text, s -> s.startsWith("a"), "> ");
        aWords.forEach(System.out::println);

        System.out.println("\n=== Local class — formatTable ===");
        List<String[]> rows = List.of(
            new String[]{"Name",    "City",      "Score"},
            new String[]{"Alice",   "Bengaluru", "95"},
            new String[]{"Bob",     "Mumbai",    "87"}
        );
        formatTable(rows, 12).forEach(System.out::println);
    }

    static void anonymousClassDemo() {
        System.out.println("\n=== Anonymous class vs lambda (Comparator) ===");
        List<String> words = new ArrayList<>(List.of("fig", "apple", "kiwi", "banana", "date"));

        words.sort(lengthThenAlpha_anonymous());
        System.out.println("anonymous: " + words);

        words.sort(lengthThenAlpha_lambda());
        System.out.println("lambda:    " + words);

        System.out.println("\n=== Anonymous class — multi-method interface ===");
        Describable d = productDescribable("Laptop Pro", 89999.99);
        System.out.println(d.describe());
        System.out.println(d.shortLabel());

        System.out.println("\n=== Anonymous class with field (countdown) ===");
        Runnable cd = countdownRunnable(3);
        cd.run(); cd.run(); cd.run(); cd.run();   // 3,2,1,done

        System.out.println("\n=== StringTransform: anonymous vs lambda ===");
        StringTransform anon   = shout_anonymous();
        StringTransform lambda = shout_lambda();
        System.out.println("anonymous: " + anon.apply("hello"));
        System.out.println("lambda:    " + lambda.apply("hello"));
    }

    public static void main(String[] args) {
        localClassDemo();
        anonymousClassDemo();
    }
}
