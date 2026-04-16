package com.javatraining.streams;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PrimitiveStreamsTest {

    // ── IntStream ─────────────────────────────────────────────────────────────

    @Nested
    class IntStreamTests {

        @Test
        void rangeSum_1_to_100() {
            assertEquals(5050L, PrimitiveStreams.rangeSum(1, 100));
        }

        @Test
        void rangeSum_single_element() {
            assertEquals(7L, PrimitiveStreams.rangeSum(7, 7));
        }

        @Test
        void evensInRange_returns_even_numbers() {
            assertEquals(List.of(2, 4, 6, 8, 10), PrimitiveStreams.evensInRange(1, 10));
        }

        @Test
        void evensInRange_starts_at_even_lo() {
            assertEquals(List.of(4, 6), PrimitiveStreams.evensInRange(4, 6));
        }

        @Test
        void multiplicationRow_correct_values() {
            assertArrayEquals(new int[]{3, 6, 9, 12, 15},
                PrimitiveStreams.multiplicationRow(3, 5));
        }
    }

    // ── mapToInt / statistics ─────────────────────────────────────────────────

    @Nested
    class MapToIntTests {

        List<PrimitiveStreams.Employee> employees = List.of(
            new PrimitiveStreams.Employee("Alice", "Eng",  120_000),
            new PrimitiveStreams.Employee("Bob",   "HR",    80_000),
            new PrimitiveStreams.Employee("Carol", "Eng",  100_000)
        );

        @Test
        void totalSalary_sums_all() {
            assertEquals(300_000, PrimitiveStreams.totalSalary(employees));
        }

        @Test
        void averageSalary_correct() {
            assertEquals(100_000.0,
                PrimitiveStreams.averageSalary(employees).orElse(-1), 0.001);
        }

        @Test
        void averageSalary_empty_is_empty() {
            assertTrue(PrimitiveStreams.averageSalary(List.of()).isEmpty());
        }

        @Test
        void salaryStats_min_max() {
            IntSummaryStatistics s = PrimitiveStreams.salaryStats(employees);
            assertEquals(80_000, s.getMin());
            assertEquals(120_000, s.getMax());
            assertEquals(3, s.getCount());
        }

        @Test
        void aboveAverageSalary_returns_correct_employees() {
            List<String> names = PrimitiveStreams.aboveAverageSalary(employees)
                .stream().map(PrimitiveStreams.Employee::name).toList();
            assertTrue(names.contains("Alice"));
            assertFalse(names.contains("Bob"));
        }
    }

    // ── LongStream ────────────────────────────────────────────────────────────

    @Nested
    class LongStreamTests {

        @Test
        void factorial_0() { assertEquals(1L, PrimitiveStreams.factorial(0)); }

        @Test
        void factorial_5() { assertEquals(120L, PrimitiveStreams.factorial(5)); }

        @Test
        void factorial_10() { assertEquals(3628800L, PrimitiveStreams.factorial(10)); }

        @Test
        void factorial_negative_throws() {
            assertThrows(IllegalArgumentException.class,
                () -> PrimitiveStreams.factorial(-1));
        }

        @Test
        void sumOfSquares_5() {
            assertEquals(55L, PrimitiveStreams.sumOfSquares(5)); // 1+4+9+16+25
        }

        @Test
        void sumOfSquares_1() {
            assertEquals(1L, PrimitiveStreams.sumOfSquares(1));
        }
    }

    // ── DoubleStream ──────────────────────────────────────────────────────────

    @Nested
    class DoubleStreamTests {

        @Test
        void stdDev_known_value() {
            // classic example: [2,4,4,4,5,5,7,9] stddev = 2.0
            assertEquals(2.0,
                PrimitiveStreams.stdDev(List.of(2.0,4.0,4.0,4.0,5.0,5.0,7.0,9.0)),
                0.0001);
        }

        @Test
        void stdDev_single_element_is_zero() {
            assertEquals(0.0, PrimitiveStreams.stdDev(List.of(5.0)));
        }

        @Test
        void normalise_maps_to_0_1() {
            List<Double> result = PrimitiveStreams.normalise(List.of(0.0, 5.0, 10.0));
            assertEquals(0.0, result.get(0), 0.0001);
            assertEquals(0.5, result.get(1), 0.0001);
            assertEquals(1.0, result.get(2), 0.0001);
        }

        @Test
        void normalise_all_same_returns_zeros() {
            List<Double> result = PrimitiveStreams.normalise(List.of(3.0, 3.0, 3.0));
            result.forEach(v -> assertEquals(0.0, v, 0.0001));
        }
    }

    // ── mapToObj ──────────────────────────────────────────────────────────────

    @Nested
    class MapToObjTests {

        @Test
        void fibonacci_first_8() {
            assertEquals(List.of(0L, 1L, 1L, 2L, 3L, 5L, 8L, 13L),
                PrimitiveStreams.fibonacci(8));
        }

        @Test
        void fibonacci_zero_returns_empty() {
            assertTrue(PrimitiveStreams.fibonacci(0).isEmpty());
        }

        @Test
        void columnLabels_5_labels() {
            assertEquals(List.of("A","B","C","D","E"),
                PrimitiveStreams.columnLabels(5));
        }

        @Test
        void columnLabels_zero_returns_empty() {
            assertTrue(PrimitiveStreams.columnLabels(0).isEmpty());
        }
    }
}
