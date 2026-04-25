package com.javatraining.performance.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark comparing virtual threads vs fixed platform thread pool
 * on a simulated I/O workload (Thread.sleep).
 *
 * Run with:
 *   mvn package -DskipTests
 *   java -jar target/performance-0.0.1-SNAPSHOT.jar VirtualThreadBenchmark
 *
 * Or for a quick run:
 *   java -jar target/performance-0.0.1-SNAPSHOT.jar VirtualThreadBenchmark -f 1 -wi 2 -i 3
 *
 * Expected result: virtualThreads throughput >> fixedPool throughput for blocking tasks.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 2)
@State(Scope.Benchmark)
public class VirtualThreadBenchmark {

    @Param({"100", "500"})
    private int taskCount;

    private static final int SLEEP_MS = 50;

    @Benchmark
    public void virtualThreads() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(taskCount);
        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                exec.submit(() -> {
                    Thread.sleep(SLEEP_MS);
                    latch.countDown();
                    return null;
                });
            }
        }
        latch.await();
    }

    @Benchmark
    public void fixedThreadPool() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(taskCount);
        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        try (ExecutorService exec = Executors.newFixedThreadPool(poolSize)) {
            for (int i = 0; i < taskCount; i++) {
                exec.submit(() -> {
                    Thread.sleep(SLEEP_MS);
                    latch.countDown();
                    return null;
                });
            }
        }
        latch.await();
    }
}
