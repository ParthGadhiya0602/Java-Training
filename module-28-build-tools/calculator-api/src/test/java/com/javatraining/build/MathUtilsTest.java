package com.javatraining.build;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MathUtils} — lives in the calculator-api module.
 * Demonstrates that each Maven module can have its own test suite.
 */
class MathUtilsTest {

    @ParameterizedTest(name = "gcd({0},{1}) = {2}")
    @CsvSource({"12,8,4", "9,6,3", "7,5,1", "0,5,5", "100,75,25"})
    void gcd(int a, int b, int expected) {
        assertEquals(expected, MathUtils.gcd(a, b));
    }

    @ParameterizedTest(name = "lcm({0},{1}) = {2}")
    @CsvSource({"4,6,12", "3,5,15", "7,7,7", "0,5,0"})
    void lcm(int a, int b, int expected) {
        assertEquals(expected, MathUtils.lcm(a, b));
    }

    @ParameterizedTest(name = "{0} is prime")
    @ValueSource(ints = {2, 3, 5, 7, 11, 13, 97})
    void known_primes(int n) {
        assertTrue(MathUtils.isPrime(n));
    }

    @ParameterizedTest(name = "{0} is not prime")
    @ValueSource(ints = {0, 1, 4, 6, 9, 25})
    void known_composites(int n) {
        assertFalse(MathUtils.isPrime(n));
    }

    @Test
    void clamp_returns_value_within_range() {
        assertEquals(5.0, MathUtils.clamp(5.0, 0.0, 10.0));
    }

    @Test
    void clamp_returns_min_when_value_below() {
        assertEquals(0.0, MathUtils.clamp(-3.0, 0.0, 10.0));
    }

    @Test
    void clamp_returns_max_when_value_above() {
        assertEquals(10.0, MathUtils.clamp(15.0, 0.0, 10.0));
    }

    @Test
    void clamp_invalid_range_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> MathUtils.clamp(5.0, 10.0, 0.0));
    }

    @Test
    void count_primes_up_to_10() {
        assertEquals(4, MathUtils.countPrimes(10)); // 2, 3, 5, 7
    }

    @Test
    void count_primes_up_to_1() {
        assertEquals(0, MathUtils.countPrimes(1));
    }
}
