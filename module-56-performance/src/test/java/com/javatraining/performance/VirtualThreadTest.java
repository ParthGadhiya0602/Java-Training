package com.javatraining.performance;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure Java 21 tests — no Spring context, no HTTP server.
 * These tests prove behavioral facts about virtual threads that the JMH benchmarks
 * quantify: namely that virtual threads unmount from their carrier while blocked,
 * allowing far more concurrent waiters than there are OS threads.
 */
class VirtualThreadTest {

    @Test
    void virtual_thread_is_marked_virtual_and_is_daemon_by_default() throws Exception {
        AtomicBoolean seenVirtual = new AtomicBoolean(false);

        Thread vt = Thread.ofVirtual()
                .name("test-vthread")
                .start(() -> seenVirtual.set(Thread.currentThread().isVirtual()));
        vt.join();

        assertThat(vt.isVirtual()).isTrue();
        assertThat(vt.isDaemon()).isTrue();    // virtual threads are always daemons
        assertThat(seenVirtual.get()).isTrue(); // Thread.currentThread().isVirtual() inside
    }

    /**
     * Proves that virtual threads unmount from carrier threads during blocking.
     *
     * 500 tasks all call {@code release.await()} simultaneously. A fixed thread pool
     * of size N could only block N tasks at once — with N < 500 it would deadlock
     * (tasks waiting for the latch that can't complete because no thread can run).
     *
     * With virtual threads, all 500 can block concurrently regardless of carrier count,
     * so {@code allWaiting} reaches zero and the test passes.
     */
    @Test
    void virtual_threads_support_more_concurrent_blockers_than_carrier_thread_count()
            throws Exception {
        int taskCount = 500;
        CountDownLatch allWaiting = new CountDownLatch(taskCount);
        CountDownLatch release    = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    allWaiting.countDown();   // signal: "I have reached the blocking point"
                    release.await();          // block — virtual thread unmounts here
                    return null;
                });
            }

            // All 500 virtual threads reach this barrier concurrently
            boolean allReachedBarrier = allWaiting.await(10, TimeUnit.SECONDS);
            release.countDown(); // unblock everyone

            assertThat(allReachedBarrier)
                    .as("All %d virtual threads must be able to block concurrently", taskCount)
                    .isTrue();
        }
    }
}
