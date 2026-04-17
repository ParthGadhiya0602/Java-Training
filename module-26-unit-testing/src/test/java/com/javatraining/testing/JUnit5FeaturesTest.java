package com.javatraining.testing;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * A guided tour of JUnit 5 (Jupiter) features.
 *
 * Topics covered:
 *   Lifecycle       — @BeforeAll / @AfterAll / @BeforeEach / @AfterEach
 *   Basic assertions — assertEquals, assertFalse, assertNotNull
 *   assertAll        — grouped assertions (all branches checked on failure)
 *   assertThrows     — exception type and message
 *   assertTimeout    — performance bounds
 *   @Nested          — logical grouping of related tests
 *   @ParameterizedTest — @ValueSource / @CsvSource / @MethodSource / @EnumSource / @NullSource
 *   @RepeatedTest    — run the same test N times
 *   Assumptions      — assumeTrue / assumingThat (conditional test execution)
 *   @Disabled        — mark a test as intentionally skipped
 *   @Tag             — categorise tests for selective execution
 *   @TestMethodOrder — deterministic method ordering
 *   @DisplayName     — human-readable names in reports
 */
@DisplayName("JUnit 5 Features")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JUnit5FeaturesTest {

    // Tracks lifecycle event counts across the suite
    static int beforeAllCount;
    static int afterAllCount;

    Calculator calculator;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * @BeforeAll runs once before the first test method.
     * Must be static (or the class must use @TestInstance(PER_CLASS)).
     */
    @BeforeAll
    static void suiteSetUp() {
        beforeAllCount++;
    }

    /**
     * @AfterAll runs once after the last test method.
     * Must also be static by default.
     */
    @AfterAll
    static void suiteTearDown() {
        afterAllCount++;
    }

    /**
     * @BeforeEach runs before every single test method.
     * Use it to reset shared mutable state so tests remain isolated.
     */
    @BeforeEach
    void setUp() {
        calculator = new Calculator();
    }

    // ── Basic Assertions ──────────────────────────────────────────────────────

    @Test
    @DisplayName("2 + 3 = 5")
    @Order(1)
    void addition() {
        assertEquals(5, calculator.add(2, 3));
    }

    @Test
    @DisplayName("3 - 2 = 1")
    @Order(2)
    void subtraction() {
        assertEquals(1, calculator.subtract(3, 2));
    }

    // ── assertAll: all branches checked even if one fails ─────────────────────

    @Test
    @DisplayName("All four arithmetic operations")
    void all_arithmetic_operations() {
        assertAll("calculator",
            () -> assertEquals(5,   calculator.add(2, 3),      "add"),
            () -> assertEquals(1,   calculator.subtract(3, 2), "subtract"),
            () -> assertEquals(12,  calculator.multiply(3, 4), "multiply"),
            () -> assertEquals(2.5, calculator.divide(5, 2),   "divide")
        );
    }

    // ── assertThrows ──────────────────────────────────────────────────────────

    @Test
    void divide_by_zero_throws_ArithmeticException() {
        ArithmeticException ex = assertThrows(ArithmeticException.class,
            () -> calculator.divide(10, 0));
        assertEquals("Division by zero", ex.getMessage());
    }

    @Test
    void factorial_of_negative_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> calculator.factorial(-1));
    }

    // ── assertTimeout ─────────────────────────────────────────────────────────

    @Test
    void factorial_20_completes_within_one_second() {
        assertTimeout(Duration.ofSeconds(1),
            () -> calculator.factorial(20),
            "factorial(20) should complete well under 1 s");
    }

    // ── @Nested: logical grouping ─────────────────────────────────────────────

    @Nested
    @DisplayName("Prime number detection")
    class PrimeTests {

        @Test
        void one_is_not_prime() {
            assertFalse(calculator.isPrime(1));
        }

        @Test
        void two_is_prime() {
            assertTrue(calculator.isPrime(2));
        }

        @Test
        void negative_is_not_prime() {
            assertFalse(calculator.isPrime(-7));
        }

        @ParameterizedTest(name = "{0} is prime")
        @ValueSource(ints = {2, 3, 5, 7, 11, 13, 97})
        void known_primes(int n) {
            assertTrue(calculator.isPrime(n));
        }

        @ParameterizedTest(name = "{0} is not prime")
        @ValueSource(ints = {1, 4, 6, 8, 9, 10, 25})
        void known_composites(int n) {
            assertFalse(calculator.isPrime(n));
        }
    }

    // ── @ParameterizedTest with @CsvSource ────────────────────────────────────

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({
        "1,  1,   2",
        "0,  0,   0",
        "-1, 1,   0",
        "100, -50, 50"
    })
    void addition_csv(int a, int b, int expected) {
        assertEquals(expected, calculator.add(a, b));
    }

    // ── @ParameterizedTest with @MethodSource ─────────────────────────────────

    static Stream<Arguments> divisionCases() {
        return Stream.of(
            Arguments.of(10.0, 2.0, 5.0),
            Arguments.of(9.0,  3.0, 3.0),
            Arguments.of(1.0,  4.0, 0.25)
        );
    }

    @ParameterizedTest(name = "{0} / {1} = {2}")
    @MethodSource("divisionCases")
    void division_method_source(double a, double b, double expected) {
        assertEquals(expected, calculator.divide(a, b), 0.001);
    }

    // ── @ParameterizedTest with @EnumSource ───────────────────────────────────

    @ParameterizedTest(name = "{0} is a weekday (value 1-5)")
    @EnumSource(value = DayOfWeek.class,
                names = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"})
    void weekdays_have_value_1_through_5(DayOfWeek day) {
        assertTrue(day.getValue() <= 5);
    }

    // ── @ParameterizedTest with @NullSource ───────────────────────────────────

    @ParameterizedTest
    @NullSource
    void null_is_not_a_palindrome(String s) {
        assertFalse(StringUtils.isPalindrome(s));
    }

    // ── @RepeatedTest ─────────────────────────────────────────────────────────

    @RepeatedTest(value = 3, name = "run {currentRepetition}/{totalRepetitions}")
    void max_of_three(RepetitionInfo info) {
        assertEquals(7, calculator.max(7, 3, 5));
        assertTrue(info.getCurrentRepetition() <= info.getTotalRepetitions());
    }

    // ── Assumptions ───────────────────────────────────────────────────────────

    /**
     * assumeTrue(false) aborts the test — it shows as "skipped" in the report,
     * not as a failure.  Use this to skip tests when preconditions aren't met
     * (e.g. external service unavailable, wrong OS).
     */
    @Test
    void assumption_false_aborts_test() {
        assumeTrue(false, "Skipping intentionally to demonstrate assumeTrue");
        fail("This line is never reached");
    }

    @Test
    void assumingThat_runs_assertion_conditionally() {
        // On any OS that isn't Windows the HOME variable must exist
        boolean onWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        assumingThat(!onWindows, () -> assertNotNull(System.getenv("HOME")));
    }

    // ── @Disabled ─────────────────────────────────────────────────────────────

    @Test
    @Disabled("Placeholder: feature not yet implemented")
    void future_feature() {
        fail("Not implemented");
    }

    // ── @Tag ─────────────────────────────────────────────────────────────────

    @Test
    @Tag("slow")
    @DisplayName("factorial(20) exact value")
    void factorial_20_exact_value() {
        assertEquals(2_432_902_008_176_640_000L, calculator.factorial(20));
    }

    // ── Lifecycle sanity ─────────────────────────────────────────────────────

    @Test
    @Order(Integer.MAX_VALUE)
    void before_all_ran_exactly_once() {
        assertEquals(1, beforeAllCount,
            "@BeforeAll should run exactly once for the whole class");
    }
}
