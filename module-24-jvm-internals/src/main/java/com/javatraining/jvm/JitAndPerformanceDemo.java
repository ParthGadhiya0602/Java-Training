package com.javatraining.jvm;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Module 24 — JIT Compiler & Performance
 *
 * JIT (Just-In-Time) compilation pipeline:
 *   1. Bytecode interpreted by the JVM interpreter (cold code)
 *   2. C1 (client) compiler — fast compilation, limited optimisation
 *      Triggered after ~1,500 invocations (tiered: level 1–3)
 *   3. C2 (server) compiler — aggressive optimisation after profiling
 *      Triggered after ~10,000–15,000 invocations (tiered: level 4)
 *
 * Key JIT optimisations:
 *   Inlining         — replace method call with method body
 *   Loop unrolling   — duplicate loop body to reduce branch overhead
 *   Escape analysis  — allocate on stack if object doesn't escape
 *   Devirtualisation — convert virtual calls to direct calls (monomorphic sites)
 *   Dead code elim.  — remove unreachable branches
 *   Scalar replacement — decompose object fields directly onto stack
 *
 * Measuring performance:
 *   System.nanoTime()  — monotonic clock, high resolution, no wall-clock meaning
 *   System.currentTimeMillis() — wall clock, lower resolution
 *   JMH (Java Microbenchmark Harness) — correct benchmarking tool
 *
 * Common performance pitfalls:
 *   - Benchmarking before JIT warms up (cold code numbers are misleading)
 *   - JIT eliminating the thing you're measuring (dead code)
 *   - Auto-boxing in hot loops (int → Integer → allocation)
 *   - Excessive object allocation (GC pressure)
 *   - Lock contention on shared state
 *   - False sharing (see Module 18)
 */
public class JitAndPerformanceDemo {

    // ── Timing utilities ──────────────────────────────────────────────────────

    public record TimingResult(long elapsedNs, long elapsedMs, Object result) {
        @Override public String toString() {
            return String.format("elapsed=%d ms (%d ns), result=%s", elapsedMs, elapsedNs, result);
        }
    }

    /** Times a Supplier, returns its result and elapsed nanoseconds. */
    public static <T> TimingResult time(Supplier<T> task) {
        long start = System.nanoTime();
        T result = task.get();
        long ns = System.nanoTime() - start;
        return new TimingResult(ns, TimeUnit.NANOSECONDS.toMillis(ns), result);
    }

    /** Times a LongSupplier (avoids boxing). */
    public static TimingResult timeLong(LongSupplier task) {
        long start = System.nanoTime();
        long result = task.getAsLong();
        long ns = System.nanoTime() - start;
        return new TimingResult(ns, TimeUnit.NANOSECONDS.toMillis(ns), result);
    }

    /**
     * Warms up a task by running it warmupRounds times,
     * then returns the median of measureRounds timed runs.
     * Simulates what a proper microbenchmark harness does.
     */
    public static long warmAndMeasureNs(Runnable task, int warmupRounds, int measureRounds) {
        for (int i = 0; i < warmupRounds; i++) task.run();
        long[] samples = new long[measureRounds];
        for (int i = 0; i < measureRounds; i++) {
            long start = System.nanoTime();
            task.run();
            samples[i] = System.nanoTime() - start;
        }
        Arrays.sort(samples);
        return samples[measureRounds / 2];  // median
    }

    // ── JIT warmup demonstration ──────────────────────────────────────────────

    /**
     * Demonstrates the warmup effect: first invocation is slow (interpreted),
     * later invocations are faster (JIT-compiled).
     *
     * Returns an array of elapsed times for each round.
     * NOT a precise benchmark — illustrates the concept.
     */
    public static long[] measureWarmup(int rounds) {
        long[] times = new long[rounds];
        for (int r = 0; r < rounds; r++) {
            long start = System.nanoTime();
            // A loop that JIT will optimise after enough iterations
            long sum = 0;
            for (int i = 0; i < 100_000; i++) sum += i;
            times[r] = System.nanoTime() - start;
            // Consume result to prevent dead-code elimination
            if (sum < 0) throw new AssertionError("sum should be non-negative");
        }
        return times;
    }

    // ── Auto-boxing cost ──────────────────────────────────────────────────────

    /** Sum using primitive int array — no boxing. */
    public static long sumPrimitive(int n) {
        long sum = 0;
        for (int i = 0; i < n; i++) sum += i;
        return sum;
    }

    /** Sum using Integer list — every element is boxed. */
    public static long sumBoxed(List<Integer> list) {
        long sum = 0;
        for (Integer i : list) sum += i;   // unboxing on every iteration
        return sum;
    }

    /** Build an Integer list (causes n allocations). */
    public static List<Integer> buildBoxedList(int n) {
        List<Integer> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(i);   // autoboxing
        return list;
    }

    // ── String concatenation cost ─────────────────────────────────────────────

    /**
     * String + in a loop creates a new String each iteration.
     * O(n²) total allocations. Never do this in production loops.
     */
    public static String concatenateLoop(int n) {
        String s = "";
        for (int i = 0; i < n; i++) s += i;
        return s;
    }

    /** StringBuilder avoids repeated allocation. O(n) total work. */
    public static String concatenateStringBuilder(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(i);
        return sb.toString();
    }

    // ── Escape analysis ───────────────────────────────────────────────────────

    /**
     * If the JIT determines a Point never escapes this method, it may
     * allocate it on the stack (scalar replacement) — zero heap allocation.
     * This is invisible to the programmer but improves throughput significantly.
     */
    public static double computeDistance(int x1, int y1, int x2, int y2) {
        // These Point objects may be optimised away by escape analysis
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // ── Compilation info ──────────────────────────────────────────────────────

    /** Returns the name of the JIT compiler in use (C1, C2, GraalVM, etc.). */
    public static String jitName() {
        return ManagementFactory.getCompilationMXBean() != null
            ? ManagementFactory.getCompilationMXBean().getName()
            : "none";
    }

    /** Returns total time spent in JIT compilation (ms). */
    public static long jitCompilationTimeMs() {
        CompilationMXBean comp = ManagementFactory.getCompilationMXBean();
        return (comp != null && comp.isCompilationTimeMonitoringSupported())
            ? comp.getTotalCompilationTime()
            : -1;
    }

    // ── CPU and thread info ───────────────────────────────────────────────────

    public static Map<String, Object> threadStats() {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        return Map.of(
            "threadCount",        threads.getThreadCount(),
            "peakThreadCount",    threads.getPeakThreadCount(),
            "daemonThreadCount",  threads.getDaemonThreadCount(),
            "totalStarted",       threads.getTotalStartedThreadCount()
        );
    }

    // ── Object allocation rate ────────────────────────────────────────────────

    /**
     * Allocates objects in a loop and measures heap delta.
     * Used to demonstrate allocation rate concepts.
     */
    public static long measureAllocationDelta(int objectCount) {
        Runtime rt = Runtime.getRuntime();
        System.gc();
        long before = rt.totalMemory() - rt.freeMemory();

        // Allocate objects — prevent dead-code elimination by touching them
        int[] sizes = new int[objectCount];
        for (int i = 0; i < objectCount; i++) {
            byte[] arr = new byte[100];
            sizes[i] = arr.length;
        }

        long after = rt.totalMemory() - rt.freeMemory();
        // Use sizes to prevent DCE
        if (sizes[0] < 0) throw new AssertionError();
        return Math.max(0, after - before);
    }

    // ── GC-friendly patterns ──────────────────────────────────────────────────

    /**
     * Object pool: reuse heavy objects instead of allocating on each call.
     * Reduces GC pressure at the cost of added complexity.
     * Only worth it for genuinely expensive-to-create objects.
     */
    public static class SimpleObjectPool<T> {
        private final Deque<T> pool = new ArrayDeque<>();
        private final Supplier<T> factory;
        private int created = 0;

        public SimpleObjectPool(Supplier<T> factory) { this.factory = factory; }

        public T acquire() {
            T obj = pool.pollFirst();
            if (obj == null) { created++; return factory.get(); }
            return obj;
        }

        public void release(T obj) { pool.addFirst(obj); }

        public int poolSize()  { return pool.size(); }
        public int createdCount() { return created; }
    }
}
