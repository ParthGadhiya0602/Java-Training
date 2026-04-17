package com.javatraining.jvm;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JitAndPerformanceDemo")
class JitAndPerformanceDemoTest {

    @Nested
    @DisplayName("Timing utilities")
    class Timing {
        @Test void time_captures_elapsed_ns() {
            var result = JitAndPerformanceDemo.time(() -> {
                long s = 0; for (int i = 0; i < 1000; i++) s += i; return s;
            });
            assertTrue(result.elapsedNs() >= 0);
        }

        @Test void time_returns_correct_result() {
            var result = JitAndPerformanceDemo.time(() -> 42);
            assertEquals(42, result.result());
        }

        @Test void timeLong_returns_correct_value() {
            var result = JitAndPerformanceDemo.timeLong(() -> 999L);
            assertEquals(999L, result.result());
        }

        @Test void warmAndMeasure_returns_positive_ns() {
            long ns = JitAndPerformanceDemo.warmAndMeasureNs(
                () -> { long s = 0; for (int i = 0; i < 1000; i++) s += i; },
                5, 7);
            assertTrue(ns >= 0);
        }
    }

    @Nested
    @DisplayName("Warmup measurement")
    class Warmup {
        @Test void returns_correct_number_of_samples() {
            long[] times = JitAndPerformanceDemo.measureWarmup(5);
            assertEquals(5, times.length);
        }

        @Test void all_samples_non_negative() {
            for (long t : JitAndPerformanceDemo.measureWarmup(4)) {
                assertTrue(t >= 0);
            }
        }
    }

    @Nested
    @DisplayName("Primitive vs boxed sum")
    class PrimitiveVsBoxed {
        @Test void sum_primitive_correct() {
            assertEquals(4950L, JitAndPerformanceDemo.sumPrimitive(100));
        }

        @Test void sum_boxed_correct() {
            List<Integer> list = JitAndPerformanceDemo.buildBoxedList(100);
            assertEquals(4950L, JitAndPerformanceDemo.sumBoxed(list));
        }

        @Test void boxed_list_has_correct_size() {
            assertEquals(50, JitAndPerformanceDemo.buildBoxedList(50).size());
        }
    }

    @Nested
    @DisplayName("String concatenation")
    class StringConcat {
        @Test void loop_concat_correct() {
            String result = JitAndPerformanceDemo.concatenateLoop(5);
            assertEquals("01234", result);
        }

        @Test void sb_concat_correct() {
            String result = JitAndPerformanceDemo.concatenateStringBuilder(5);
            assertEquals("01234", result);
        }

        @Test void both_produce_same_output() {
            assertEquals(
                JitAndPerformanceDemo.concatenateLoop(10),
                JitAndPerformanceDemo.concatenateStringBuilder(10));
        }
    }

    @Nested
    @DisplayName("Escape analysis demo")
    class EscapeAnalysis {
        @Test void compute_distance_3_4_5_triangle() {
            assertEquals(5.0,
                JitAndPerformanceDemo.computeDistance(0, 0, 3, 4), 1e-9);
        }

        @Test void compute_distance_same_point_is_zero() {
            assertEquals(0.0,
                JitAndPerformanceDemo.computeDistance(5, 5, 5, 5), 1e-9);
        }
    }

    @Nested
    @DisplayName("JIT info")
    class JitInfo {
        @Test void jit_name_not_blank() {
            // May be "HotSpot 64-Bit Tiered Compilers" or similar
            assertFalse(JitAndPerformanceDemo.jitName().isBlank());
        }

        @Test void compilation_time_is_non_negative_or_minus_one() {
            long t = JitAndPerformanceDemo.jitCompilationTimeMs();
            assertTrue(t >= -1);
        }
    }

    @Nested
    @DisplayName("Thread stats")
    class ThreadStats {
        @Test void thread_count_positive() {
            Map<String, Object> stats = JitAndPerformanceDemo.threadStats();
            assertTrue(((Number) stats.get("threadCount")).intValue() > 0);
        }

        @Test void thread_stats_has_expected_keys() {
            var stats = JitAndPerformanceDemo.threadStats();
            assertTrue(stats.containsKey("threadCount"));
            assertTrue(stats.containsKey("peakThreadCount"));
            assertTrue(stats.containsKey("daemonThreadCount"));
        }
    }

    @Nested
    @DisplayName("SimpleObjectPool")
    class ObjectPool {
        @Test void acquire_creates_new_when_empty() {
            var pool = new JitAndPerformanceDemo.SimpleObjectPool<>(StringBuilder::new);
            pool.acquire();
            assertEquals(1, pool.createdCount());
        }

        @Test void release_then_acquire_reuses_object() {
            var pool = new JitAndPerformanceDemo.SimpleObjectPool<>(StringBuilder::new);
            StringBuilder sb = pool.acquire();
            pool.release(sb);
            StringBuilder sb2 = pool.acquire();
            assertSame(sb, sb2);
            assertEquals(1, pool.createdCount()); // still only 1 created
        }

        @Test void pool_size_reflects_releases() {
            var pool = new JitAndPerformanceDemo.SimpleObjectPool<>(Object::new);
            pool.release(new Object());
            pool.release(new Object());
            assertEquals(2, pool.poolSize());
        }

        @Test void allocation_delta_non_negative() {
            long delta = JitAndPerformanceDemo.measureAllocationDelta(100);
            assertTrue(delta >= 0);
        }
    }
}
