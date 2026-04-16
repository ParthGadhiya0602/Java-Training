package com.javatraining.streams;

import java.util.*;
import java.util.stream.*;

/**
 * TOPIC: Stream basics — creation, intermediate ops, terminal ops
 *
 * A Stream<T> is a lazy sequence of elements that supports declarative
 * bulk operations.  It does NOT store data; it wraps a source (Collection,
 * array, generator, …) and defines a pipeline of operations.
 *
 * Key rules:
 *   • Intermediate ops are LAZY — no work happens until a terminal op is called.
 *   • A stream can be consumed only ONCE.  Reuse requires creating a new stream.
 *   • Streams do not modify their source.
 *   • Order matters: filter early to avoid unnecessary work downstream.
 */
public class StreamBasics {

    // ── shared domain model ──────────────────────────────────────────────────
    record Product(String name, String category, double price, int stock) {}

    // -------------------------------------------------------------------------
    // 1. Creating streams
    // -------------------------------------------------------------------------

    static Stream<String> fromCollection(List<String> list) {
        return list.stream();
    }

    static Stream<Integer> fromVarargs(Integer... values) {
        return Stream.of(values);
    }

    /** Infinite stream of even numbers: 0, 2, 4, 6, … — must be limited downstream. */
    static Stream<Integer> evenNumbers() {
        return Stream.iterate(0, n -> n + 2);
    }

    /** Infinite stream of random doubles in [0,1). */
    static Stream<Double> randomDoubles(long seed) {
        Random rng = new Random(seed);
        return Stream.generate(rng::nextDouble);
    }

    /** Stream from an array. */
    static Stream<String> fromArray(String[] arr) {
        return Arrays.stream(arr);
    }

    // -------------------------------------------------------------------------
    // 2. filter — keep elements that satisfy a predicate
    // -------------------------------------------------------------------------

    static List<Product> inStock(List<Product> products) {
        return products.stream()
            .filter(p -> p.stock() > 0)
            .toList();
    }

    static List<Product> byCategory(List<Product> products, String category) {
        return products.stream()
            .filter(p -> p.category().equalsIgnoreCase(category))
            .toList();
    }

    static List<Product> affordable(List<Product> products, double maxPrice) {
        return products.stream()
            .filter(p -> p.price() <= maxPrice)
            .toList();
    }

    // -------------------------------------------------------------------------
    // 3. map — transform each element
    // -------------------------------------------------------------------------

    static List<String> names(List<Product> products) {
        return products.stream()
            .map(Product::name)
            .toList();
    }

    /** Returns prices with a percentage discount applied. */
    static List<Double> discountedPrices(List<Product> products, double pct) {
        return products.stream()
            .map(p -> p.price() * (1 - pct / 100.0))
            .toList();
    }

    static List<String> upperCaseNames(List<String> list) {
        return list.stream()
            .map(String::toUpperCase)
            .toList();
    }

    // -------------------------------------------------------------------------
    // 4. sorted / distinct / limit / skip
    // -------------------------------------------------------------------------

    static List<Product> sortedByPrice(List<Product> products) {
        return products.stream()
            .sorted(Comparator.comparingDouble(Product::price))
            .toList();
    }

    static List<Product> top3ByPrice(List<Product> products) {
        return products.stream()
            .sorted(Comparator.comparingDouble(Product::price).reversed())
            .limit(3)
            .toList();
    }

    /** Returns distinct words (case-insensitive) in original case. */
    static List<String> distinctWords(List<String> words) {
        Set<String> seen = new LinkedHashSet<>();
        return words.stream()
            .filter(w -> seen.add(w.toLowerCase()))
            .toList();
    }

    /** Pagination: returns page of size {@code size} starting at page 0. */
    static <T> List<T> page(List<T> items, int pageIndex, int pageSize) {
        return items.stream()
            .skip((long) pageIndex * pageSize)
            .limit(pageSize)
            .toList();
    }

    // -------------------------------------------------------------------------
    // 5. Terminal ops: count, reduce, findFirst, anyMatch/allMatch/noneMatch
    // -------------------------------------------------------------------------

    static long countInStock(List<Product> products) {
        return products.stream()
            .filter(p -> p.stock() > 0)
            .count();
    }

    /** Sum prices using reduce. */
    static double totalPrice(List<Product> products) {
        return products.stream()
            .mapToDouble(Product::price)
            .reduce(0.0, Double::sum);
    }

    static Optional<Product> cheapest(List<Product> products) {
        return products.stream()
            .min(Comparator.comparingDouble(Product::price));
    }

    static Optional<Product> mostExpensive(List<Product> products) {
        return products.stream()
            .max(Comparator.comparingDouble(Product::price));
    }

    static boolean anyExpensive(List<Product> products, double threshold) {
        return products.stream().anyMatch(p -> p.price() > threshold);
    }

    static boolean allInStock(List<Product> products) {
        return products.stream().allMatch(p -> p.stock() > 0);
    }

    static boolean noneOutOfStock(List<Product> products) {
        return products.stream().noneMatch(p -> p.stock() == 0);
    }

    static Optional<Product> firstByCategory(List<Product> products, String cat) {
        return products.stream()
            .filter(p -> p.category().equalsIgnoreCase(cat))
            .findFirst();
    }

    // -------------------------------------------------------------------------
    // 6. String joining via stream
    // -------------------------------------------------------------------------

    static String joinNames(List<Product> products, String delimiter) {
        return products.stream()
            .map(Product::name)
            .collect(Collectors.joining(delimiter));
    }

    static String csvLine(List<String> values) {
        return values.stream()
            .collect(Collectors.joining(",", "\"", "\""));
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static List<Product> sampleProducts() {
        return List.of(
            new Product("Laptop",   "Electronics", 75000.0, 10),
            new Product("Phone",    "Electronics", 25000.0,  5),
            new Product("Shirt",    "Clothing",      800.0, 20),
            new Product("Jeans",    "Clothing",     1500.0,  0),
            new Product("Rice",     "Food",          120.0, 50),
            new Product("Headphones","Electronics", 3500.0, 15)
        );
    }

    static void demo() {
        List<Product> products = sampleProducts();

        System.out.println("=== filter ===");
        System.out.println("in stock:        " + names(inStock(products)));
        System.out.println("electronics:     " + names(byCategory(products, "electronics")));
        System.out.println("≤ 2000:          " + names(affordable(products, 2000)));

        System.out.println("\n=== map / sort ===");
        System.out.println("all names:       " + names(products));
        System.out.println("sorted by price: " + names(sortedByPrice(products)));
        System.out.println("top 3 by price:  " + names(top3ByPrice(products)));

        System.out.println("\n=== terminal ===");
        System.out.println("in-stock count:  " + countInStock(products));
        System.out.println("total price:     " + totalPrice(products));
        System.out.println("cheapest:        " + cheapest(products).map(Product::name).orElse("none"));
        System.out.println("any > 50000:     " + anyExpensive(products, 50000));
        System.out.println("all in stock:    " + allInStock(products));

        System.out.println("\n=== joining ===");
        System.out.println("names joined:    " + joinNames(products, " | "));
        System.out.println("csv:             " + csvLine(List.of("Alice", "30", "Eng")));
    }

    public static void main(String[] args) { demo(); }
}
