package com.javatraining.concurrency;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Module 18 — Java Memory Model (JMM)
 *
 * The JMM defines which values a read is allowed to see.
 * Without explicit synchronization, the JVM and CPU may:
 *   - Cache writes in registers or store buffers (visibility problem)
 *   - Reorder instructions for performance (reordering problem)
 *
 * Happens-before (HB) relationships guarantee visibility and ordering:
 *   • monitor unlock  HB  subsequent lock on same monitor
 *   • volatile write  HB  subsequent volatile read of same variable
 *   • Thread.start()  HB  all actions in the started thread
 *   • thread death    HB  Thread.join() return in joining thread
 *   • HB is transitive: if A HB B and B HB C, then A HB C
 *
 * This class demonstrates correct and incorrect publication patterns,
 * double-checked locking, and safe immutable object publication.
 */
public class MemoryModelDemo {

    // ── Unsafe publication — the problem ─────────────────────────────────────

    /**
     * BROKEN: without synchronization another thread may see a partially
     * constructed object or an old null value even after set() is called.
     * This is a teaching example — do not use in production.
     */
    public static class UnsafeHolder {
        private Object value;                     // no synchronization
        public void set(Object v) { value = v; }
        public Object get()       { return value; }
    }

    // ── Safe publication patterns ─────────────────────────────────────────────

    /**
     * Pattern 1 — volatile field.
     * Volatile write HB volatile read: any thread that reads a non-null
     * value is guaranteed to see the fully constructed object.
     */
    public static class VolatileHolder {
        private volatile Object value;
        public void set(Object v) { value = v; }
        public Object get()       { return value; }
    }

    /**
     * Pattern 2 — final field.
     * The JMM guarantees that final fields are visible to all threads
     * after the constructor returns — no synchronization needed.
     * Immutable objects published through any mechanism are safe.
     */
    public static final class ImmutablePoint {
        public final int x;
        public final int y;
        public ImmutablePoint(int x, int y) { this.x = x; this.y = y; }
    }

    /**
     * Pattern 3 — static initializer.
     * Class loading is thread-safe: the JVM holds a lock during
     * static initialisation. Useful for singleton-style factories.
     */
    public static class SingletonViaStatic {
        private static final SingletonViaStatic INSTANCE = new SingletonViaStatic();
        private SingletonViaStatic() {}
        public static SingletonViaStatic getInstance() { return INSTANCE; }
    }

    // ── Double-checked locking — correct implementation ───────────────────────

    /**
     * DCL requires volatile on the instance field (Java 5+).
     * Without volatile, the JVM may publish a reference to a partially
     * constructed object because object construction and field assignment
     * can be reordered.
     *
     * volatile write (instance = obj) HB volatile read (instance != null)
     * ensures the fully-constructed object is visible.
     */
    public static class DCLSingleton {
        private volatile static DCLSingleton instance;
        private final String config;

        private DCLSingleton(String config) { this.config = config; }

        public static DCLSingleton getInstance(String config) {
            if (instance == null) {                     // first check (no lock)
                synchronized (DCLSingleton.class) {
                    if (instance == null) {             // second check (with lock)
                        instance = new DCLSingleton(config);
                    }
                }
            }
            return instance;
        }

        public String getConfig() { return config; }

        // Reset for testing only
        static void reset() { instance = null; }
    }

    // ── Initialization-on-demand holder idiom ─────────────────────────────────

    /**
     * Preferred over DCL when the instance needs no parameters.
     * The Holder class is not loaded until getInstance() is first called,
     * so initialisation is both lazy and thread-safe without any locks.
     */
    public static class LazyHolder {
        private LazyHolder() {}

        private static class Holder {
            static final LazyHolder INSTANCE = new LazyHolder();
        }

        public static LazyHolder getInstance() { return Holder.INSTANCE; }
    }

    // ── AtomicReference for lock-free publication ────────────────────────────

    /**
     * AtomicReference<T> provides CAS-based reference updates.
     * compareAndSet is the building block of all lock-free data structures.
     */
    public static class AtomicConfig {
        private final AtomicReference<ImmutablePoint> ref =
            new AtomicReference<>(new ImmutablePoint(0, 0));

        /** Atomically update — retries if another thread raced and won. */
        public void shift(int dx, int dy) {
            ImmutablePoint prev, next;
            do {
                prev = ref.get();
                next = new ImmutablePoint(prev.x + dx, prev.y + dy);
            } while (!ref.compareAndSet(prev, next));
        }

        public ImmutablePoint get() { return ref.get(); }
    }

    // ── False sharing ─────────────────────────────────────────────────────────

    /**
     * False sharing: two independently-modified variables share a CPU cache
     * line (typically 64 bytes).  Every write to one invalidates the other
     * in all other CPU caches, causing unnecessary cache-coherence traffic.
     *
     * Fix: pad or align fields so they occupy separate cache lines.
     * Java 8+ @Contended annotation (in jdk.internal) does this automatically.
     * Here we show the manual padding technique for clarity.
     */
    public static class PaddedCounter {
        // 8 bytes value + 7*8 bytes padding = 64 bytes = one cache line
        public volatile long value;
        public long p1, p2, p3, p4, p5, p6, p7;

        public void increment() { value++; }
        public long get()       { return value; }
    }

    /** Two padded counters updated by separate threads don't share a cache line. */
    public static long[] parallelCountWithPadding(int ops) throws InterruptedException {
        PaddedCounter c1 = new PaddedCounter();
        PaddedCounter c2 = new PaddedCounter();

        Thread t1 = new Thread(() -> { for (int i = 0; i < ops; i++) c1.increment(); });
        Thread t2 = new Thread(() -> { for (int i = 0; i < ops; i++) c2.increment(); });
        t1.start(); t2.start();
        t1.join();  t2.join();

        return new long[]{ c1.get(), c2.get() };
    }
}
