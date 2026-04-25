package com.javatraining.build;

/**
 * Standard implementation of {@link Calculator}.
 * Lives in {@code calculator-impl} - depends on {@code calculator-api}
 * as a compile-scope Maven dependency.
 */
public class BasicCalculator implements Calculator {

    @Override public double add(double a, double b)      { return a + b; }
    @Override public double subtract(double a, double b) { return a - b; }
    @Override public double multiply(double a, double b) { return a * b; }

    @Override
    public double divide(double a, double b) {
        if (b == 0) throw new ArithmeticException("Division by zero");
        return a / b;
    }
}
