package com.javatraining.threads;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExecutorDemo")
class ExecutorDemoTest {

    @Nested
    @DisplayName("submit and Future")
    class Submit {
        @Test void computeOnThread_returns_square() throws Exception {
            assertEquals(25, ExecutorDemo.computeOnThread(5));
        }

        @Test void runAndWait_completes_task() throws Exception {
            int[] ran = {0};
            ExecutorDemo.runAndWait(() -> ran[0] = 1);
            assertEquals(1, ran[0]);
        }
    }

    @Nested
    @DisplayName("invokeAll")
    class InvokeAll {
        @Test void invokeAllSquares_returns_squares_in_order() throws Exception {
            List<Integer> result = ExecutorDemo.invokeAllSquares(List.of(1, 2, 3, 4, 5));
            assertEquals(List.of(1, 4, 9, 16, 25), result);
        }

        @Test void invokeAllSquares_empty_list_returns_empty() throws Exception {
            assertTrue(ExecutorDemo.invokeAllSquares(List.of()).isEmpty());
        }
    }

    @Nested
    @DisplayName("invokeAny")
    class InvokeAny {
        @Test void invokeAny_returns_first_result() throws Exception {
            List<Callable<String>> tasks = List.of(
                () -> "fast",
                () -> { Thread.sleep(1000); return "slow"; }
            );
            String result = ExecutorDemo.invokeAnyFastest(tasks);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("timed Future.get")
    class TimedGet {
        @Test void returns_result_when_fast() throws Exception {
            Optional<String> result = ExecutorDemo.getWithTimeout(() -> "done", 500);
            assertTrue(result.isPresent());
            assertEquals("done", result.get());
        }

        @Test void returns_empty_when_slow() throws Exception {
            Optional<String> result = ExecutorDemo.getWithTimeout(() -> {
                Thread.sleep(500);
                return "late";
            }, 50);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("ThreadPoolExecutor")
    class PoolTest {
        @Test void buildBoundedPool_has_correct_core_size() {
            ThreadPoolExecutor pool = ExecutorDemo.buildBoundedPool(2, 4, 10);
            assertEquals(2, pool.getCorePoolSize());
            assertEquals(4, pool.getMaximumPoolSize());
            pool.shutdown();
        }
    }

    @Nested
    @DisplayName("ScheduledExecutor")
    class Scheduled {
        @Test void scheduleAtRate_fires_n_times() throws InterruptedException {
            List<Long> times = ExecutorDemo.scheduleAtRate(3, 20);
            assertEquals(3, times.size());
        }
    }

    @Nested
    @DisplayName("CompletableFuture")
    class CF {
        @Test void asyncPipeline_applies_transforms() throws Exception {
            String result = ExecutorDemo.asyncPipeline(5).get();
            assertEquals("RESULT=25", result);
        }

        @Test void combineTwo_sums_transformed_values() throws Exception {
            // a=3: 3*2=6, b=4: 4*3=12, sum=18
            assertEquals(18, ExecutorDemo.combineTwo(3, 4).get());
        }

        @Test void withFallback_returns_result_on_success() throws Exception {
            assertEquals("hello", ExecutorDemo.withFallback(() -> "hello").get());
        }

        @Test void withFallback_returns_fallback_on_exception() throws Exception {
            assertEquals("fallback", ExecutorDemo.withFallback(() -> {
                throw new RuntimeException("boom");
            }).get());
        }

        @Test void allSquares_computes_all() throws Exception {
            List<Integer> result = ExecutorDemo.allSquares(List.of(1, 2, 3, 4)).get();
            assertEquals(List.of(1, 4, 9, 16), result);
        }
    }
}
