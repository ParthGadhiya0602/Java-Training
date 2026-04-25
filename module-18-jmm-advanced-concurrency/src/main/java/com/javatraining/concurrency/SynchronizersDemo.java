package com.javatraining.concurrency;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Module 18 - Advanced Synchronizers
 *
 * java.util.concurrent ships synchronizers for common coordination patterns:
 *
 *   Semaphore       - controls the number of concurrent accessors (permits)
 *   CyclicBarrier   - N threads wait at a barrier, then all proceed together (reusable)
 *   Phaser          - flexible multi-phase barrier; threads can register/deregister
 *   Exchanger       - two threads swap an object at a meeting point
 */
public class SynchronizersDemo {

    // ── Semaphore ─────────────────────────────────────────────────────────────

    /**
     * Semaphore(n): at most n threads may be inside the guarded region.
     *   acquire() - take a permit (blocks if none available)
     *   release() - return a permit
     *
     * Common uses:
     *   n=1   → binary semaphore (mutex, but not re-entrant)
     *   n>1   → rate limiting, connection pool, throttling
     */
    public static class ConnectionPool {
        private final Semaphore semaphore;
        private final Queue<String> connections;

        public ConnectionPool(int size) {
            semaphore = new Semaphore(size, true); // fair=true: FIFO ordering
            connections = new ConcurrentLinkedQueue<>();
            for (int i = 0; i < size; i++) connections.add("conn-" + i);
        }

        public String acquire() throws InterruptedException {
            semaphore.acquire();
            return connections.poll();
        }

        public boolean tryAcquire(long timeoutMs) throws InterruptedException {
            return semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
        }

        public void release(String conn) {
            connections.add(conn);
            semaphore.release();
        }

        public int availablePermits() { return semaphore.availablePermits(); }
    }

    /** Rate limiter: allow at most maxConcurrent tasks running at once. */
    public static List<Long> throttledExecution(int taskCount, int maxConcurrent)
            throws InterruptedException {
        Semaphore sem = new Semaphore(maxConcurrent);
        List<Long> startTimes = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(taskCount);
        AtomicInteger peak = new AtomicInteger();
        AtomicInteger active = new AtomicInteger();

        for (int i = 0; i < taskCount; i++) {
            new Thread(() -> {
                try {
                    sem.acquire();
                    int curr = active.incrementAndGet();
                    peak.accumulateAndGet(curr, Math::max);
                    startTimes.add(System.currentTimeMillis());
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    active.decrementAndGet();
                    sem.release();
                    done.countDown();
                }
            }).start();
        }
        done.await();
        return startTimes;
    }

    // ── CyclicBarrier ─────────────────────────────────────────────────────────

    /**
     * CyclicBarrier(n): N threads all call await(); when the N-th arrives,
     * all are released simultaneously.  The optional Runnable runs in the last
     * thread before release.  "Cyclic" because it resets automatically for reuse.
     */
    public static List<Integer> parallelPhases(int workers, int phases)
            throws InterruptedException, BrokenBarrierException {
        List<Integer> completedPhases = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger phaseCounter = new AtomicInteger(0);

        CyclicBarrier barrier = new CyclicBarrier(workers, () ->
            completedPhases.add(phaseCounter.incrementAndGet())
        );

        CountDownLatch allDone = new CountDownLatch(workers);
        for (int w = 0; w < workers; w++) {
            new Thread(() -> {
                try {
                    for (int p = 0; p < phases; p++) {
                        // simulate per-phase work
                        Thread.sleep(2);
                        barrier.await();
                    }
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    allDone.countDown();
                }
            }).start();
        }
        allDone.await();
        return completedPhases;
    }

    // ── Phaser ────────────────────────────────────────────────────────────────

    /**
     * Phaser is a more flexible CyclicBarrier:
     *   - Parties can register/deregister dynamically
     *   - arriveAndAwaitAdvance() - arrive and wait for all
     *   - arriveAndDeregister()   - arrive then leave permanently
     *   - onAdvance() hook        - override to stop or transform phases
     */
    public static class PhasedPipeline {
        private final Phaser phaser;
        private final List<String> log = Collections.synchronizedList(new ArrayList<>());

        public PhasedPipeline(int workers) {
            // Register workers + coordinator (this thread)
            phaser = new Phaser(workers + 1) {
                @Override protected boolean onAdvance(int phase, int registeredParties) {
                    log.add("phase-" + phase + "-complete");
                    return phase >= 2; // stop after phase 2 (0-indexed: 3 phases)
                }
            };
        }

        public void startWorker(String name) {
            new Thread(() -> {
                while (!phaser.isTerminated()) {
                    log.add(name + "-p" + phaser.getPhase());
                    phaser.arriveAndAwaitAdvance();
                }
            }).start();
        }

        /** Drive the phaser: coordinator arrives each phase. */
        public void run() throws InterruptedException {
            while (!phaser.isTerminated()) {
                phaser.arriveAndAwaitAdvance();
                Thread.sleep(2);
            }
        }

        public List<String> getLog() { return log; }
        public Phaser getPhaser()    { return phaser; }
    }

    // ── Exchanger ────────────────────────────────────────────────────────────

    /**
     * Exchanger<V>: exactly two threads call exchange(v).
     * Each thread blocks until the other arrives, then they swap their values.
     * Useful for pipeline stages passing data between producer and consumer.
     */
    public static String[] exchangeData(String producerData, String consumerData)
            throws InterruptedException {
        Exchanger<String> exchanger = new Exchanger<>();
        String[] results = new String[2];

        Thread producer = new Thread(() -> {
            try { results[0] = exchanger.exchange(producerData); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread consumer = new Thread(() -> {
            try { results[1] = exchanger.exchange(consumerData); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        // results[0] = what producer received (consumerData)
        // results[1] = what consumer received (producerData)
        return results;
    }

    /** Double-buffer swap via Exchanger - classic pipeline pattern. */
    public static List<String> doubleBufferPipeline(List<String> inputs)
            throws InterruptedException {
        Exchanger<List<String>> exchanger = new Exchanger<>();
        List<String> output = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(1);

        // Producer: fills a buffer then exchanges it for an empty one
        Thread producer = new Thread(() -> {
            try {
                List<String> filled = new ArrayList<>(inputs);
                exchanger.exchange(filled); // swap with consumer
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Consumer: swaps its empty buffer for the filled one
        Thread consumer = new Thread(() -> {
            try {
                List<String> received = exchanger.exchange(new ArrayList<>());
                received.stream()
                        .map(String::toUpperCase)
                        .forEach(output::add);
                done.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        done.await();
        producer.join();
        consumer.join();
        return output;
    }
}
