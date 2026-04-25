package com.javatraining.threads;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Module 17 - ExecutorService, Callable, Future, CompletableFuture
 *
 * Raw Thread creation is rarely the right choice in production code.
 * ExecutorService decouples task submission from thread management:
 *   - Thread reuse (pools avoid startup overhead)
 *   - Bounded concurrency
 *   - Orderly shutdown
 *   - Return values and exceptions via Future/Callable
 *
 * Thread pool types (via Executors factory):
 *   newFixedThreadPool(n)       - n threads; unbounded queue
 *   newCachedThreadPool()       - grows/shrinks; 60s idle timeout
 *   newSingleThreadExecutor()   - guaranteed sequential execution
 *   newScheduledThreadPool(n)   - delays and periodic tasks
 *   newVirtualThreadPerTaskExecutor() - Java 21; one virtual thread per task
 *
 * Prefer ThreadPoolExecutor directly in production for explicit queue bounds.
 */
public class ExecutorDemo {

    // ── Basic submit ──────────────────────────────────────────────────────────

    /**
     * submit(Callable) returns a Future.
     * Future.get() blocks until the result is available or throws.
     */
    public static int computeOnThread(int input) throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> future = exec.submit(() -> input * input);
            return future.get();
        } finally {
            exec.shutdown();
        }
    }

    /**
     * submit(Runnable) returns Future<?> - get() returns null but lets
     * you detect completion and propagate exceptions.
     */
    public static void runAndWait(Runnable task) throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            exec.submit(task).get();
        } finally {
            exec.shutdown();
        }
    }

    // ── invokeAll / invokeAny ─────────────────────────────────────────────────

    /**
     * invokeAll: submits all tasks, blocks until every Future is done.
     * Returns results in submission order (not completion order).
     */
    public static List<Integer> invokeAllSquares(List<Integer> inputs) throws Exception {
        if (inputs.isEmpty()) return List.of();
        ExecutorService exec = Executors.newFixedThreadPool(
            Math.min(inputs.size(), Runtime.getRuntime().availableProcessors()));
        try {
            List<Callable<Integer>> tasks = inputs.stream()
                .<Callable<Integer>>map(n -> () -> n * n)
                .collect(Collectors.toList());
            List<Future<Integer>> futures = exec.invokeAll(tasks);
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> f : futures) results.add(f.get());
            return results;
        } finally {
            exec.shutdown();
        }
    }

    /**
     * invokeAny: returns the first successful result, cancels the rest.
     * Useful when multiple strategies can produce the same answer.
     */
    public static String invokeAnyFastest(List<Callable<String>> tasks) throws Exception {
        ExecutorService exec = Executors.newCachedThreadPool();
        try {
            return exec.invokeAny(tasks);
        } finally {
            exec.shutdown();
        }
    }

    // ── Timed Future.get ──────────────────────────────────────────────────────

    /**
     * Future.get(timeout) avoids blocking forever on slow tasks.
     * Returns the result, or throws TimeoutException if not ready in time.
     */
    public static Optional<String> getWithTimeout(Callable<String> task, long timeoutMs)
            throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            Future<String> f = exec.submit(task);
            try {
                return Optional.of(f.get(timeoutMs, TimeUnit.MILLISECONDS));
            } catch (TimeoutException e) {
                f.cancel(true);
                return Optional.empty();
            }
        } finally {
            exec.shutdown();
        }
    }

    // ── ThreadPoolExecutor with explicit bounds ────────────────────────────────

    /**
     * In production prefer explicit ThreadPoolExecutor over Executors factories:
     *   - bounded work queue prevents OOM from task accumulation
     *   - rejection policy is explicit (not silent)
     */
    public static ThreadPoolExecutor buildBoundedPool(int coreSize, int maxSize, int queueCapacity) {
        return new ThreadPoolExecutor(
            coreSize, maxSize,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new ThreadFactory() {
                private final AtomicInteger n = new AtomicInteger();
                @Override public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "worker-" + n.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()  // slow down producer instead of dropping
        );
    }

    // ── ScheduledExecutorService ──────────────────────────────────────────────

    /**
     * scheduleAtFixedRate: next run starts exactly period ms after previous start.
     * scheduleWithFixedDelay: next run starts delay ms after previous finish.
     *
     * Use scheduleWithFixedDelay when each task duration varies and you don't
     * want overlapping executions.
     */
    public static List<Long> scheduleAtRate(int count, long periodMs) throws InterruptedException {
        List<Long> timestamps = Collections.synchronizedList(new ArrayList<>());
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch latch = new CountDownLatch(count);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            timestamps.add(System.currentTimeMillis());
            latch.countDown();
        }, 0, periodMs, TimeUnit.MILLISECONDS);

        latch.await();
        future.cancel(false);
        scheduler.shutdown();
        return timestamps;
    }

    // ── CompletableFuture ─────────────────────────────────────────────────────

    /**
     * CompletableFuture is a Future you can complete manually and chain with
     * callbacks - no blocking needed for simple pipelines.
     *
     * Key methods:
     *   supplyAsync(Supplier)      - run async, produces a value
     *   thenApply(Function)        - transform result when ready
     *   thenAccept(Consumer)       - consume result, returns Void
     *   thenCompose(Function)      - flatMap - chains another async stage
     *   thenCombine(other, BiFunc) - combine two independent futures
     *   exceptionally(Function)    - recover from exception
     *   allOf / anyOf              - wait for N futures
     */
    public static CompletableFuture<String> asyncPipeline(int input) {
        return CompletableFuture
            .supplyAsync(() -> input * input)
            .thenApply(squared -> "result=" + squared)
            .thenApply(String::toUpperCase);
    }

    /** thenCombine: two independent async computations merged. */
    public static CompletableFuture<Integer> combineTwo(int a, int b) {
        CompletableFuture<Integer> fa = CompletableFuture.supplyAsync(() -> a * 2);
        CompletableFuture<Integer> fb = CompletableFuture.supplyAsync(() -> b * 3);
        return fa.thenCombine(fb, Integer::sum);
    }

    /** exceptionally: provide a fallback value when the stage throws. */
    public static CompletableFuture<String> withFallback(Callable<String> task) {
        return CompletableFuture
            .supplyAsync(() -> {
                try { return task.call(); }
                catch (Exception e) { throw new RuntimeException(e); }
            })
            .exceptionally(ex -> "fallback");
    }

    /** allOf: wait for all futures; then collect results. */
    public static CompletableFuture<List<Integer>> allSquares(List<Integer> inputs) {
        List<CompletableFuture<Integer>> futures = inputs.stream()
            .map(n -> CompletableFuture.supplyAsync(() -> n * n))
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
}
