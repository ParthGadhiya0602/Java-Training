package com.javatraining.threads;

import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;
import java.util.*;

/**
 * Module 17 - Synchronization, Locks, and Visibility
 *
 * Memory visibility problems:
 *   Without synchronization, a thread may read a stale cached value.
 *   The Java Memory Model (JMM) defines happens-before relationships
 *   that guarantee visibility across threads.
 *
 * Happens-before edges:
 *   monitor unlock → subsequent lock on same monitor
 *   volatile write → subsequent volatile read of same variable
 *   Thread.start() → all actions in the started thread
 *   thread termination → Thread.join() in the joining thread
 *
 * Tools in order of overhead and flexibility:
 *   volatile       - visibility only; no atomicity for compound ops
 *   synchronized   - mutual exclusion + visibility; intrinsic lock
 *   AtomicXxx      - lock-free CAS; fast for single-variable updates
 *   ReentrantLock  - explicit lock; supports tryLock, fairness, Condition
 *   ReadWriteLock  - multiple concurrent readers OR one exclusive writer
 *   StampedLock    - optimistic reads; highest throughput for read-heavy
 */
public class SynchronizationDemo {

    // ── volatile - visibility without atomicity ───────────────────────────────

    /**
     * volatile guarantees:
     *   1. Every write is immediately visible to all threads (no CPU cache)
     *   2. Writes and reads are not reordered across the volatile access
     *
     * NOT sufficient for: check-then-act, read-modify-write (i++ is 3 ops)
     */
    public static class StopFlag {
        private volatile boolean stopped = false;

        public void stop()        { stopped = true; }
        public boolean isStopped(){ return stopped; }

        /** Safe worker: reads volatile flag on every loop iteration. */
        public Thread startWorker(List<Integer> output) {
            Thread t = new Thread(() -> {
                int count = 0;
                while (!stopped) { count++; }
                output.add(count);
            });
            t.start();
            return t;
        }
    }

    // ── synchronized - intrinsic lock ────────────────────────────────────────

    /**
     * synchronized(lock) acquires the intrinsic monitor.
     * Only one thread holds the monitor at a time.
     * Entering synchronized block establishes a happens-before with the
     * thread that last released the same monitor.
     */
    public static class SafeCounter {
        private int count = 0;

        public synchronized void increment() { count++; }
        public synchronized void add(int n)  { count += n; }
        public synchronized int  get()       { return count; }

        /** synchronized on 'this' - same as the method-level keyword */
        public void reset() {
            synchronized (this) { count = 0; }
        }
    }

    /** Demonstrate a data race: unsynchronized counter loses increments. */
    public static int unsafeIncrement(int threadCount, int incrementsPerThread)
            throws InterruptedException {
        int[] count = {0};
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) count[0]++; // RACE
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();
        return count[0];  // likely < threadCount * incrementsPerThread
    }

    // ── AtomicInteger - lock-free CAS ─────────────────────────────────────────

    /**
     * AtomicInteger uses CPU compare-and-swap (CAS) instructions.
     * No blocking, no context switches - fastest for single-variable counters.
     */
    public static int atomicIncrement(int threadCount, int incrementsPerThread)
            throws InterruptedException {
        AtomicInteger count = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) count.incrementAndGet();
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();
        return count.get();  // always == threadCount * incrementsPerThread
    }

    /** compareAndSet: atomic conditional update - the basis of all lock-free algorithms. */
    public static boolean trySetMax(AtomicInteger ref, int candidate) {
        int current;
        do {
            current = ref.get();
            if (candidate <= current) return false;
        } while (!ref.compareAndSet(current, candidate));
        return true;
    }

    // ── ReentrantLock ─────────────────────────────────────────────────────────

    /**
     * ReentrantLock mirrors synchronized but adds:
     *   tryLock()           - non-blocking attempt; avoid deadlocks
     *   tryLock(time, unit) - timed attempt
     *   lockInterruptibly() - can be interrupted while waiting
     *   fair=true           - FIFO ordering (lower throughput, no starvation)
     *
     * Always unlock in finally - never rely on GC to release a lock.
     */
    public static class BoundedBuffer<T> {
        private final Lock lock = new ReentrantLock();
        private final Condition notFull  = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();
        private final Object[]  items;
        private int head, tail, count;

        public BoundedBuffer(int capacity) {
            items = new Object[capacity];
        }

        public void put(T item) throws InterruptedException {
            lock.lock();
            try {
                while (count == items.length) notFull.await();
                items[tail] = item;
                tail = (tail + 1) % items.length;
                count++;
                notEmpty.signal();
            } finally {
                lock.unlock();  // always in finally
            }
        }

        @SuppressWarnings("unchecked")
        public T take() throws InterruptedException {
            lock.lock();
            try {
                while (count == 0) notEmpty.await();
                T item = (T) items[head];
                items[head] = null;
                head = (head + 1) % items.length;
                count--;
                notFull.signal();
                return item;
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            lock.lock();
            try { return count; }
            finally { lock.unlock(); }
        }
    }

    // ── ReadWriteLock ─────────────────────────────────────────────────────────

    /**
     * ReadWriteLock: multiple threads can hold the read lock simultaneously,
     * but the write lock is exclusive.  Ideal when reads vastly outnumber writes.
     */
    public static class CachedData {
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final Map<String, String> cache = new HashMap<>();

        public String get(String key) {
            rwLock.readLock().lock();
            try {
                return cache.get(key);
            } finally {
                rwLock.readLock().unlock();
            }
        }

        public void put(String key, String value) {
            rwLock.writeLock().lock();
            try {
                cache.put(key, value);
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        public int size() {
            rwLock.readLock().lock();
            try { return cache.size(); }
            finally { rwLock.readLock().unlock(); }
        }
    }

    // ── StampedLock - optimistic reads ────────────────────────────────────────

    /**
     * StampedLock adds an optimistic read mode:
     *   1. Take optimistic read stamp (no actual lock acquired)
     *   2. Read data
     *   3. validate(stamp) - if true, no writer intervened; data is safe
     *   4. If false, fall back to a real read lock
     *
     * Fastest for read-heavy workloads with rare writes.
     * NOT reentrant - do not call from code that already holds the stamp.
     */
    public static class Point {
        private final StampedLock sl = new StampedLock();
        private double x, y;

        public void move(double dx, double dy) {
            long stamp = sl.writeLock();
            try { x += dx; y += dy; }
            finally { sl.unlockWrite(stamp); }
        }

        public double distanceFromOrigin() {
            long stamp = sl.tryOptimisticRead();
            double cx = x, cy = y;
            if (!sl.validate(stamp)) {            // writer intervened
                stamp = sl.readLock();            // fall back to real read lock
                try { cx = x; cy = y; }
                finally { sl.unlockRead(stamp); }
            }
            return Math.sqrt(cx * cx + cy * cy);
        }

        public double getX() { return x; }
        public double getY() { return y; }
    }
}
