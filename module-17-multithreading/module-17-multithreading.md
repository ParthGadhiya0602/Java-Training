---
title: "17 — Multithreading"
parent: "Phase 2 — Core APIs"
nav_order: 17
render_with_liquid: false
---

# Module 17 — Multithreading & Thread Management
{: .no_toc }

**Goal:** Write correct, safe concurrent code. Understand the thread lifecycle, synchronization tools, thread pools, and Java 21 virtual threads.

---

## Table of Contents
{: .no_toc .text-delta }
1. TOC
{:toc}

---

## Thread Lifecycle

```
NEW ──start()──► RUNNABLE ──► BLOCKED   (waiting for monitor)
                     │
                     ├──► WAITING       (join/wait/park with no timeout)
                     ├──► TIMED_WAITING (sleep/join(ms)/wait(ms))
                     └──► TERMINATED    (run() returned)
```

## Creating Threads

```java
// 1. Runnable lambda (preferred for fire-and-forget)
Thread t = new Thread(() -> doWork());
t.start();

// 2. Builder API (Java 19+)
Thread t = Thread.ofPlatform().name("worker").daemon(true).start(task);

// 3. Virtual thread (Java 21)
Thread vt = Thread.ofVirtual().name("vt").start(task);
```

Never extend `Thread` — it couples task logic to thread management.

## Coordination

### join

```java
thread.join();           // wait forever
thread.join(1000);       // wait up to 1 second; check isAlive() after
```

### Interruption — cooperative cancellation

```java
// Correct pattern: poll the flag, restore on exception
while (!Thread.currentThread().isInterrupted()) {
    doWork();
    Thread.sleep(10);   // throws InterruptedException and CLEARS the flag
}
// After InterruptedException:
Thread.currentThread().interrupt();  // restore the flag for callers
```

**Never swallow `InterruptedException`** without restoring the flag.

### CountDownLatch

```java
CountDownLatch latch = new CountDownLatch(3);
// 3 workers each call latch.countDown() when done
latch.await();     // coordinator waits here
```

Count-down only — cannot reset. Use `CyclicBarrier` for reusable barriers.

### ThreadLocal

```java
static final ThreadLocal<String> USER = ThreadLocal.withInitial(() -> "anonymous");
USER.set("alice");   // only visible to current thread
USER.get();          // "alice"
USER.remove();       // ALWAYS remove in thread pools — reused threads carry stale state
```

---

## Synchronization

### volatile — visibility without atomicity

```java
private volatile boolean stopped = false;
// Guarantees: writes visible immediately; no reordering across access
// NOT sufficient for: i++ (read-modify-write is three operations)
```

### synchronized — mutual exclusion + visibility

```java
public synchronized void increment() { count++; }   // locks 'this'

synchronized (lock) { count++; }   // locks explicit object
```

### AtomicInteger — lock-free CAS

```java
AtomicInteger n = new AtomicInteger();
n.incrementAndGet();              // atomic i++
n.compareAndSet(expected, update); // CAS — basis of all lock-free algorithms
```

Use `AtomicInteger`, `AtomicLong`, `AtomicReference` for single-variable updates. Avoid boxing overhead of `synchronized` for simple counters.

### ReentrantLock

```java
Lock lock = new ReentrantLock();
lock.lock();
try {
    // critical section
} finally {
    lock.unlock();  // always in finally
}

// Non-blocking attempt
if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
    try { ... } finally { lock.unlock(); }
}
```

Adds over `synchronized`: `tryLock`, timed lock, interruptible lock, `Condition` variables, fairness option.

### ReadWriteLock

```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();
// Many readers concurrently:
rwLock.readLock().lock();
try { return cache.get(key); } finally { rwLock.readLock().unlock(); }

// One exclusive writer:
rwLock.writeLock().lock();
try { cache.put(key, value); } finally { rwLock.writeLock().unlock(); }
```

### StampedLock — optimistic reads

```java
StampedLock sl = new StampedLock();
long stamp = sl.tryOptimisticRead();
double x = this.x, y = this.y;
if (!sl.validate(stamp)) {        // writer intervened? fall back
    stamp = sl.readLock();
    try { x = this.x; y = this.y; } finally { sl.unlockRead(stamp); }
}
```

---

## ExecutorService

```java
// Fixed pool — n threads, unbounded queue
ExecutorService exec = Executors.newFixedThreadPool(n);

// Callable + Future — get return value or exception
Future<Integer> f = exec.submit(() -> expensiveCompute());
Integer result = f.get();              // blocks
Integer result = f.get(1, SECONDS);    // blocks with timeout

// Shutdown
exec.shutdown();          // accept no new tasks, drain queue
exec.awaitTermination(5, SECONDS);
```

### invokeAll / invokeAny

```java
List<Future<R>> futures = exec.invokeAll(tasks);  // all complete (or timeout)
R first = exec.invokeAny(tasks);                  // fastest succeeds, rest cancelled
```

### Bounded ThreadPoolExecutor (production)

```java
new ThreadPoolExecutor(
    coreSize, maxSize,
    60L, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(1000),         // bounded queue — prevents OOM
    new ThreadPoolExecutor.CallerRunsPolicy() // slow producer instead of drop
);
```

### CompletableFuture

```java
CompletableFuture.supplyAsync(() -> fetch())
    .thenApply(data -> transform(data))
    .thenAccept(result -> persist(result))
    .exceptionally(ex -> { log(ex); return null; });

// Combine two independent futures
CompletableFuture.allOf(fa, fb).thenApply(v -> List.of(fa.join(), fb.join()));
```

---

## Virtual Threads (Java 21)

```java
// Create
Thread vt = Thread.ofVirtual().name("vt").start(task);

// Executor: one virtual thread per task — no pool sizing needed
ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
```

| | Platform threads | Virtual threads |
|---|---|---|
| Managed by | OS kernel | JVM |
| Memory | ~1 MB stack | ~few KB |
| Blocking cost | Blocks OS thread | Unmounts from carrier |
| Typical count | Hundreds–thousands | Millions |
| Best for | CPU-bound | I/O-bound |

**Pinning** — a virtual thread is pinned when it calls a blocking operation inside `synchronized`. Replace `synchronized` with `ReentrantLock` in hot I/O paths to avoid this.

---

## Source Files

| File | What it covers |
|---|---|
| `ThreadBasics.java` | Creation, join, interruption, CountDownLatch, ThreadLocal |
| `SynchronizationDemo.java` | volatile, synchronized, AtomicInteger, ReentrantLock, ReadWriteLock, StampedLock |
| `ExecutorDemo.java` | ExecutorService, invokeAll/invokeAny, timed Future, ThreadPoolExecutor, ScheduledExecutor, CompletableFuture |
| `VirtualThreadsDemo.java` | Virtual thread creation, scale test, fan-out with virtual executor, thread-per-request pattern |

---

## Common Mistakes

{: .warning }
> **Never swallow `InterruptedException`.** Either propagate it (`throws`) or restore the flag: `Thread.currentThread().interrupt()`.

{: .warning }
> **Always unlock in `finally`.** A `ReentrantLock` not released in `finally` will deadlock any thread that subsequently tries to acquire it.

{: .warning }
> **`volatile` does not make compound operations atomic.** `volatile int i; i++` is still a race. Use `AtomicInteger` or `synchronized`.

{: .tip }
> **Virtual threads for I/O, platform threads for CPU.** A CPU-bound task on a virtual thread still occupies a carrier thread and blocks other virtual threads on that carrier. Use `ForkJoinPool` or a bounded fixed pool for CPU work.
