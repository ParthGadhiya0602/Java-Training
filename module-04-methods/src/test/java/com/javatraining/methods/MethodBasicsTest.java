package com.javatraining.methods;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MethodBasics - static, instance, guard clauses")
class MethodBasicsTest {

    @ParameterizedTest(name = "{0}°C = {1}°F")
    @CsvSource({
        "0,     32.0",
        "100,   212.0",
        "-40,   -40.0",   // famous crossover point
        "37,    98.6",    // body temperature
    })
    void celsiusToFahrenheit(double c, double expected) {
        assertEquals(expected, MethodBasics.celsiusToFahrenheit(c), 0.01);
    }

    @ParameterizedTest(name = "clamp({0}, {1}, {2}) = {3}")
    @CsvSource({
        "50,  0, 100, 50",   // within range
        "-5,  0, 100,  0",   // below min → min
        "150, 0, 100, 100",  // above max → max
        "0,   0, 100,   0",  // exact min boundary
        "100, 0, 100, 100",  // exact max boundary
    })
    void clamp(int value, int min, int max, int expected) {
        assertEquals(expected, MethodBasics.clamp(value, min, max));
    }

    @Test
    @DisplayName("clamp throws when min > max")
    void clampInvalidRange() {
        assertThrows(IllegalArgumentException.class,
            () -> MethodBasics.clamp(5, 100, 0));
    }

    @Test
    @DisplayName("safeDivide returns NaN for denominator 0")
    void safeDivideByZero() {
        assertTrue(Double.isNaN(MethodBasics.safeDivide(10, 0)));
    }

    @ParameterizedTest(name = "BMI({0}kg, {1}m) category = {2}")
    @CsvSource({
        "50,  1.75, Underweight",
        "70,  1.75, Normal weight",
        "85,  1.75, Overweight",
        "100, 1.75, Obese",
    })
    void bmiCategory(double weight, double height, String expected) {
        double bmi = MethodBasics.bmi(weight, height);
        assertEquals(expected, MethodBasics.bmiCategory(bmi));
    }

    @Test
    @DisplayName("bmi throws for non-positive weight")
    void bmiNegativeWeight() {
        assertThrows(IllegalArgumentException.class,
            () -> MethodBasics.bmi(-1, 1.75));
    }

    @Test
    @DisplayName("Instance methods operate on independent object state")
    void instanceMethodsArePerObject() {
        MethodBasics alice = new MethodBasics("Alice", 100);
        MethodBasics bob   = new MethodBasics("Bob", 100);

        alice.addPoints(50);
        // bob's score is unaffected
        assertEquals(150, alice.getScore());
        assertEquals(100, bob.getScore());
    }

    @Test
    @DisplayName("compareWith correctly identifies leading player")
    void compareWith() {
        MethodBasics a = new MethodBasics("A", 200);
        MethodBasics b = new MethodBasics("B", 150);

        assertTrue(a.compareWith(b).contains("A leads"));
        assertTrue(b.compareWith(a).contains("A leads"));

        MethodBasics c = new MethodBasics("C", 200);
        assertTrue(a.compareWith(c).contains("Tied"));
    }
}
