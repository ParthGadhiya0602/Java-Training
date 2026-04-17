package com.javatraining.build;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BasicCalculator}.
 *
 * This test class is in {@code calculator-impl}, which depends on
 * {@code calculator-api}.  The Calculator interface comes from the api module;
 * BasicCalculator comes from this module — a cross-module dependency in action.
 */
class BasicCalculatorTest {

    Calculator calc;   // typed to the interface from calculator-api

    @BeforeEach
    void setUp() {
        calc = new BasicCalculator();
    }

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({"1,2,3", "0,0,0", "-5,3,-2", "100,200,300"})
    void addition(double a, double b, double expected) {
        assertEquals(expected, calc.add(a, b), 0.001);
    }

    @ParameterizedTest(name = "{0} - {1} = {2}")
    @CsvSource({"5,3,2", "0,0,0", "1,10,-9"})
    void subtraction(double a, double b, double expected) {
        assertEquals(expected, calc.subtract(a, b), 0.001);
    }

    @ParameterizedTest(name = "{0} * {1} = {2}")
    @CsvSource({"3,4,12", "0,5,0", "-2,3,-6"})
    void multiplication(double a, double b, double expected) {
        assertEquals(expected, calc.multiply(a, b), 0.001);
    }

    @ParameterizedTest(name = "{0} / {1} = {2}")
    @CsvSource({"10,2,5", "9,3,3", "1,4,0.25"})
    void division(double a, double b, double expected) {
        assertEquals(expected, calc.divide(a, b), 0.001);
    }

    @Test
    void divide_by_zero_throws() {
        assertThrows(ArithmeticException.class, () -> calc.divide(5, 0));
    }
}
