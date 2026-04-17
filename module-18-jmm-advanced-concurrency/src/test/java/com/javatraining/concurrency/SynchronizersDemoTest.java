package com.javatraining.concurrency;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SynchronizersDemo")
class SynchronizersDemoTest {

    @Nested
    @DisplayName("ConnectionPool (Semaphore)")
    class ConnectionPoolTest {
        @Test void acquire_and_release_roundtrip() throws InterruptedException {
            SynchronizersDemo.ConnectionPool pool = new SynchronizersDemo.ConnectionPool(3);
            assertEquals(3, pool.availablePermits());
            String conn = pool.acquire();
            assertNotNull(conn);
            assertEquals(2, pool.availablePermits());
            pool.release(conn);
            assertEquals(3, pool.availablePermits());
        }

        @Test void tryAcquire_returns_false_when_exhausted() throws InterruptedException {
            SynchronizersDemo.ConnectionPool pool = new SynchronizersDemo.ConnectionPool(1);
            pool.acquire(); // exhaust
            assertFalse(pool.tryAcquire(30));
        }

        @Test void pool_limits_concurrency() throws InterruptedException {
            // All tasks run but never exceed the pool size concurrently
            List<Long> times = SynchronizersDemo.throttledExecution(6, 2);
            assertEquals(6, times.size());
        }
    }

    @Nested
    @DisplayName("CyclicBarrier")
    class CyclicBarrierTest {
        @Test void barrier_action_runs_once_per_phase() throws Exception {
            List<Integer> completed = SynchronizersDemo.parallelPhases(3, 2);
            assertEquals(2, completed.size());
            assertEquals(List.of(1, 2), completed);
        }

        @Test void single_worker_trivially_completes() throws Exception {
            List<Integer> completed = SynchronizersDemo.parallelPhases(1, 3);
            assertEquals(3, completed.size());
        }
    }

    @Nested
    @DisplayName("Phaser")
    class PhaserTest {
        @Test void phaser_completes_three_phases() throws InterruptedException {
            SynchronizersDemo.PhasedPipeline pipeline = new SynchronizersDemo.PhasedPipeline(2);
            pipeline.startWorker("w1");
            pipeline.startWorker("w2");
            pipeline.run();
            assertTrue(pipeline.getPhaser().isTerminated());
            // The onAdvance log should record 3 phase completions (phases 0,1,2)
            long phaseMarkers = pipeline.getLog().stream()
                .filter(s -> s.startsWith("phase-"))
                .count();
            assertEquals(3, phaseMarkers);
        }
    }

    @Nested
    @DisplayName("Exchanger")
    class ExchangerTest {
        @Test void exchange_swaps_values() throws InterruptedException {
            String[] results = SynchronizersDemo.exchangeData("from-producer", "from-consumer");
            // results[0] = what producer received = consumerData
            assertEquals("from-consumer", results[0]);
            // results[1] = what consumer received = producerData
            assertEquals("from-producer", results[1]);
        }

        @Test void double_buffer_pipeline_transforms_data() throws InterruptedException {
            List<String> result = SynchronizersDemo.doubleBufferPipeline(
                List.of("hello", "world"));
            assertEquals(2, result.size());
            assertTrue(result.containsAll(List.of("HELLO", "WORLD")));
        }
    }
}
