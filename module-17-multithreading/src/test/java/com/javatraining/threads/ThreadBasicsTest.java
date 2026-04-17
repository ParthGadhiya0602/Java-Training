package com.javatraining.threads;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ThreadBasics")
class ThreadBasicsTest {

    @Nested
    @DisplayName("Thread creation")
    class Creation {
        @Test void startThread_thread_completes() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            Thread t = ThreadBasics.startThread(latch::countDown);
            t.join(1000);
            assertEquals(0, latch.getCount());
        }

        @Test void startDaemonThread_is_daemon() throws InterruptedException {
            Thread t = ThreadBasics.startDaemonThread("bg", () -> {});
            t.join(1000);
            assertTrue(t.isDaemon());
        }

        @Test void buildAndStart_uses_given_name() throws InterruptedException {
            Thread t = ThreadBasics.buildAndStart("my-thread", false, () -> {});
            assertEquals("my-thread", t.getName());
            t.join(1000);
        }
    }

    @Nested
    @DisplayName("fan-out and join")
    class FanOut {
        @Test void fanOutAndJoin_all_threads_complete() throws InterruptedException {
            assertEquals(10, ThreadBasics.fanOutAndJoin(10));
        }

        @Test void joinWithTimeout_returns_true_when_thread_finishes() throws InterruptedException {
            Thread t = new Thread(() -> {});
            t.start();
            assertTrue(ThreadBasics.joinWithTimeout(t, 500));
        }

        @Test void joinWithTimeout_returns_false_when_thread_hangs() throws InterruptedException {
            CountDownLatch block = new CountDownLatch(1);
            Thread t = new Thread(() -> {
                try { block.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            t.start();
            assertFalse(ThreadBasics.joinWithTimeout(t, 50));
            block.countDown();
            t.join(500);
        }
    }

    @Nested
    @DisplayName("Interruption")
    class Interruption {
        @Test void interrupt_stops_worker() throws InterruptedException {
            List<String> log = new ArrayList<>();
            Thread t = ThreadBasics.startInterruptibleWorker(log);
            Thread.sleep(30);
            t.interrupt();
            t.join(500);
            assertFalse(t.isAlive());
            assertTrue(log.contains("interrupted"));
        }

        @Test void worker_logs_working_before_interrupt() throws InterruptedException {
            List<String> log = new ArrayList<>();
            Thread t = ThreadBasics.startInterruptibleWorker(log);
            Thread.sleep(30);
            t.interrupt();
            t.join(500);
            assertTrue(log.contains("working"));
        }
    }

    @Nested
    @DisplayName("CountDownLatch")
    class Latch {
        @Test void parallelSum_correct() throws InterruptedException {
            int[] values = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
            assertEquals(55, ThreadBasics.parallelSum(values));
        }

        @Test void startTogether_all_threads_record_time() throws InterruptedException {
            List<Long> times = ThreadBasics.startTogether(5);
            assertEquals(5, times.size());
        }
    }

    @Nested
    @DisplayName("ThreadLocal")
    class TL {
        @Test void each_thread_sees_own_value() throws InterruptedException {
            List<String> results = ThreadBasics.threadLocalDemo(5);
            assertEquals(5, results.size());
            // every result should match the pattern "user-N"
            assertTrue(results.stream().allMatch(s -> s.startsWith("user-")));
        }

        @Test void no_thread_sees_another_threads_value() throws InterruptedException {
            List<String> results = ThreadBasics.threadLocalDemo(4);
            // all values must be distinct (each thread set its own)
            assertEquals(results.size(), results.stream().distinct().count());
        }
    }
}
