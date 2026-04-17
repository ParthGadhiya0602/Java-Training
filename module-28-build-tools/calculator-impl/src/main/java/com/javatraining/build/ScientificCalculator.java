package com.javatraining.build;

/**
 * Extended calculator with scientific operations.
 * Extends {@link BasicCalculator} rather than re-implementing basic ops.
 *
 * Demonstrates that the impl module can have internal class hierarchies
 * that depend purely on the api module's interface.
 */
public class ScientificCalculator extends BasicCalculator {

    /** Square root. @throws ArithmeticException if {@code a < 0} */
    public double sqrt(double a) {
        if (a < 0) throw new ArithmeticException("sqrt of negative number");
        return Math.sqrt(a);
    }

    /** {@code base} raised to {@code exponent}. */
    public double pow(double base, double exponent) {
        return Math.pow(base, exponent);
    }

    /** Natural logarithm. @throws ArithmeticException if {@code a <= 0} */
    public double ln(double a) {
        if (a <= 0) throw new ArithmeticException("ln of non-positive number");
        return Math.log(a);
    }

    /** Base-10 logarithm. @throws ArithmeticException if {@code a <= 0} */
    public double log10(double a) {
        if (a <= 0) throw new ArithmeticException("log10 of non-positive number");
        return Math.log10(a);
    }

    /** Factorial (iterative). @throws IllegalArgumentException if {@code n < 0} */
    public long factorial(int n) {
        if (n < 0) throw new IllegalArgumentException("factorial undefined for n < 0");
        long result = 1;
        for (int i = 2; i <= n; i++) result *= i;
        return result;
    }
}
