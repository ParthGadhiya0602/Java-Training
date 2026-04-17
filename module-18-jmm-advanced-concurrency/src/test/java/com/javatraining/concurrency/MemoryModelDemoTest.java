package com.javatraining.concurrency;

import org.junit.jupiter.api.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MemoryModelDemo")
class MemoryModelDemoTest {

    @Nested
    @DisplayName("VolatileHolder")
    class Volatile {
        @Test void set_and_get_returns_value() {
            MemoryModelDemo.VolatileHolder h = new MemoryModelDemo.VolatileHolder();
            h.set("hello");
            assertEquals("hello", h.get());
        }

        @Test void initial_value_is_null() {
            assertEquals(null, new MemoryModelDemo.VolatileHolder().get());
        }
    }

    @Nested
    @DisplayName("ImmutablePoint (final fields)")
    class Immutable {
        @Test void fields_accessible() {
            MemoryModelDemo.ImmutablePoint p = new MemoryModelDemo.ImmutablePoint(3, 4);
            assertEquals(3, p.x);
            assertEquals(4, p.y);
        }
    }

    @Nested
    @DisplayName("DCL Singleton")
    class DCL {
        @BeforeEach void reset() { MemoryModelDemo.DCLSingleton.reset(); }

        @Test void returns_same_instance_on_repeated_calls() {
            MemoryModelDemo.DCLSingleton a = MemoryModelDemo.DCLSingleton.getInstance("cfg");
            MemoryModelDemo.DCLSingleton b = MemoryModelDemo.DCLSingleton.getInstance("cfg");
            assertSame(a, b);
        }

        @Test void stores_config() {
            assertEquals("prod", MemoryModelDemo.DCLSingleton.getInstance("prod").getConfig());
        }

        @Test void concurrent_access_returns_same_instance() throws InterruptedException {
            int threads = 20;
            MemoryModelDemo.DCLSingleton[] instances =
                new MemoryModelDemo.DCLSingleton[threads];
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done  = new CountDownLatch(threads);

            for (int i = 0; i < threads; i++) {
                final int idx = i;
                new Thread(() -> {
                    try { start.await(); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    instances[idx] = MemoryModelDemo.DCLSingleton.getInstance("cfg");
                    done.countDown();
                }).start();
            }
            start.countDown();
            done.await();
            for (MemoryModelDemo.DCLSingleton inst : instances) {
                assertSame(instances[0], inst);
            }
        }
    }

    @Nested
    @DisplayName("LazyHolder")
    class LazyHolderTest {
        @Test void returns_non_null_instance() {
            assertNotNull(MemoryModelDemo.LazyHolder.getInstance());
        }

        @Test void returns_same_instance_always() {
            assertSame(MemoryModelDemo.LazyHolder.getInstance(),
                       MemoryModelDemo.LazyHolder.getInstance());
        }
    }

    @Nested
    @DisplayName("AtomicConfig (lock-free reference)")
    class AtomicConfigTest {
        @Test void initial_position_is_origin() {
            MemoryModelDemo.AtomicConfig cfg = new MemoryModelDemo.AtomicConfig();
            assertEquals(0, cfg.get().x);
            assertEquals(0, cfg.get().y);
        }

        @Test void shift_updates_position() {
            MemoryModelDemo.AtomicConfig cfg = new MemoryModelDemo.AtomicConfig();
            cfg.shift(3, 4);
            assertEquals(3, cfg.get().x);
            assertEquals(4, cfg.get().y);
        }

        @Test void concurrent_shifts_converge() throws InterruptedException {
            MemoryModelDemo.AtomicConfig cfg = new MemoryModelDemo.AtomicConfig();
            int threads = 10;
            CountDownLatch done = new CountDownLatch(threads);
            for (int i = 0; i < threads; i++) {
                new Thread(() -> {
                    cfg.shift(1, 1);
                    done.countDown();
                }).start();
            }
            done.await();
            assertEquals(threads, cfg.get().x);
            assertEquals(threads, cfg.get().y);
        }
    }

    @Nested
    @DisplayName("PaddedCounter (false-sharing mitigation)")
    class PaddedCounterTest {
        @Test void both_counters_reach_expected_value() throws InterruptedException {
            long[] results = MemoryModelDemo.parallelCountWithPadding(10_000);
            assertEquals(10_000, results[0]);
            assertEquals(10_000, results[1]);
        }
    }
}
