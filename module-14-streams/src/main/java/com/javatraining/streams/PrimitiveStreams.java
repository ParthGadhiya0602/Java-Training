package com.javatraining.streams;

import java.util.*;
import java.util.stream.*;

/**
 * TOPIC: Primitive streams — IntStream, LongStream, DoubleStream
 *
 * Why primitive streams?
 *   Stream<Integer> boxes each int into an Integer object.
 *   IntStream avoids boxing overhead and adds numeric-specific operations:
 *     sum(), average(), min(), max(), summaryStatistics(),
 *     range(start, end), rangeClosed(start, end), asLongStream(), asDoubleStream()
 *
 * Conversions:
 *   Stream<T>  → IntStream     via mapToInt(ToIntFunction<T>)
 *   IntStream  → Stream<T>     via mapToObj(IntFunction<T>)  or boxed()
 *   IntStream  → LongStream    via asLongStream()
 *   IntStream  → DoubleStream  via asDoubleStream()
 */
public class PrimitiveStreams {

    // -------------------------------------------------------------------------
    // 1. IntStream.range / rangeClosed
    // -------------------------------------------------------------------------

    /** Returns the sum of integers from lo to hi inclusive. */
    static long rangeSum(int lo, int hi) {
        return IntStream.rangeClosed(lo, hi).asLongStream().sum();
    }

    /** Returns all even numbers between lo and hi inclusive. */
    static List<Integer> evensInRange(int lo, int hi) {
        return IntStream.rangeClosed(lo, hi)
            .filter(n -> n % 2 == 0)
            .boxed()
            .toList();
    }

    /** Builds a multiplication table row for the given multiplier. */
    static int[] multiplicationRow(int multiplier, int upTo) {
        return IntStream.rangeClosed(1, upTo)
            .map(i -> i * multiplier)
            .toArray();
    }

    // -------------------------------------------------------------------------
    // 2. mapToInt / mapToLong / mapToDouble — Object stream → primitive stream
    // -------------------------------------------------------------------------

    record Employee(String name, String dept, int salary) {}

    static int totalSalary(List<Employee> employees) {
        return employees.stream()
            .mapToInt(Employee::salary)
            .sum();
    }

    static OptionalDouble averageSalary(List<Employee> employees) {
        return employees.stream()
            .mapToInt(Employee::salary)
            .average();
    }

    static IntSummaryStatistics salaryStats(List<Employee> employees) {
        return employees.stream()
            .mapToInt(Employee::salary)
            .summaryStatistics();
    }

    /** Returns salaries above the department average. */
    static List<Employee> aboveAverageSalary(List<Employee> employees) {
        double avg = employees.stream()
            .mapToInt(Employee::salary)
            .average()
            .orElse(0);
        return employees.stream()
            .filter(e -> e.salary() > avg)
            .toList();
    }

    // -------------------------------------------------------------------------
    // 3. LongStream — useful for large aggregations avoiding int overflow
    // -------------------------------------------------------------------------

    /** Computes n! using LongStream.rangeClosed (exact for n ≤ 20). */
    static long factorial(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        if (n == 0) return 1L;
        return LongStream.rangeClosed(1, n).reduce(1L, (a, b) -> a * b);
    }

    /** Sum of squares: 1² + 2² + ... + n² */
    static long sumOfSquares(int n) {
        return LongStream.rangeClosed(1, n).map(i -> i * i).sum();
    }

    // -------------------------------------------------------------------------
    // 4. DoubleStream
    // -------------------------------------------------------------------------

    /** Returns the standard deviation of a list of doubles. */
    static double stdDev(List<Double> values) {
        if (values.size() < 2) return 0.0;
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream()
            .mapToDouble(v -> (v - mean) * (v - mean))
            .average()
            .orElse(0);
        return Math.sqrt(variance);
    }

    /** Normalises values to [0,1] range using min-max scaling. */
    static List<Double> normalise(List<Double> values) {
        DoubleStream ds = values.stream().mapToDouble(Double::doubleValue);
        DoubleSummaryStatistics stats = ds.summaryStatistics();
        double range = stats.getMax() - stats.getMin();
        if (range == 0) return values.stream().map(v -> 0.0).toList();
        return values.stream()
            .mapToDouble(v -> (v - stats.getMin()) / range)
            .boxed()
            .toList();
    }

    // -------------------------------------------------------------------------
    // 5. mapToObj — primitive stream → object stream
    // -------------------------------------------------------------------------

    /** Generates the first n Fibonacci numbers. */
    static List<Long> fibonacci(int n) {
        if (n <= 0) return List.of();
        long[] state = {0L, 1L};  // effectively-final array trick for lambdas
        return Stream.iterate(
                new long[]{0L, 1L},
                s -> new long[]{s[1], s[0] + s[1]})
            .limit(n)
            .map(s -> s[0])
            .toList();
    }

    /** Returns a list of 'A'-based char labels: A, B, C, … for n items. */
    static List<String> columnLabels(int n) {
        return IntStream.range(0, n)
            .mapToObj(i -> String.valueOf((char) ('A' + i)))
            .toList();
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void demo() {
        System.out.println("=== IntStream.range ===");
        System.out.println("sum(1..100):      " + rangeSum(1, 100));
        System.out.println("evens(1..10):     " + evensInRange(1, 10));
        System.out.println("5× table (1-5):   " + Arrays.toString(multiplicationRow(5, 5)));

        List<Employee> employees = List.of(
            new Employee("Alice", "Eng",   120_000),
            new Employee("Bob",   "HR",     80_000),
            new Employee("Carol", "Eng",   135_000),
            new Employee("Dave",  "HR",     75_000),
            new Employee("Eve",   "Eng",   110_000)
        );

        System.out.println("\n=== mapToInt ===");
        System.out.println("total salary:     " + totalSalary(employees));
        System.out.printf ("avg salary:       %.1f%n",
            averageSalary(employees).orElse(0));
        IntSummaryStatistics s = salaryStats(employees);
        System.out.printf ("min=%d max=%d%n", s.getMin(), s.getMax());
        System.out.println("above avg:        " +
            aboveAverageSalary(employees).stream().map(Employee::name).toList());

        System.out.println("\n=== LongStream ===");
        System.out.println("10! = " + factorial(10));
        System.out.println("sum of squares(5): " + sumOfSquares(5)); // 1+4+9+16+25=55

        System.out.println("\n=== DoubleStream ===");
        System.out.printf("stdDev([2,4,4,4,5,5,7,9]): %.4f%n",
            stdDev(List.of(2.0,4.0,4.0,4.0,5.0,5.0,7.0,9.0)));
        System.out.println("normalise([0,5,10]):  " + normalise(List.of(0.0, 5.0, 10.0)));

        System.out.println("\n=== mapToObj ===");
        System.out.println("fibonacci(8): " + fibonacci(8));
        System.out.println("labels(5):    " + columnLabels(5));
    }

    public static void main(String[] args) { demo(); }
}
