package com.javatraining.jvm;

import java.lang.management.*;
import java.lang.ref.*;
import java.util.*;

/**
 * Module 24 — JVM Memory & Garbage Collection
 *
 * JVM Memory layout:
 *
 *   Heap (managed by GC)
 *     Young Generation
 *       Eden space       — new objects allocated here
 *       Survivor S0/S1   — objects that survived at least one minor GC
 *     Old (Tenured) Gen  — objects that survived many minor GCs
 *
 *   Non-Heap
 *     Metaspace          — class metadata (replaced PermGen in Java 8)
 *     Code Cache         — JIT-compiled native code
 *     Thread stacks      — one per thread
 *
 * GC algorithms (Java 11–21):
 *   Serial GC     — single-threaded; for small heaps, embedded
 *   Parallel GC   — throughput-optimised; multi-threaded stop-the-world
 *   G1 GC         — default since Java 9; balanced latency/throughput
 *   ZGC           — sub-millisecond pauses; Java 15+ production-ready
 *   Shenandoah    — concurrent compaction; low pause
 *
 * Reference types (strength):
 *   Strong  — normal reference; prevents GC
 *   Soft    — cleared when JVM needs memory (before OOM); good for caches
 *   Weak    — cleared at next GC (no strong refs); WeakHashMap keys
 *   Phantom — cleared after finalisation; used for cleanup actions
 *
 * JVM flags:
 *   -Xms<size>  — initial heap size
 *   -Xmx<size>  — maximum heap size
 *   -Xss<size>  — thread stack size
 *   -XX:+UseG1GC / -XX:+UseZGC / -XX:+UseShenandoahGC
 *   -XX:+PrintGCDetails -Xlog:gc*   — GC logging
 */
public class MemoryDemo {

    // ── Runtime memory stats ──────────────────────────────────────────────────

    public record MemoryStats(long totalHeapBytes,
                               long usedHeapBytes,
                               long freeHeapBytes,
                               long maxHeapBytes) {
        public double usedFraction() {
            return maxHeapBytes > 0 ? (double) usedHeapBytes / maxHeapBytes : 0;
        }
    }

    public static MemoryStats heapStats() {
        Runtime rt = Runtime.getRuntime();
        long total = rt.totalMemory();
        long free  = rt.freeMemory();
        long used  = total - free;
        long max   = rt.maxMemory();
        return new MemoryStats(total, used, free, max);
    }

    // ── MemoryMXBean (Management API) ─────────────────────────────────────────

    public static Map<String, Long> heapUsage() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return Map.of(
            "init",      heap.getInit(),
            "used",      heap.getUsed(),
            "committed", heap.getCommitted(),
            "max",       heap.getMax()
        );
    }

    public static Map<String, Long> nonHeapUsage() {
        MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        return Map.of(
            "used",      nonHeap.getUsed(),
            "committed", nonHeap.getCommitted()
        );
    }

    /** Returns a map of memory pool names to their current usage in bytes. */
    public static Map<String, Long> memoryPoolUsages() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            MemoryUsage u = pool.getUsage();
            if (u != null) result.put(pool.getName(), u.getUsed());
        }
        return result;
    }

    // ── GC monitoring ─────────────────────────────────────────────────────────

    public record GcInfo(String name, long collectionCount, long collectionTimeMs) {}

    /** Returns GC stats for all collectors. */
    public static List<GcInfo> gcStats() {
        List<GcInfo> stats = new ArrayList<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            stats.add(new GcInfo(gc.getName(), gc.getCollectionCount(),
                                 gc.getCollectionTime()));
        }
        return stats;
    }

    /** Returns total number of GC events across all collectors. */
    public static long totalGcCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
            .mapToLong(GarbageCollectorMXBean::getCollectionCount)
            .filter(c -> c >= 0)
            .sum();
    }

    // ── Reference types ───────────────────────────────────────────────────────

    /**
     * SoftReference: JVM clears it before throwing OutOfMemoryError.
     * Good for caches — grows when memory is available, shrinks under pressure.
     */
    public static <T> SoftReference<T> softRef(T value) {
        return new SoftReference<>(value);
    }

    /**
     * WeakReference: cleared at the NEXT garbage collection when no strong refs exist.
     * Used in WeakHashMap — keys don't prevent GC of their entries.
     */
    public static <T> WeakReference<T> weakRef(T value) {
        return new WeakReference<>(value);
    }

    /**
     * PhantomReference: get() always returns null.
     * Used with ReferenceQueue for post-finalisation cleanup.
     * Java 9+ Cleaner is the preferred alternative.
     */
    public static <T> PhantomReference<T> phantomRef(T value, ReferenceQueue<T> queue) {
        return new PhantomReference<>(value, queue);
    }

    /**
     * Simple soft-reference cache: stores value under key; returns null if GC'd.
     */
    public static class SoftCache<K, V> {
        private final Map<K, SoftReference<V>> map = new HashMap<>();

        public void put(K key, V value) {
            map.put(key, new SoftReference<>(value));
        }

        public V get(K key) {
            SoftReference<V> ref = map.get(key);
            return ref != null ? ref.get() : null;  // ref.get() returns null if GC'd
        }

        public boolean containsKey(K key) {
            SoftReference<V> ref = map.get(key);
            return ref != null && ref.get() != null;
        }

        public int size() { return map.size(); }
    }

    /**
     * WeakHashMap demo: entries are eligible for GC when the key has no other
     * strong references.  Keys are stored as WeakReferences internally.
     */
    public static WeakHashMap<Object, String> weakHashMapDemo() {
        WeakHashMap<Object, String> map = new WeakHashMap<>();
        Object key1 = new Object();
        Object key2 = new Object();
        map.put(key1, "value1");
        map.put(key2, "value2");
        return map;
    }

    // ── Object size estimation ────────────────────────────────────────────────

    /**
     * Estimates object size by measuring heap change before/after allocation.
     * Not precise (GC may run, other allocations may occur) but demonstrates
     * the Runtime-based approach used before java.lang.instrument.Instrumentation.
     */
    public static long estimateHeapDelta(Runnable allocator) {
        Runtime rt = Runtime.getRuntime();
        System.gc();
        long before = rt.totalMemory() - rt.freeMemory();
        allocator.run();
        long after = rt.totalMemory() - rt.freeMemory();
        return after - before;
    }

    // ── Finalization alternative: Cleaner ─────────────────────────────────────

    /**
     * java.lang.ref.Cleaner (Java 9+) is the recommended alternative to
     * Object.finalize() (deprecated since Java 9, removed in Java 18).
     *
     * Cleaner runs a Runnable when the registered object becomes phantom-reachable.
     * The Runnable must NOT hold a reference to the object (would prevent GC).
     */
    public static class ManagedResource implements AutoCloseable {
        private static final java.lang.ref.Cleaner CLEANER = java.lang.ref.Cleaner.create();

        private final java.lang.ref.Cleaner.Cleanable cleanable;
        private volatile boolean closed = false;

        // State held separately — the cleaner action must not reference 'this'
        private static class State implements Runnable {
            private final String name;
            volatile boolean cleanerRan = false;
            State(String name) { this.name = name; }
            @Override public void run() { cleanerRan = true; }
        }

        private final State state;

        public ManagedResource(String name) {
            this.state     = new State(name);
            this.cleanable = CLEANER.register(this, state);
        }

        @Override public void close() {
            closed = true;
            cleanable.clean();
        }

        public boolean isClosed()     { return closed; }
        public boolean cleanerRan()   { return state.cleanerRan; }
    }

    // ── Thread and runtime info ───────────────────────────────────────────────

    public static int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static Map<String, Object> runtimeInfo() {
        RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
        return Map.of(
            "vmName",    rt.getVmName(),
            "vmVersion", rt.getVmVersion(),
            "uptimeMs",  rt.getUptime()
        );
    }
}
