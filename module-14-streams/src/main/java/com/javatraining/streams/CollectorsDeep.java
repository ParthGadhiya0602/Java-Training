package com.javatraining.streams;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * TOPIC: Collectors in depth
 *
 * Collectors.groupingBy    - partition into Map<K, List<T>>
 * Collectors.partitioningBy - special case: Map<Boolean, List<T>>
 * Collectors.toMap         - Map<K, V> with explicit key/value extractors
 * Collectors.joining       - concatenate strings
 * Collectors.counting      - count elements in a group
 * Collectors.summarizingInt/Long/Double - min/max/sum/count/average in one pass
 * Collectors.teeing        - apply two collectors to the same stream, merge results
 * Downstream collectors    - groupingBy(fn, downstream) chains collectors
 */
public class CollectorsDeep {

    record Order(String id, String customer, String category,
                 double amount, boolean paid) {}

    // -------------------------------------------------------------------------
    // 1. groupingBy - the most important collector
    // -------------------------------------------------------------------------

    /** Groups orders by category. */
    static Map<String, List<Order>> byCategory(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.groupingBy(Order::category));
    }

    /** Groups orders by category, counting per group (downstream collector). */
    static Map<String, Long> countByCategory(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.groupingBy(Order::category, Collectors.counting()));
    }

    /** Groups by category, sums amount per group. */
    static Map<String, Double> revenueByCategory(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.groupingBy(
                Order::category,
                Collectors.summingDouble(Order::amount)));
    }

    /** Groups by category, collects just the IDs per group. */
    static Map<String, List<String>> idsByCategory(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.groupingBy(
                Order::category,
                Collectors.mapping(Order::id, Collectors.toList())));
    }

    /** Groups by category, keeps only the most expensive order per group. */
    static Map<String, Optional<Order>> maxAmountByCategory(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.groupingBy(
                Order::category,
                Collectors.maxBy(Comparator.comparingDouble(Order::amount))));
    }

    // -------------------------------------------------------------------------
    // 2. partitioningBy - two groups: true and false
    // -------------------------------------------------------------------------

    /** Splits orders into paid and unpaid. */
    static Map<Boolean, List<Order>> paidVsUnpaid(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.partitioningBy(Order::paid));
    }

    /** Splits into high-value (≥ threshold) and low-value. */
    static Map<Boolean, Long> highLowValueCount(List<Order> orders, double threshold) {
        return orders.stream()
            .collect(Collectors.partitioningBy(
                o -> o.amount() >= threshold,
                Collectors.counting()));
    }

    // -------------------------------------------------------------------------
    // 3. toMap - build a lookup map
    // -------------------------------------------------------------------------

    /** Builds an id→order lookup map. Throws on duplicate keys. */
    static Map<String, Order> byId(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.toMap(Order::id, Function.identity()));
    }

    /** Builds a customer→totalAmount map, merging duplicate customers by summing. */
    static Map<String, Double> spendByCustomer(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.toMap(
                Order::customer,
                Order::amount,
                Double::sum));
    }

    // -------------------------------------------------------------------------
    // 4. joining
    // -------------------------------------------------------------------------

    static String orderSummary(List<Order> orders) {
        return orders.stream()
            .map(o -> o.id() + "=" + String.format("%.0f", o.amount()))
            .collect(Collectors.joining(", ", "[", "]"));
    }

    // -------------------------------------------------------------------------
    // 5. summarizingInt/Double - min/max/sum/count/average in one pass
    // -------------------------------------------------------------------------

    static DoubleSummaryStatistics amountStats(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.summarizingDouble(Order::amount));
    }

    // -------------------------------------------------------------------------
    // 6. teeing - apply two collectors to the SAME stream, merge results (Java 12+)
    //    Classic use: compute sum AND count to get average in one pass;
    //    or collect two separate lists from one stream traversal.
    // -------------------------------------------------------------------------

    record SumCount(double sum, long count) {
        double average() { return count == 0 ? 0 : sum / count; }
    }

    /** Computes sum and count in a single stream pass using teeing. */
    static SumCount sumAndCount(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.teeing(
                Collectors.summingDouble(Order::amount),
                Collectors.counting(),
                SumCount::new));
    }

    /** Splits into two lists (paid / unpaid) in a single stream pass. */
    static Map.Entry<List<Order>, List<Order>> splitPaidUnpaid(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.teeing(
                Collectors.filtering(Order::paid,    Collectors.toList()),
                Collectors.filtering(o -> !o.paid(), Collectors.toList()),
                Map::entry));
    }

    // -------------------------------------------------------------------------
    // 7. Multi-level grouping
    // -------------------------------------------------------------------------

    /** Groups by category, then by paid status. */
    static Map<String, Map<Boolean, List<Order>>> byCategoryThenPaid(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.groupingBy(
                Order::category,
                Collectors.partitioningBy(Order::paid)));
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static List<Order> sampleOrders() {
        return List.of(
            new Order("O1", "Alice", "Electronics", 25000, true),
            new Order("O2", "Bob",   "Clothing",      800, false),
            new Order("O3", "Alice", "Food",          200, true),
            new Order("O4", "Carol", "Electronics", 75000, true),
            new Order("O5", "Bob",   "Electronics",  3500, false),
            new Order("O6", "Carol", "Clothing",     1500, true),
            new Order("O7", "Alice", "Food",          150, false)
        );
    }

    static void demo() {
        List<Order> orders = sampleOrders();

        System.out.println("=== groupingBy ===");
        System.out.println("count/category: " + new TreeMap<>(countByCategory(orders)));
        System.out.println("revenue/cat:    " + new TreeMap<>(revenueByCategory(orders)));

        System.out.println("\n=== partitioningBy ===");
        Map<Boolean, List<Order>> split = paidVsUnpaid(orders);
        System.out.println("paid:    " + split.get(true).stream().map(Order::id).toList());
        System.out.println("unpaid:  " + split.get(false).stream().map(Order::id).toList());

        System.out.println("\n=== toMap ===");
        System.out.println("spend/customer: " + new TreeMap<>(spendByCustomer(orders)));

        System.out.println("\n=== joining ===");
        System.out.println(orderSummary(orders));

        System.out.println("\n=== summarizing ===");
        DoubleSummaryStatistics stats = amountStats(orders);
        System.out.printf("count=%d sum=%.0f min=%.0f max=%.0f avg=%.1f%n",
            stats.getCount(), stats.getSum(), stats.getMin(), stats.getMax(), stats.getAverage());

        System.out.println("\n=== teeing ===");
        SumCount sc = sumAndCount(orders);
        System.out.printf("sum=%.0f count=%d avg=%.1f%n", sc.sum(), sc.count(), sc.average());
    }

    public static void main(String[] args) { demo(); }
}
