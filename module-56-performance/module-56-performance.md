---
title: "Module 56 — Performance & Profiling"
parent: "Phase 6 — Production & Architecture"
nav_order: 56
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-56-performance/src){: .btn .btn-outline }

# Module 56 — Performance & Profiling

## What this module covers

Virtual threads (Java 21), JMH microbenchmarks, async-profiler CPU/memory profiling,
Java Flight Recorder (JFR) event recording, JVM GC selection and tuning, and how to
load-test with JMeter. The runnable code demonstrates virtual thread behavior; the
profiling and load-testing tools are documented with command references.

---

## Project structure

```
src/main/java/com/javatraining/performance/
├── PerformanceApplication.java
├── task/
│   ├── TaskService.java         # Executors.newVirtualThreadPerTaskExecutor()
│   └── TaskController.java      # POST /tasks (202), GET /tasks/{id}
└── benchmark/
    └── VirtualThreadBenchmark.java  # JMH benchmark (run separately)

src/main/resources/
└── application.properties       # spring.threads.virtual.enabled=true

src/test/java/com/javatraining/performance/
├── VirtualThreadTest.java        # virtual thread behavior, no Spring (2 tests)
└── task/TaskControllerTest.java  # @SpringBootTest + MockMvc (2 tests)
```

---

## Virtual threads

### What changed in Java 21

| | Platform thread | Virtual thread |
|---|---|---|
| Backed by | OS thread (1:1) | JVM scheduler (M:N) |
| Cost per thread | ~1 MB stack, OS kernel object | ~few KB heap object |
| Blocking behavior | Blocks OS thread | Unmounts from carrier — carrier is free |
| Max practical concurrency | ~thousands | ~millions |
| API | `new Thread(...)` | `Thread.ofVirtual()`, `newVirtualThreadPerTaskExecutor()` |

### The key property — carrier unmounting

When a virtual thread calls a blocking operation (`Thread.sleep`, `LockSupport.park`,
blocking I/O, `synchronized` on an uncontended monitor), the JVM **unmounts** it from its
carrier (OS) thread. The carrier immediately picks up another virtual thread. When the
blocking operation completes, the virtual thread is remounted on any available carrier.

This is why 500 virtual threads can all block on a `CountDownLatch` simultaneously,
even if there are only 8 carrier threads (one per CPU core).

### Spring Boot configuration

```properties
# application.properties
spring.threads.virtual.enabled=true
```

This single property switches Tomcat's request-handling pool from platform threads to a
virtual-thread-per-request executor. No application code changes. Every HTTP request,
filter, and interceptor runs on a virtual thread.

### TaskService implementation

```java
private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

public String submit(String payload) {
    String taskId = UUID.randomUUID().toString();
    tasks.put(taskId, TaskStatus.PENDING);
    executor.submit(() -> {
        tasks.put(taskId, TaskStatus.RUNNING);
        Thread.sleep(100);   // unmounts the virtual thread — carrier is free
        tasks.put(taskId, TaskStatus.DONE);
        return null;
    });
    return taskId;
}
```

### Testing virtual thread behavior

```java
@Test
void virtual_threads_support_more_concurrent_blockers_than_carrier_thread_count() {
    int taskCount = 500;
    CountDownLatch allWaiting = new CountDownLatch(taskCount);
    CountDownLatch release    = new CountDownLatch(1);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                allWaiting.countDown();
                release.await();   // blocks — virtual thread unmounts here
                return null;
            });
        }
        // All 500 can block concurrently; a fixed(N) pool with N < 500 would deadlock
        assertThat(allWaiting.await(10, TimeUnit.SECONDS)).isTrue();
        release.countDown();
    }
}
```

This test would deadlock with `Executors.newFixedThreadPool(10)` because the 10 threads
would be exhausted waiting on `release` while new tasks can't be scheduled to decrement
`allWaiting`.

### Pinning — the one gotcha

Virtual threads are **pinned** (cannot unmount) when:
- Inside a `synchronized` block that blocks
- Calling native code that blocks

Pinned virtual threads hold a carrier thread and reduce concurrency back toward platform
thread behavior. The fix is to replace `synchronized` with `ReentrantLock`.

JFR event `jdk.VirtualThreadPinned` captures pinning incidents in production.

---

## JMH — Java Microbenchmark Harness

### Setup

```xml
<dependency>
    <groupId>org.openjdk.jmh</groupId>
    <artifactId>jmh-core</artifactId>
    <version>1.37</version>
</dependency>
<dependency>
    <groupId>org.openjdk.jmh</groupId>
    <artifactId>jmh-generator-annprocess</artifactId>
    <version>1.37</version>
    <scope>provided</scope>
</dependency>
```

`jmh-generator-annprocess` generates synthetic runner classes from `@Benchmark` methods
at compile time. When using `annotationProcessorPaths` in the compiler plugin, all other
processors (e.g., Lombok) must also be listed explicitly — otherwise auto-discovery is
replaced by the explicit list.

### Benchmark class

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 2)
@State(Scope.Benchmark)
public class VirtualThreadBenchmark {

    @Param({"100", "500"})
    private int taskCount;

    @Benchmark
    public void virtualThreads() throws InterruptedException {
        // ... taskCount tasks each sleeping 50ms via virtual thread executor
    }

    @Benchmark
    public void fixedThreadPool() throws InterruptedException {
        // ... same tasks via fixedThreadPool(availableProcessors * 2)
    }
}
```

### Running benchmarks

```bash
# Build fat JAR (JMH embeds its own main class)
mvn package -DskipTests

# Run all benchmarks with default settings
java -jar target/performance-0.0.1-SNAPSHOT.jar

# Quick run (fewer iterations — useful for smoke-testing the setup)
java -jar target/performance-0.0.1-SNAPSHOT.jar VirtualThreadBenchmark -f 1 -wi 2 -i 3

# Expected output (throughput ops/s — higher is better):
# Benchmark                           (taskCount)  Mode  Cnt    Score    Error  Units
# VirtualThreadBenchmark.virtualThreads        100  thrpt   10  187.432 ± 3.21  ops/s
# VirtualThreadBenchmark.fixedThreadPool       100  thrpt   10    7.143 ± 0.08  ops/s
```

### Common mistakes

| Mistake | Effect | Fix |
|---|---|---|
| Too few warm-up iterations | JIT not fully compiled — scores too low | `@Warmup(iterations = 5, time = 2)` |
| `@Fork(0)` or no fork | Shares JVM state with test runner — unreliable | Always use `@Fork(value = 2)` |
| Benchmarking inside `@Test` | Surefire JVM overhead, shared JIT cache | Always use separate JVM via fat JAR |
| Dead-code elimination | JVM optimises away unmeasured results | Use `Blackhole.consume(result)` |

---

## async-profiler

async-profiler is a low-overhead, signal-based CPU and memory profiler that doesn't
require safepoints (unlike JVisualVM).

```bash
# Download
curl -sL https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-macos.zip | tar -xz

# CPU profiling — attach to running JVM
./asprof -d 30 -f flamegraph.html <pid>

# Allocation profiling — track heap allocations
./asprof -e alloc -d 30 -f alloc.html <pid>

# Wall-clock profiling — includes time blocked/sleeping (useful for virtual threads)
./asprof -e wall -d 30 -f wall.html <pid>
```

For virtual thread profiling, use `-e wall` (wall-clock mode). CPU mode only captures
threads actively running on CPU — parked virtual threads won't appear.

### With Spring Boot

```bash
JAVA_OPTS="-agentpath:/path/to/libasyncProfiler.dylib=start,event=cpu,file=flamegraph.html"
java $JAVA_OPTS -jar target/performance-0.0.1-SNAPSHOT.jar
```

---

## Java Flight Recorder (JFR)

JFR is built into the JDK — no agent or license required since Java 11.

```bash
# Start with flight recorder enabled
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr -jar app.jar

# Or attach to a running process
jcmd <pid> JFR.start duration=60s filename=recording.jfr
jcmd <pid> JFR.stop
```

### Useful event types

| Event | What it captures |
|---|---|
| `jdk.VirtualThreadPinned` | Virtual thread pinned to carrier (degraded concurrency) |
| `jdk.GarbageCollection` | GC pauses, type, cause |
| `jdk.ExecutionSample` | CPU sampling (like async-profiler but lower resolution) |
| `jdk.ObjectAllocationInNewTLAB` | Allocation hot spots |
| `jdk.ThreadStart` / `jdk.ThreadEnd` | Thread lifecycle |

Open `recording.jfr` in **JDK Mission Control (JMC)** for visual analysis.

---

## GC tuning

### GC selection

| Collector | Flag | When to use |
|---|---|---|
| G1 (default, JDK 9+) | `-XX:+UseG1GC` | General purpose — balanced pause/throughput |
| ZGC | `-XX:+UseZGC` | Low-latency — sub-millisecond pauses; Java 21+ fully generational |
| Shenandoah | `-XX:+UseShenandoahGC` | Low-latency alternative; concurrent compaction |
| Serial | `-XX:+UseSerialGC` | Single-core or very small heaps (e.g., CLI tools) |

### Key JVM flags

```bash
# Heap sizing
-Xms512m -Xmx2g                       # initial and max heap

# GC logging (essential for diagnosing pause spikes)
-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=20m

# ZGC for low-latency (Java 21+ default generational mode)
-XX:+UseZGC -XX:+ZGenerational

# Container awareness (always set in Docker/Kubernetes)
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0

# Disable biased locking (removed in Java 21, but guard for Java 17 images)
# -XX:-UseBiasedLocking
```

### G1 pause tuning

```bash
# Target max pause time of 100ms (default 200ms)
-XX:MaxGCPauseMillis=100

# Increase concurrent marking threads for large heaps
-XX:ConcGCThreads=4
```

---

## JMeter load testing

```bash
# Run a test plan non-interactively (headless)
jmeter -n -t load-test.jmx -l results.jtl -e -o report/

# Key thread group settings in load-test.jmx:
# - Number of Threads (users): 500
# - Ramp-up Period: 30s
# - Loop Count: 100
```

For virtual thread comparison: run the same JMeter plan against the app with
`spring.threads.virtual.enabled=true` vs `false` and compare throughput (req/s)
and mean response time under high concurrency.

---

## Tests

| Class | Type | Tests |
|---|---|---|
| `VirtualThreadTest` | JUnit 5, no Spring | 2 |
| `TaskControllerTest` | `@SpringBootTest` + MockMvc | 2 |

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test`
Result: **4/4 pass**

---

## Key decisions

| Decision | Reason |
|---|---|
| `CountDownLatch` barrier test over time comparison | Timing tests are flaky on slow CI machines; the barrier test proves the structural property (concurrent blocking beyond carrier count) — not a number |
| JMH benchmarks in `src/main/java`, not `src/test/java` | `jmh-generator-annprocess` generates runner classes into `target/generated-sources/annotations`; test-scope source sets have separate compilation with different annotation processors |
| Explicit `annotationProcessorPaths` with both Lombok and JMH | `annotationProcessorPaths` replaces classpath scanning — omitting Lombok silently breaks `@Slf4j`, `@Data`, etc. |
| `spring.threads.virtual.enabled=true` as the only config | Spring Boot 3.2+ plumbing change; zero application code changes demonstrates the "drop-in" nature of virtual thread adoption |
| Wall-clock profiling (`-e wall`) for virtual threads | CPU profiling misses parked virtual threads; wall-clock shows all threads including those blocked on I/O or locks |
{% endraw %}
