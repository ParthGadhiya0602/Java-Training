package com.javatraining.controlflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NumberAnalyzer — logic correctness")
class NumberAnalyzerTest {

    // -------------------------------------------------------------------------
    // isPrime
    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "{0} is prime")
    @ValueSource(ints = {2, 3, 5, 7, 11, 13, 17, 97})
    @DisplayName("Known primes are correctly identified")
    void knownPrimes(int n) {
        assertTrue(NumberAnalyzer.isPrime(n));
    }

    @ParameterizedTest(name = "{0} is not prime")
    @ValueSource(ints = {0, 1, 4, 6, 9, 15, 100})
    @DisplayName("Known composites are correctly rejected")
    void knownComposites(int n) {
        assertFalse(NumberAnalyzer.isPrime(n));
    }

    // -------------------------------------------------------------------------
    // sieve
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Sieve finds correct count of primes up to 50")
    void sieveCount() {
        int[] primes = NumberAnalyzer.sieve(50);
        assertEquals(15, primes.length); // 2,3,5,7,11,13,17,19,23,29,31,37,41,43,47
    }

    @Test
    @DisplayName("Sieve result starts with 2 and ends with 47 for limit 50")
    void sieveBoundaryValues() {
        int[] primes = NumberAnalyzer.sieve(50);
        assertEquals(2,  primes[0]);
        assertEquals(47, primes[primes.length - 1]);
    }

    // -------------------------------------------------------------------------
    // primeFactors
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Prime factorization of 12 = [2, 2, 3]")
    void factorsOf12() {
        assertArrayEquals(new int[]{2, 2, 3}, NumberAnalyzer.primeFactors(12));
    }

    @Test
    @DisplayName("Prime factorization of 360 = [2,2,2,3,3,5]")
    void factorsOf360() {
        assertArrayEquals(new int[]{2, 2, 2, 3, 3, 5}, NumberAnalyzer.primeFactors(360));
    }

    @Test
    @DisplayName("Prime factorization of a prime returns itself")
    void factorsOfPrime() {
        assertArrayEquals(new int[]{97}, NumberAnalyzer.primeFactors(97));
    }

    @Test
    @DisplayName("Prime factorization of 1 returns empty array")
    void factorsOfOne() {
        assertEquals(0, NumberAnalyzer.primeFactors(1).length);
    }

    // -------------------------------------------------------------------------
    // digitalRoot
    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "digitalRoot({0}) = {1}")
    @CsvSource({
        "9875,  2",   // 9+8+7+5=29 → 2+9=11 → 1+1=2
        "493,   7",   // 4+9+3=16 → 1+6=7
        "1,     1",   // already single digit
        "999,   9",   // 9+9+9=27 → 2+7=9
        "0,     0",   // edge: zero
        "-9875, 2",   // negative: same as positive
    })
    @DisplayName("Digital root reduces to single digit correctly")
    void digitalRoot(int input, int expected) {
        assertEquals(expected, NumberAnalyzer.digitalRoot(input));
    }

    // -------------------------------------------------------------------------
    // fizzBuzz
    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "fizzBuzz({0}) = {1}")
    @CsvSource({
        "1,  1",
        "3,  Fizz",
        "5,  Buzz",
        "15, FizzBuzz",
        "30, FizzBuzz",
        "9,  Fizz",
        "25, Buzz",
        "7,  7",
    })
    @DisplayName("FizzBuzz produces correct labels for all cases")
    void fizzBuzz(int n, String expected) {
        assertEquals(expected, NumberAnalyzer.fizzBuzz(n));
    }
}
