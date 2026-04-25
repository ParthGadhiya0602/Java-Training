package com.javatraining.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Module 17 - Thread creation, lifecycle, coordination
 *
 * Thread lifecycle states:
 *   NEW          - created, not yet started
 *   RUNNABLE     - executing or ready to execute on CPU
 *   BLOCKED      - waiting to acquire a monitor lock
 *   WAITING      - waiting indefinitely (join, wait, park)
 *   TIMED_WAITING- waiting with timeout (sleep, join(ms), wait(ms))
 *   TERMINATED   - run() method has returned
 *
 * Three ways to create a thread:
 *   1. Extend Thread - avoid; couples task logic to thread management
 *   2. Implement Runnable - preferred for fire-and-forget tasks
 *   3. Implement Callable - when you need a return value or checked exception
 *
 * Java 21 adds virtual threads (Thread.ofVirtual()) - see VirtualThreadsDemo.
 */
public class ThreadBasics {

    // ── Thread creation ───────────────────────────────────────────────────────

    /** Create and start a thread from a Runnable lambda. */
    public static Thread startThread(Runnable task) {
        Thread t = new Thread(task);
        t.start();
        return t;
    }

    /**
     * Named, daemon thread.
     * Daemon threads are killed automatically when all non-daemon threads finish -
     * useful for background housekeeping but never for work that must complete.
     */
    public static Thread startDaemonThread(String name, Runnable task) {
        Thread t = new Thread(task, name);
        t.setDaemon(true);
        t.start();
        return t;
    }

    /**
     * Thread.Builder API (Java 19+) - cleaner than the constructor soup.
     */
    public static Thread buildAndStart(String name, boolean daemon, Runnable task) {
        return Thread.ofPlatform()
                     .name(name)
                     .daemon(daemon)
                     .start(task);
    }

    // ── join - wait for completion ────────────────────────────────────────────

    /**
     * Fan out N tasks across N threads, then join all of them.
     * Returns the total number of increments - proves all threads completed.
     */
    public static int fanOutAndJoin(int threadCount) throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> counter.incrementAndGet());
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();   // caller blocks until t terminates
        }
        return counter.get();
    }

    /** join with timeout - don't wait forever for misbehaving threads. */
    public static boolean joinWithTimeout(Thread t, long timeoutMs) throws InterruptedException {
        t.join(timeoutMs);
        return !t.isAlive();  // true = thread finished within timeout
    }

    // ── Interruption ─────────────────────────────────────────────────────────

    /**
     * Cooperative cancellation via interruption.
     *
     * interrupt() sets the interrupted flag. Blocking methods (sleep, wait,
     * join) throw InterruptedException and clear the flag when they see it set.
     * Non-blocking code must poll Thread.currentThread().isInterrupted().
     *
     * KEY RULE: never swallow InterruptedException without restoring the flag.
     */
    public static Thread startInterruptibleWorker(List<String> log) {
        Thread t = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    log.add("working");
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                // InterruptedException clears the flag - restore it so callers can see it
                Thread.currentThread().interrupt();
                log.add("interrupted");
            }
        });
        t.start();
        return t;
    }

    // ── CountDownLatch - one-shot barrier ────────────────────────────────────

    /**
     * CountDownLatch lets one or more threads wait until a set of operations
     * completes.  The count can only go down - it cannot be reset.
     *
     * Common patterns:
     *   count=N, N workers count down → coordinator waits for all N
     *   count=1, coordinator counts down → N workers wait for start signal
     */
    public static int parallelSum(int[] values) throws InterruptedException {
        int half = values.length / 2;
        int[] partials = new int[2];
        CountDownLatch latch = new CountDownLatch(2);

        new Thread(() -> {
            for (int i = 0; i < half; i++) partials[0] += values[i];
            latch.countDown();
        }).start();

        new Thread(() -> {
            for (int i = half; i < values.length; i++) partials[1] += values[i];
            latch.countDown();
        }).start();

        latch.await();    // wait until both threads call countDown()
        return partials[0] + partials[1];
    }

    /** Starting pistol pattern: hold N workers at a barrier then release all at once. */
    public static List<Long> startTogether(int workerCount) throws InterruptedException {
        CountDownLatch ready  = new CountDownLatch(workerCount);  // workers signal ready
        CountDownLatch start  = new CountDownLatch(1);            // coordinator fires gun
        CountDownLatch done   = new CountDownLatch(workerCount);  // wait for completion
        List<Long> startTimes = new ArrayList<>();
        Object lock = new Object();

        for (int i = 0; i < workerCount; i++) {
            new Thread(() -> {
                ready.countDown();
                try {
                    start.await();                // wait for gun
                    synchronized (lock) { startTimes.add(System.nanoTime()); }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        ready.await();   // wait until all workers are ready
        start.countDown(); // fire!
        done.await();

        return startTimes;
    }

    // ── Thread-local storage ──────────────────────────────────────────────────

    /**
     * ThreadLocal gives each thread its own independent copy of a value.
     * Classic use cases: per-request security context, per-thread formatters,
     * per-thread DB connections.
     *
     * WARNING: always call remove() when done - especially in thread pools
     * where threads are reused, stale values from a previous task can leak.
     */
    public static class RequestContext {
        private static final ThreadLocal<String> CURRENT_USER =
            ThreadLocal.withInitial(() -> "anonymous");

        public static void  setUser(String user) { CURRENT_USER.set(user); }
        public static String getUser()           { return CURRENT_USER.get(); }
        public static void  clear()              { CURRENT_USER.remove(); }
    }

    /** Demonstrate that each thread sees its own ThreadLocal value. */
    public static List<String> threadLocalDemo(int threadCount) throws InterruptedException {
        List<String> results = new ArrayList<>();
        Object lock = new Object();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final String user = "user-" + i;
            Thread t = new Thread(() -> {
                RequestContext.setUser(user);
                try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                String seen = RequestContext.getUser();
                RequestContext.clear();
                synchronized (lock) { results.add(seen); }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();
        return results;
    }
}
