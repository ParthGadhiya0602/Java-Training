package com.javatraining.testing;

/**
 * Simple arithmetic utility — used as the subject-under-test for JUnit 5 feature demos.
 * Provides enough variation (primitives, exception paths, iterative algorithms)
 * to exercise a wide range of assertion and parameterization techniques.
 */
public class Calculator {

    public int add(int a, int b)      { return a + b; }
    public int subtract(int a, int b) { return a - b; }
    public int multiply(int a, int b) { return a * b; }

    /**
     * Integer division.
     * @throws ArithmeticException if {@code b == 0}
     */
    public double divide(double a, double b) {
        if (b == 0) throw new ArithmeticException("Division by zero");
        return a / b;
    }

    /** Returns the largest of three integers. */
    public int max(int a, int b, int c) {
        return Math.max(a, Math.max(b, c));
    }

    /**
     * Primality test.
     * @param n value to test (negative numbers and 0 are not prime)
     */
    public boolean isPrime(int n) {
        if (n < 2) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        for (int i = 3; (long) i * i <= n; i += 2) {
            if (n % i == 0) return false;
        }
        return true;
    }

    /**
     * Iterative factorial.
     * @throws IllegalArgumentException if {@code n < 0}
     */
    public long factorial(int n) {
        if (n < 0) throw new IllegalArgumentException("factorial undefined for negative numbers");
        long result = 1;
        for (int i = 2; i <= n; i++) result *= i;
        return result;
    }
}
