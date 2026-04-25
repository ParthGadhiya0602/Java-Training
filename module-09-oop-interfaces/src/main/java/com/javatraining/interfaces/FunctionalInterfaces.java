package com.javatraining.interfaces;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * TOPIC: Functional interfaces - @FunctionalInterface, all built-in types,
 *        lambda syntax, method references, and composition operators.
 *
 * A functional interface has exactly ONE abstract method (SAM).
 * @FunctionalInterface makes the compiler enforce this.
 * Lambdas and method references are syntactic sugar for anonymous implementations.
 *
 * Lambda syntax forms:
 *   ()         -> expr              // no params
 *   x          -> expr              // single param, no parens needed
 *   (x, y)     -> expr              // multiple params
 *   (x, y)     -> { stmts; return; }// block body
 *   (int x)    -> x * 2             // explicit type (rarely needed)
 *
 * Method reference forms:
 *   ClassName::staticMethod         // static
 *   instance::instanceMethod        // bound instance
 *   ClassName::instanceMethod       // unbound (first param becomes receiver)
 *   ClassName::new                  // constructor reference
 */
public class FunctionalInterfaces {

    // -------------------------------------------------------------------------
    // 1. Custom functional interfaces
    // -------------------------------------------------------------------------

    @FunctionalInterface
    interface Transformer<T> {
        T transform(T input);

        // default methods are allowed - still functional (one abstract)
        default Transformer<T> andThen(Transformer<T> after) {
            return input -> after.transform(this.transform(input));
        }
    }

    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Exception;

        // Wrap to a regular Supplier - swallow checked exceptions (with care)
        static <T> Supplier<T> wrap(ThrowingSupplier<T> supplier) {
            return () -> {
                try {
                    return supplier.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }

    // -------------------------------------------------------------------------
    // 2. Built-in functional types - all forms
    // -------------------------------------------------------------------------
    static void supplierDemo() {
        System.out.println("=== Supplier<T> - () → T ===");

        // Lambda
        Supplier<String> greeting = () -> "Hello, World!";
        // Method reference (static)
        Supplier<Double> random   = Math::random;
        // Constructor reference
        Supplier<ArrayList<String>> listFactory = ArrayList::new;

        System.out.println(greeting.get());
        System.out.printf("Random: %.4f%n", random.get());
        System.out.println("Empty list: " + listFactory.get());

        // Lazy evaluation - supplier defers computation until needed
        Supplier<String> expensive = () -> {
            // imagine heavy computation here
            return "computed";
        };
        String result = expensive.get();
        System.out.println("Lazy result: " + result);
    }

    static void consumerDemo() {
        System.out.println("\n=== Consumer<T> - T → void ===");

        Consumer<String> printer  = System.out::println;
        Consumer<String> upper    = s -> System.out.println(s.toUpperCase());

        // andThen - chain consumers (both run)
        Consumer<String> printAndUpper = printer.andThen(upper);

        List<String> names = List.of("alice", "bob", "carol");
        System.out.println("andThen chain:");
        names.forEach(printAndUpper);

        // BiConsumer
        BiConsumer<String, Integer> nameAge = (name, age) ->
            System.out.println(name + " is " + age);
        nameAge.accept("Dave", 30);
    }

    static void functionDemo() {
        System.out.println("\n=== Function<T,R> - T → R ===");

        Function<String, Integer> length  = String::length;
        Function<Integer, String> asHex   = Integer::toHexString;
        Function<String, Boolean> isLong  = s -> s.length() > 5;

        // compose: g.compose(f) = g(f(x)) - right to left
        Function<String, String> lengthToHex = asHex.compose(length);
        // andThen: f.andThen(g) = g(f(x)) - left to right
        Function<String, String> lengthToHex2 = length.andThen(asHex);

        System.out.println("length(\"Hello\"):       " + length.apply("Hello"));
        System.out.println("lengthToHex(\"Hello\"):  " + lengthToHex.apply("Hello"));  // 5 → "5"
        System.out.println("Same via andThen:       " + lengthToHex2.apply("Hello"));

        // UnaryOperator - Function<T,T>
        UnaryOperator<String> trim    = String::trim;
        UnaryOperator<String> toLower = String::toLowerCase;
        // andThen on UnaryOperator returns Function<T,T>; cast back to UnaryOperator via lambda
        UnaryOperator<String> normalise = s -> toLower.apply(trim.apply(s));

        System.out.println("normalise(\"  HELLO  \"): \"" + normalise.apply("  HELLO  ") + "\"");

        // BiFunction
        BiFunction<String, String, String> concat =
            (a, b) -> a + " " + b;
        System.out.println("concat: " + concat.apply("Hello", "World"));

        // BinaryOperator - BiFunction<T,T,T>
        BinaryOperator<Integer> sum = Integer::sum;
        System.out.println("sum(3,4): " + sum.apply(3, 4));
    }

    static void predicateDemo() {
        System.out.println("\n=== Predicate<T> - T → boolean ===");

        Predicate<String> nonEmpty  = s -> !s.isEmpty();
        Predicate<String> hasAt     = s -> s.contains("@");
        Predicate<String> longEnough = s -> s.length() >= 5;

        // Composition
        Predicate<String> emailLike = nonEmpty.and(hasAt).and(longEnough);
        Predicate<String> notEmail  = emailLike.negate();

        String[] candidates = { "alice@example.com", "bob", "", "x@y" };
        for (String s : candidates) {
            System.out.printf("  %-22s emailLike=%-5s notEmail=%s%n",
                "\"" + s + "\"",
                emailLike.test(s),
                notEmail.test(s));
        }

        // or
        Predicate<Integer> even = n -> n % 2 == 0;
        Predicate<Integer> div3 = n -> n % 3 == 0;
        Predicate<Integer> evenOrDiv3 = even.or(div3);

        List<Integer> nums = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        List<Integer> result = nums.stream().filter(evenOrDiv3).toList();
        System.out.println("even or div by 3: " + result);

        // Predicate.not (Java 11+)
        List<String> nonBlanks = List.of("hello", "  ", "", "world")
            .stream()
            .filter(Predicate.not(String::isBlank))
            .toList();
        System.out.println("non-blanks: " + nonBlanks);
    }

    static void customFunctionalDemo() {
        System.out.println("\n=== Custom Functional Interfaces ===");

        Transformer<String> trim   = String::trim;
        Transformer<String> upper  = String::toUpperCase;
        Transformer<String> exclaim = s -> s + "!";

        // Chain via andThen
        Transformer<String> pipeline = trim.andThen(upper).andThen(exclaim);
        System.out.println(pipeline.transform("  hello world  "));  // "HELLO WORLD!"

        // TriFunction
        TriFunction<String, Integer, Double, String> summary =
            (name, age, score) ->
                String.format("%s (age %d) scored %.1f", name, age, score);
        System.out.println(summary.apply("Alice", 30, 98.5));

        // ThrowingSupplier wrapping
        Supplier<Integer> safe = ThrowingSupplier.wrap(() -> Integer.parseInt("42"));
        System.out.println("ThrowingSupplier wrapped: " + safe.get());
    }

    static void compositionDemo() {
        System.out.println("\n=== Composition - building pipelines ===");

        // Pipeline: raw string → trimmed → validated → formatted
        Function<String, String>  trim        = String::trim;
        Function<String, String>  capitalise  = s ->
            s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
        Function<String, Integer> score       = s -> s.length() * 10;

        Function<String, String> cleanName    = trim.andThen(capitalise);
        Function<String, Integer> nameScore   = cleanName.andThen(score);

        List<String> rawNames = List.of("  alice  ", "BOB", " carol ");
        System.out.println("Cleaned names:");
        rawNames.stream()
            .map(cleanName)
            .forEach(n -> System.out.println("  " + n + " (score=" + nameScore.apply(n) + ")"));

        // Collecting with functional transforms
        Map<String, Integer> scores = rawNames.stream()
            .collect(Collectors.toMap(
                cleanName,
                nameScore
            ));
        System.out.println("Score map: " + scores);
    }

    public static void main(String[] args) {
        supplierDemo();
        consumerDemo();
        functionDemo();
        predicateDemo();
        customFunctionalDemo();
        compositionDemo();
    }
}
