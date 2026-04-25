---
title: "18 - JMM & Advanced Concurrency"
parent: "Phase 2 - Core APIs"
nav_order: 18
render_with_liquid: false
---

{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-18-jmm-advanced-concurrency/src){: .btn .btn-outline }

# Module 18 - Java Memory Model & Advanced Concurrency

{: .no_toc }

**Goal:** Understand the Java Memory Model, safe publication, and the advanced concurrency utilities - synchronizers, concurrent collections, and the Fork/Join framework.

---

## Table of Contents

{: .no_toc .text-delta }

1. TOC
   {:toc}

---

## Java Memory Model (JMM)

The JMM defines which value a thread is allowed to see when it reads a variable. Without explicit synchronisation, CPUs and the JVM may reorder instructions and cache writes - so a thread may see a stale value even after another thread "wrote" the new one.

### Happens-before (HB)

If action A happens-before action B, then all effects of A are visible to B.

| Relationship       | HB edge                                           |
| ------------------ | ------------------------------------------------- |
| `monitor.unlock()` | → next `monitor.lock()` on the same object        |
| `volatile` write   | → subsequent `volatile` read of the same variable |
| `Thread.start()`   | → all actions in the started thread               |
| Thread death       | → `Thread.join()` return in the joining thread    |
| HB is transitive   | A HB B and B HB C ⟹ A HB C                        |

### Safe publication patterns

```java
// 1. volatile field
private volatile Config config;

// 2. final fields - visible to all threads after constructor returns
public final class Point { public final int x, y; ... }

// 3. static initialiser - class loading is thread-safe
private static final Singleton INSTANCE = new Singleton();

// 4. AtomicReference - CAS-based lock-free reference update
AtomicReference<Config> ref = new AtomicReference<>(initial);
ref.compareAndSet(expected, updated);
```

### Double-checked locking - requires volatile

```java
private volatile static DCLSingleton instance;

public static DCLSingleton getInstance() {
    if (instance == null) {                     // first check (no lock)
        synchronized (DCLSingleton.class) {
            if (instance == null) {             // second check (with lock)
                instance = new DCLSingleton();
            }
        }
    }
    return instance;
}
```

Without `volatile`, the JVM can publish a reference to a partially-constructed object.

### Initialization-on-demand holder (preferred)

```java
private static class Holder {
    static final MyService INSTANCE = new MyService();  // initialised at first access
}
public static MyService getInstance() { return Holder.INSTANCE; }
```

Lazy, thread-safe, no locking, no `volatile`.

### False sharing

Two variables that share a CPU cache line (64 bytes) cause unnecessary cache invalidations when independently written by separate threads. Pad to separate cache lines:

```java
public volatile long value;
public long p1, p2, p3, p4, p5, p6, p7;  // 7 × 8 bytes padding
```

---

## Advanced Synchronizers

### Semaphore - bounded concurrency

```java
Semaphore sem = new Semaphore(3);   // at most 3 concurrent holders
sem.acquire();
try { ... } finally { sem.release(); }

sem.tryAcquire(100, MILLISECONDS);  // non-blocking with timeout
```

Use for: connection pools, rate limiting, resource guards.

### CyclicBarrier - reusable N-party barrier

```java
CyclicBarrier barrier = new CyclicBarrier(workers, () -> log("phase complete"));
// Each worker:
barrier.await();   // blocks until all workers have called await()
// All released simultaneously; barrier resets for the next phase
```

### Phaser - flexible multi-phase barrier

```java
Phaser phaser = new Phaser(workers) {
    @Override protected boolean onAdvance(int phase, int parties) {
        return phase >= 2;  // terminate after phase 2
    }
};
phaser.arriveAndAwaitAdvance();  // arrive and wait for all
phaser.arriveAndDeregister();    // arrive then permanently leave
```

Use when: tasks join or leave between phases, or phases have different party counts.

### Exchanger - two-thread rendezvous

```java
Exchanger<Buffer> exchanger = new Exchanger<>();
// Thread 1:
Buffer full = exchanger.exchange(myBuffer);   // blocks until Thread 2 arrives
// Thread 2:
Buffer received = exchanger.exchange(emptyBuffer);
```

Classic use: double-buffering between a producer and consumer.

---

## Concurrent Collections

### ConcurrentHashMap

```java
// Atomic update - no external lock needed
map.merge(key, 1, Integer::sum);                    // increment or insert 1
map.computeIfAbsent(key, k -> new ArrayList<>());   // lazy init nested structure
map.replaceAll((k, v) -> v + 1);                    // update all values
int total = map.reduceValues(1, Integer::sum);      // parallel reduce
```

**Never** put `null` keys or values - it throws `NullPointerException`.

### CopyOnWriteArrayList

```java
CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
// Safe to iterate while other threads add/remove:
for (Listener l : listeners) l.onEvent(e);
```

Write cost: O(n) copy on every mutation. Use only when reads vastly outnumber writes (e.g. event listener lists).

### BlockingQueue family

| Type                    | Bounded?      | Ordering     |
| ----------------------- | ------------- | ------------ |
| `ArrayBlockingQueue`    | Yes           | FIFO         |
| `LinkedBlockingQueue`   | Optional      | FIFO         |
| `PriorityBlockingQueue` | No            | Comparator   |
| `DelayQueue`            | No            | Delay expiry |
| `SynchronousQueue`      | Zero capacity | Rendezvous   |

```java
BlockingQueue<Task> q = new LinkedBlockingQueue<>(100);
q.put(task);                    // blocks if full
Task t = q.take();              // blocks if empty
q.offer(task, 100, MILLIS);     // timed offer
q.poll(100, MILLIS);            // timed poll
```

### ConcurrentSkipListMap / Set

Sorted concurrent equivalents of `TreeMap`/`TreeSet`. O(log n) operations, full `NavigableMap`/`NavigableSet` API under concurrent modification.

---

## Fork/Join Framework

Divide-and-conquer parallelism with work-stealing:

```java
class SumTask extends RecursiveTask<Long> {
    protected Long compute() {
        if (size <= THRESHOLD) return sequentialSum();  // base case
        SumTask left  = new SumTask(array, from, mid);
        SumTask right = new SumTask(array, mid,  to);
        left.fork();                        // schedule left async
        long rightResult = right.compute(); // compute right on this thread
        return left.join() + rightResult;   // wait for left
    }
}
ForkJoinPool pool = new ForkJoinPool();
long result = pool.invoke(new SumTask(array, 0, array.length));
```

- `RecursiveTask<V>` - returns a value
- `RecursiveAction` - void result
- `invokeAll(left, right)` - fork both, wait for both

### Threshold selection

Too small → task-creation overhead dominates. Too large → poor parallelism. Typical: 512–1024 elements for numeric work.

### Work-stealing

Each worker thread has a deque. When idle, it steals tasks from the _tail_ of a busy thread's deque. This keeps all cores busy with minimal coordination.

---

## Source Files

| File                         | What it covers                                                                                                                              |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| `MemoryModelDemo.java`       | Happens-before, volatile publication, final fields, DCL, init-on-demand holder, AtomicReference CAS, false sharing                          |
| `SynchronizersDemo.java`     | Semaphore (pool + throttle), CyclicBarrier, Phaser, Exchanger (swap + double-buffer)                                                        |
| `ConcurrentCollections.java` | ConcurrentHashMap (merge/compute), CopyOnWriteArrayList, BlockingQueue, PriorityBlockingQueue, ConcurrentSkipListMap, ConcurrentLinkedQueue |
| `ForkJoinDemo.java`          | RecursiveTask (sum, max, Fibonacci), RecursiveAction (merge sort), work-stealing pool stats                                                 |

---

## Common Mistakes

{: .warning }

> **DCL without `volatile` is broken.** The JVM can reorder object construction and assignment, publishing a reference before the object is fully initialised.

{: .warning }

> **`ConcurrentHashMap` does not allow null keys or values.** Unlike `HashMap`, any null will throw `NullPointerException` immediately.

{: .warning }

> **Don't block inside a `ForkJoinTask`.** Blocking a FJ thread ties up a carrier and reduces parallelism. Use `managedBlock` if you must block inside a task.

{: .tip }

> **Prefer init-on-demand holder over DCL.** It is simpler, has no volatile overhead, and is guaranteed correct by the class-loading specification.
> {% endraw %}
