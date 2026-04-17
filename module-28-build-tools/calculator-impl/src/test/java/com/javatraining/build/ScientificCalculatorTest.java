package com.javatraining.build;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScientificCalculatorTest {

    ScientificCalculator calc;

    @BeforeEach
    void setUp() { calc = new ScientificCalculator(); }

    // ── Inherited basic ops still work ────────────────────────────────────

    @Test
    void inherits_addition() {
        assertEquals(7.0, calc.add(3, 4), 0.001);
    }

    // ── Scientific operations ─────────────────────────────────────────────

    @Test
    void sqrt_positive() {
        assertEquals(3.0, calc.sqrt(9.0), 0.001);
    }

    @Test
    void sqrt_zero() {
        assertEquals(0.0, calc.sqrt(0.0), 0.001);
    }

    @Test
    void sqrt_negative_throws() {
        assertThrows(ArithmeticException.class, () -> calc.sqrt(-1.0));
    }

    @Test
    void pow_integer_exponent() {
        assertEquals(8.0, calc.pow(2, 3), 0.001);
    }

    @Test
    void pow_fractional_exponent() {
        assertEquals(2.0, calc.pow(4, 0.5), 0.001);
    }

    @Test
    void ln_of_e_is_one() {
        assertEquals(1.0, calc.ln(Math.E), 0.001);
    }

    @Test
    void ln_of_one_is_zero() {
        assertEquals(0.0, calc.ln(1.0), 0.001);
    }

    @Test
    void ln_non_positive_throws() {
        assertThrows(ArithmeticException.class, () -> calc.ln(0));
        assertThrows(ArithmeticException.class, () -> calc.ln(-1));
    }

    @Test
    void log10_of_100_is_2() {
        assertEquals(2.0, calc.log10(100.0), 0.001);
    }

    @Test
    void factorial_5() {
        assertEquals(120L, calc.factorial(5));
    }

    @Test
    void factorial_0_is_1() {
        assertEquals(1L, calc.factorial(0));
    }

    @Test
    void factorial_negative_throws() {
        assertThrows(IllegalArgumentException.class, () -> calc.factorial(-1));
    }
}
