package com.javatraining.threads;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SynchronizationDemo")
class SynchronizationDemoTest {

    @Nested
    @DisplayName("volatile StopFlag")
    class VolatileTest {
        @Test void stop_terminates_worker() throws InterruptedException {
            SynchronizationDemo.StopFlag flag = new SynchronizationDemo.StopFlag();
            List<Integer> output = new ArrayList<>();
            Thread t = flag.startWorker(output);
            Thread.sleep(20);
            flag.stop();
            t.join(500);
            assertFalse(t.isAlive());
            assertFalse(output.isEmpty());
        }
    }

    @Nested
    @DisplayName("SafeCounter")
    class SafeCounterTest {
        @Test void concurrent_increments_are_exact() throws InterruptedException {
            SynchronizationDemo.SafeCounter c = new SynchronizationDemo.SafeCounter();
            int threads = 10, ops = 1000;
            List<Thread> list = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Thread t = new Thread(() -> { for (int j = 0; j < ops; j++) c.increment(); });
                list.add(t);
                t.start();
            }
            for (Thread t : list) t.join();
            assertEquals(threads * ops, c.get());
        }

        @Test void reset_zeroes_counter() throws InterruptedException {
            SynchronizationDemo.SafeCounter c = new SynchronizationDemo.SafeCounter();
            c.add(100);
            c.reset();
            assertEquals(0, c.get());
        }
    }

    @Nested
    @DisplayName("AtomicInteger")
    class AtomicTest {
        @Test void atomicIncrement_is_always_exact() throws InterruptedException {
            int threads = 10, ops = 1000;
            assertEquals(threads * ops,
                SynchronizationDemo.atomicIncrement(threads, ops));
        }

        @Test void trySetMax_updates_when_candidate_larger() {
            AtomicInteger ref = new AtomicInteger(5);
            assertTrue(SynchronizationDemo.trySetMax(ref, 10));
            assertEquals(10, ref.get());
        }

        @Test void trySetMax_no_update_when_candidate_smaller() {
            AtomicInteger ref = new AtomicInteger(10);
            assertFalse(SynchronizationDemo.trySetMax(ref, 5));
            assertEquals(10, ref.get());
        }

        @Test void trySetMax_no_update_when_equal() {
            AtomicInteger ref = new AtomicInteger(5);
            assertFalse(SynchronizationDemo.trySetMax(ref, 5));
        }
    }

    @Nested
    @DisplayName("BoundedBuffer")
    class BoundedBufferTest {
        @Test void single_thread_put_take_roundtrip() throws InterruptedException {
            SynchronizationDemo.BoundedBuffer<String> buf =
                new SynchronizationDemo.BoundedBuffer<>(4);
            buf.put("hello");
            buf.put("world");
            assertEquals("hello", buf.take());
            assertEquals("world", buf.take());
        }

        @Test void producer_consumer_concurrent() throws InterruptedException {
            SynchronizationDemo.BoundedBuffer<Integer> buf =
                new SynchronizationDemo.BoundedBuffer<>(8);
            int n = 100;
            List<Integer> consumed = new ArrayList<>();
            CountDownLatch done = new CountDownLatch(1);

            Thread producer = new Thread(() -> {
                try { for (int i = 0; i < n; i++) buf.put(i); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            Thread consumer = new Thread(() -> {
                try {
                    for (int i = 0; i < n; i++) consumed.add(buf.take());
                    done.countDown();
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            producer.start(); consumer.start();
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertEquals(n, consumed.size());
        }
    }

    @Nested
    @DisplayName("CachedData ReadWriteLock")
    class CachedDataTest {
        @Test void put_and_get_returns_value() {
            SynchronizationDemo.CachedData cache = new SynchronizationDemo.CachedData();
            cache.put("key", "value");
            assertEquals("value", cache.get("key"));
        }

        @Test void concurrent_reads_do_not_block_each_other() throws InterruptedException {
            SynchronizationDemo.CachedData cache = new SynchronizationDemo.CachedData();
            cache.put("k", "v");
            CountDownLatch latch = new CountDownLatch(10);
            for (int i = 0; i < 10; i++) {
                new Thread(() -> {
                    assertEquals("v", cache.get("k"));
                    latch.countDown();
                }).start();
            }
            assertTrue(latch.await(2, TimeUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("StampedLock Point")
    class StampedLockTest {
        @Test void move_updates_coordinates() {
            SynchronizationDemo.Point p = new SynchronizationDemo.Point();
            p.move(3.0, 4.0);
            assertEquals(5.0, p.distanceFromOrigin(), 0.001);
        }

        @Test void distance_at_origin_is_zero() {
            SynchronizationDemo.Point p = new SynchronizationDemo.Point();
            assertEquals(0.0, p.distanceFromOrigin(), 0.0001);
        }

        @Test void concurrent_moves_and_reads_do_not_throw() throws InterruptedException {
            SynchronizationDemo.Point p = new SynchronizationDemo.Point();
            CountDownLatch done = new CountDownLatch(20);
            for (int i = 0; i < 10; i++) {
                new Thread(() -> { p.move(1, 1); done.countDown(); }).start();
                new Thread(() -> { p.distanceFromOrigin(); done.countDown(); }).start();
            }
            assertTrue(done.await(2, TimeUnit.SECONDS));
        }
    }
}
