package com.javatraining.threads;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VirtualThreadsDemo")
class VirtualThreadsDemoTest {

    @Nested
    @DisplayName("Virtual thread creation")
    class Creation {
        @Test void startVirtual_is_virtual_thread() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            Thread t = VirtualThreadsDemo.startVirtual("vt-test", latch::countDown);
            assertTrue(t.isVirtual());
            assertTrue(latch.await(1, TimeUnit.SECONDS));
        }

        @Test void virtualExecutor_is_non_null() {
            ExecutorService exec = VirtualThreadsDemo.virtualExecutor();
            assertNotNull(exec);
            exec.shutdown();
        }
    }

    @Nested
    @DisplayName("Scale test")
    class Scale {
        @Test void runConcurrent_all_complete() throws InterruptedException {
            int count = 500;
            assertEquals(count, VirtualThreadsDemo.runConcurrent(count, 5));
        }
    }

    @Nested
    @DisplayName("computeAll with virtual executor")
    class ComputeAll {
        @Test void computeAll_returns_squares() throws Exception {
            List<Integer> result = VirtualThreadsDemo.computeAll(List.of(1, 2, 3, 4, 5));
            assertEquals(List.of(1, 4, 9, 16, 25), result);
        }

        @Test void computeAll_empty_returns_empty() throws Exception {
            assertTrue(VirtualThreadsDemo.computeAll(List.of()).isEmpty());
        }
    }

    @Nested
    @DisplayName("Fan-out search")
    class FanOut {
        @Test void fanOutSearch_returns_result_for_each_source() throws Exception {
            List<VirtualThreadsDemo.SearchResult> results =
                VirtualThreadsDemo.fanOutSearch(List.of("alpha", "beta", "gamma"));
            assertEquals(3, results.size());
            assertTrue(results.stream().anyMatch(r -> r.source().equals("alpha")));
        }

        @Test void fanOutSearch_uppercases_values() throws Exception {
            List<VirtualThreadsDemo.SearchResult> results =
                VirtualThreadsDemo.fanOutSearch(List.of("hello"));
            assertEquals("HELLO", results.get(0).value());
        }
    }

    @Nested
    @DisplayName("SimpleRequestHandler")
    class Handler {
        @Test void handles_multiple_requests() throws Exception {
            VirtualThreadsDemo.SimpleRequestHandler handler =
                new VirtualThreadsDemo.SimpleRequestHandler();
            List<Future<String>> futures = List.of(
                handler.handle("req1"),
                handler.handle("req2"),
                handler.handle("req3")
            );
            for (Future<String> f : futures) {
                assertTrue(f.get(2, TimeUnit.SECONDS).startsWith("response:"));
            }
            assertEquals(3, handler.getHandledCount());
            handler.shutdown();
        }
    }

    @Nested
    @DisplayName("Spawn time comparison")
    class SpawnTime {
        @Test void virtual_threads_spawn_without_error() throws InterruptedException {
            // Just verify no exception for a modest count
            long ms = VirtualThreadsDemo.measureSpawnTime(200, true);
            assertTrue(ms >= 0);
        }
    }
}
