package com.javatraining.concurrency;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Module 18 - Fork/Join Framework
 *
 * ForkJoinPool is designed for divide-and-conquer parallelism:
 *   fork()  - schedule a subtask asynchronously
 *   join()  - wait for a subtask result
 *   invoke()- fork + join in one call
 *
 * Work-stealing: idle threads steal tasks from the tails of busy
 * threads' deques, keeping all CPUs busy without coordination overhead.
 *
 * RecursiveTask<V>   - returns a value (like Callable)
 * RecursiveAction    - void (like Runnable)
 *
 * Threshold: split until problem size <= threshold, then solve sequentially.
 * Too small a threshold → too much overhead from task creation.
 * Too large → poor parallelism.  A threshold of ~1000 elements is typical.
 *
 * ForkJoinPool.commonPool() is shared by parallel streams and CompletableFuture.
 * For user tasks use a dedicated pool to avoid starving platform code.
 */
public class ForkJoinDemo {

    private static final int THRESHOLD = 512;

    // ── Parallel sum (RecursiveTask) ──────────────────────────────────────────

    public static class SumTask extends RecursiveTask<Long> {
        private final long[] array;
        private final int from, to;

        public SumTask(long[] array, int from, int to) {
            this.array = array; this.from = from; this.to = to;
        }

        @Override
        protected Long compute() {
            int size = to - from;
            if (size <= THRESHOLD) {
                // Base case: sequential sum
                long sum = 0;
                for (int i = from; i < to; i++) sum += array[i];
                return sum;
            }
            int mid = from + size / 2;
            SumTask left  = new SumTask(array, from, mid);
            SumTask right = new SumTask(array, mid,  to);
            left.fork();                    // schedule left asynchronously
            long rightResult = right.compute(); // compute right on this thread
            return left.join() + rightResult;   // wait for left
        }
    }

    public static long parallelSum(long[] array) {
        ForkJoinPool pool = new ForkJoinPool();
        try {
            return pool.invoke(new SumTask(array, 0, array.length));
        } finally {
            pool.shutdown();
        }
    }

    // ── Parallel max (RecursiveTask) ──────────────────────────────────────────

    public static class MaxTask extends RecursiveTask<Long> {
        private final long[] array;
        private final int from, to;

        public MaxTask(long[] array, int from, int to) {
            this.array = array; this.from = from; this.to = to;
        }

        @Override
        protected Long compute() {
            int size = to - from;
            if (size <= THRESHOLD) {
                long max = array[from];
                for (int i = from + 1; i < to; i++) if (array[i] > max) max = array[i];
                return max;
            }
            int mid = from + size / 2;
            MaxTask left  = new MaxTask(array, from, mid);
            MaxTask right = new MaxTask(array, mid,  to);
            invokeAll(left, right);                   // fork both, wait for both
            return Math.max(left.join(), right.join());
        }
    }

    public static long parallelMax(long[] array) {
        ForkJoinPool pool = new ForkJoinPool();
        try {
            return pool.invoke(new MaxTask(array, 0, array.length));
        } finally {
            pool.shutdown();
        }
    }

    // ── Parallel sort (RecursiveAction) ──────────────────────────────────────

    /**
     * Parallel merge sort using RecursiveAction (no return value).
     * Uses a temporary array to merge, writing results back in-place.
     */
    public static class MergeSortAction extends RecursiveAction {
        private final int[] array;
        private final int from, to;

        public MergeSortAction(int[] array, int from, int to) {
            this.array = array; this.from = from; this.to = to;
        }

        @Override
        protected void compute() {
            int size = to - from;
            if (size <= THRESHOLD) {
                Arrays.sort(array, from, to);
                return;
            }
            int mid = from + size / 2;
            MergeSortAction left  = new MergeSortAction(array, from, mid);
            MergeSortAction right = new MergeSortAction(array, mid,  to);
            invokeAll(left, right);
            merge(array, from, mid, to);
        }

        private static void merge(int[] a, int lo, int mid, int hi) {
            int[] tmp = Arrays.copyOfRange(a, lo, hi);
            int left = 0, right = mid - lo, out = lo;
            while (left < mid - lo && right < hi - lo) {
                a[out++] = tmp[left] <= tmp[right] ? tmp[left++] : tmp[right++];
            }
            while (left  < mid - lo) a[out++] = tmp[left++];
            while (right < hi  - lo) a[out++] = tmp[right++];
        }
    }

    public static int[] parallelSort(int[] array) {
        int[] copy = array.clone();
        ForkJoinPool pool = new ForkJoinPool();
        try {
            pool.invoke(new MergeSortAction(copy, 0, copy.length));
        } finally {
            pool.shutdown();
        }
        return copy;
    }

    // ── Fibonacci (RecursiveTask - classic teaching example) ─────────────────

    /**
     * Naive parallel Fibonacci - each subproblem below threshold is solved
     * sequentially to avoid exponential task explosion.
     * NOT efficient in practice (memoization is better), but illustrates fork/join.
     */
    public static class FibTask extends RecursiveTask<Long> {
        private static final int SEQ_THRESHOLD = 10;
        private final int n;

        public FibTask(int n) { this.n = n; }

        @Override
        protected Long compute() {
            if (n <= SEQ_THRESHOLD) return seqFib(n);
            FibTask f1 = new FibTask(n - 1);
            FibTask f2 = new FibTask(n - 2);
            f1.fork();
            return f2.compute() + f1.join();
        }

        private static long seqFib(int n) {
            if (n <= 1) return n;
            long a = 0, b = 1;
            for (int i = 2; i <= n; i++) { long c = a + b; a = b; b = c; }
            return b;
        }
    }

    public static long parallelFib(int n) {
        ForkJoinPool pool = new ForkJoinPool();
        try {
            return pool.invoke(new FibTask(n));
        } finally {
            pool.shutdown();
        }
    }

    // ── Async tasks via ForkJoinPool.submit ───────────────────────────────────

    /**
     * ForkJoinPool.submit() returns a ForkJoinTask (subtype of Future).
     * Useful for mixing FJ tasks with regular Future-based code.
     */
    public static long asyncSum(long[] array) throws Exception {
        ForkJoinPool pool = new ForkJoinPool();
        try {
            ForkJoinTask<Long> task = pool.submit(new SumTask(array, 0, array.length));
            return task.get();
        } finally {
            pool.shutdown();
        }
    }

    // ── Work-stealing demo ────────────────────────────────────────────────────

    /**
     * Measure pool parallelism and queue stats after a computation.
     * Steal count grows when work is unevenly distributed.
     */
    public static Map<String, Long> poolStats(int parallelism) throws Exception {
        ForkJoinPool pool = new ForkJoinPool(parallelism);
        long[] data = new long[1_000_000];
        Arrays.fill(data, 1L);
        try {
            pool.invoke(new SumTask(data, 0, data.length));
            return Map.of(
                "parallelism",  (long) pool.getParallelism(),
                "poolSize",     (long) pool.getPoolSize(),
                "stealCount",   pool.getStealCount()
            );
        } finally {
            pool.shutdown();
        }
    }
}
