package com.javatraining.threads;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Module 17 - Virtual Threads (Java 21)
 *
 * Virtual threads are lightweight threads managed by the JVM, not the OS.
 * Key properties:
 *   - Cheap to create: millions fit in memory (vs ~1k platform threads)
 *   - Cheap to block: a blocking virtual thread unmounts from its carrier
 *     thread, freeing the carrier to run another virtual thread
 *   - No pool needed: create one per task - the JVM multiplexes onto
 *     a small pool of carrier (platform) threads
 *   - Same API: Thread, synchronized, locks, Thread.sleep all work
 *
 * When to use virtual threads:
 *   YES: I/O-bound tasks - HTTP calls, DB queries, file I/O
 *   NO:  CPU-bound tasks - they still need platform threads
 *   NO:  Tasks that hold locks for a long time (pinning risk)
 *
 * Pinning: a virtual thread is "pinned" to its carrier when inside a
 * synchronized block or native method during a blocking call.
 * Solution: replace synchronized with ReentrantLock in hot paths.
 */
public class VirtualThreadsDemo {

    // ── Creating virtual threads ──────────────────────────────────────────────

    /** Thread.ofVirtual() - builder API for virtual threads. */
    public static Thread startVirtual(String name, Runnable task) {
        return Thread.ofVirtual()
                     .name(name)
                     .start(task);
    }

    /** Executor that creates one virtual thread per submitted task. */
    public static ExecutorService virtualExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    // ── Scale test: many concurrent tasks ────────────────────────────────────

    /**
     * Spawn taskCount virtual threads each sleeping for sleepMs,
     * then count how many actually completed.
     *
     * With platform threads this would exhaust the OS thread limit around 10k.
     * With virtual threads, hundreds of thousands work fine.
     */
    public static int runConcurrent(int taskCount, long sleepMs)
            throws InterruptedException {
        AtomicInteger completed = new AtomicInteger();
        List<Thread> threads = new ArrayList<>(taskCount);

        for (int i = 0; i < taskCount; i++) {
            threads.add(Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(sleepMs);
                    completed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        for (Thread t : threads) t.join();
        return completed.get();
    }

    // ── Virtual threads with ExecutorService ──────────────────────────────────

    /**
     * newVirtualThreadPerTaskExecutor() is the idiomatic way to run
     * many I/O-bound tasks without sizing a thread pool.
     */
    public static List<Integer> computeAll(List<Integer> inputs) throws Exception {
        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> futures = inputs.stream()
                .map(n -> exec.submit(() -> {
                    Thread.sleep(1); // simulate I/O latency
                    return n * n;
                }))
                .collect(Collectors.toList());

            List<Integer> results = new ArrayList<>();
            for (Future<Integer> f : futures) results.add(f.get());
            return results;
        }
    }

    // ── Structured Concurrency (Java 21 preview → finalized later) ────────────

    /**
     * Structured Concurrency (java.util.concurrent.StructuredTaskScope)
     * ensures that child tasks do not outlive their parent scope:
     *   ShutdownOnFailure - cancel all siblings if any task fails
     *   ShutdownOnSuccess - cancel all siblings when first task succeeds
     *
     * This demo uses plain fork/join to show the pattern without the
     * preview API, which requires --enable-preview flag.
     */
    public static record SearchResult(String source, String value) {}

    /**
     * Fan-out: run tasks concurrently and collect all results.
     * Simulates StructuredTaskScope.ShutdownOnFailure semantics.
     */
    public static List<SearchResult> fanOutSearch(List<String> sources)
            throws InterruptedException, ExecutionException {
        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<SearchResult>> futures = sources.stream()
                .map(src -> exec.submit(() -> {
                    Thread.sleep(1);  // simulate network call
                    return new SearchResult(src, src.toUpperCase());
                }))
                .collect(Collectors.toList());

            List<SearchResult> results = new ArrayList<>();
            for (Future<SearchResult> f : futures) results.add(f.get());
            return results;
        }
    }

    // ── Comparing virtual vs platform thread cost ─────────────────────────────

    /**
     * Measure time to spawn, run, and join N threads.
     * Returns elapsed milliseconds.
     */
    public static long measureSpawnTime(int count, boolean virtual) throws InterruptedException {
        long start = System.currentTimeMillis();
        List<Thread> threads = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Thread t = virtual
                ? Thread.ofVirtual().unstarted(() -> {})
                : Thread.ofPlatform().daemon(true).unstarted(() -> {});
            threads.add(t);
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        return System.currentTimeMillis() - start;
    }

    // ── Thread-per-request server pattern ────────────────────────────────────

    /**
     * Classic server pattern made viable again with virtual threads:
     * one thread per connection - simple, readable, no callback hell.
     *
     * Previously this required async/reactive frameworks because platform
     * threads were too expensive.  Virtual threads restore simple blocking code.
     */
    public static class SimpleRequestHandler {
        private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        private final AtomicInteger handled = new AtomicInteger();

        public Future<String> handle(String request) {
            return executor.submit(() -> {
                Thread.sleep(1);  // simulate DB / network I/O
                handled.incrementAndGet();
                return "response:" + request.toUpperCase();
            });
        }

        public int getHandledCount() { return handled.get(); }

        public void shutdown() { executor.shutdown(); }
    }
}
