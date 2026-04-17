package com.javatraining.build;

/**
 * Public API contract for calculator operations.
 * Lives in {@code calculator-api} — other modules depend on this interface,
 * not on any concrete implementation.
 *
 * This separation is the Java equivalent of "program to an interface":
 * {@code calculator-impl} can be swapped for any other implementation
 * without touching any code that depends on {@code calculator-api}.
 */
public interface Calculator {
    double add(double a, double b);
    double subtract(double a, double b);
    double multiply(double a, double b);

    /**
     * @throws ArithmeticException if {@code b == 0}
     */
    double divide(double a, double b);
}
