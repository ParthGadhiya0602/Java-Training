---
title: "23 — Algorithms & Data Structures"
parent: "Phase 2 — Core APIs"
nav_order: 23
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-23-algorithms/src){: .btn .btn-outline }

# Module 23 — Algorithms & Data Structures
{: .no_toc }

<details open markdown="block">
  <summary>Table of contents</summary>
  {: .text-delta }
1. TOC
{:toc}
</details>

---

## Sorting Algorithms

| Algorithm | Best | Average | Worst | Space | Stable |
|---|---|---|---|---|---|
| Bubble | O(n) | O(n²) | O(n²) | O(1) | yes |
| Selection | O(n²) | O(n²) | O(n²) | O(1) | no |
| Insertion | O(n) | O(n²) | O(n²) | O(1) | yes |
| Merge | O(n log n) | O(n log n) | O(n log n) | O(n) | yes |
| Quick | O(n log n) | O(n log n) | O(n²) | O(log n) | no |
| Heap | O(n log n) | O(n log n) | O(n log n) | O(1) | no |
| Counting | O(n+k) | O(n+k) | O(n+k) | O(k) | yes |

**Java's Arrays.sort():** Dual-pivot Quicksort for primitives; TimSort (merge + insertion) for objects — stable, O(n log n).

### Insertion sort

```java
for (int i = 1; i < arr.length; i++) {
    int key = arr[i], j = i - 1;
    while (j >= 0 && arr[j] > key) arr[j + 1] = arr[j--];
    arr[j + 1] = key;
}
```

Best case O(n) — excellent for nearly-sorted data; TimSort's base case.

### Merge sort

```java
void mergeSort(int[] arr, int l, int r) {
    if (l >= r) return;
    int mid = l + (r - l) / 2;
    mergeSort(arr, l, mid);
    mergeSort(arr, mid + 1, r);
    merge(arr, l, mid, r);   // O(n) merge with temporary array
}
```

### Quick sort

Pivot selection matters: median-of-three avoids O(n²) on sorted input.

```java
int partition(int[] arr, int lo, int hi) {
    int pivot = arr[hi], i = lo - 1;
    for (int j = lo; j < hi; j++)
        if (arr[j] <= pivot) swap(arr, ++i, j);
    swap(arr, i + 1, hi);
    return i + 1;
}
```

### Counting sort

```java
int[] count = new int[max + 1];
for (int v : arr) count[v]++;
for (int i = 1; i <= max; i++) count[i] += count[i - 1];  // prefix sums
// Traverse right-to-left for stability
for (int i = arr.length - 1; i >= 0; i--)
    output[--count[arr[i]]] = arr[i];
```

---

## Search Algorithms

### Binary search

```java
int lo = 0, hi = arr.length - 1;
while (lo <= hi) {
    int mid = lo + (hi - lo) / 2;   // avoids overflow
    if      (arr[mid] == target) return mid;
    else if (arr[mid] <  target) lo = mid + 1;
    else                         hi = mid - 1;
}
return -1;
```

### Binary search bounds

```java
// Left bound — first occurrence
int lo = 0, hi = n - 1, result = -1;
while (lo <= hi) {
    int mid = lo + (hi - lo) / 2;
    if (arr[mid] == target) { result = mid; hi = mid - 1; }  // keep searching left
    else if (arr[mid] < target) lo = mid + 1;
    else                        hi = mid - 1;
}

// Lower bound — first index where arr[i] >= target (like C++ lower_bound)
int lo = 0, hi = n;
while (lo < hi) {
    int mid = lo + (hi - lo) / 2;
    if (arr[mid] < target) lo = mid + 1;
    else                   hi = mid;
}
return lo;   // returns arr.length if all elements < target
```

### Binary search on the answer

When the answer has a monotone property (false, false, ..., true, true):

```java
// Find minimum x in [lo, hi] where predicate(x) is true
while (lo < hi) {
    long mid = lo + (hi - lo) / 2;
    if (predicate(mid)) hi = mid;
    else                lo = mid + 1;
}
return lo;
```

### Search in rotated sorted array

```java
if (arr[lo] <= arr[mid]) {          // left half is sorted
    if (arr[lo] <= target && target < arr[mid]) hi = mid - 1;
    else                                         lo = mid + 1;
} else {                            // right half is sorted
    if (arr[mid] < target && target <= arr[hi]) lo = mid + 1;
    else                                         hi = mid - 1;
}
```

### 2D matrix search (sorted rows and columns)

Start top-right: if too large move left, if too small move down. O(m + n).

---

## Data Structures

### Stack

```java
// Array-backed LIFO, O(1) push/pop
push: data[size++] = value;  // double array if full
pop:  return data[--size];
```

### Queue (circular array)

```java
// Circular indices: tail wraps around
enqueue: data[tail] = value; tail = (tail + 1) % capacity; size++;
dequeue: value = data[head]; head = (head + 1) % capacity; size--;
```

### Singly Linked List

```java
addFirst: node.next = head; head = node;
reverse:  Node prev = null; while (cur != null) { next = cur.next; cur.next = prev; prev = cur; cur = next; }
hasCycle: Floyd's tortoise and hare — slow/fast pointers meet iff cycle exists
```

### Binary Search Tree

```java
insert: if val < node.val recurse left, else recurse right
delete: leaf → null; one child → replace; two children → swap with in-order successor
inOrder: left → root → right gives ascending order
```

Average O(log n) for balanced trees; O(n) worst case (degenerate/sorted input).

### Min-Heap

```java
// Parent: (i-1)/2   Left child: 2i+1   Right child: 2i+2
insert: append, siftUp (swap with parent while smaller)
poll:   swap root with last, remove last, siftDown (swap with smaller child)
```

`java.util.PriorityQueue` is a min-heap; use `Collections.reverseOrder()` for max-heap.

### Hash Map (separate chaining)

```java
bucketIndex = (key.hashCode() & 0x7fff_ffff) % capacity
// Resize when size / capacity > 0.75 (load factor)
```

---

## Common Patterns

### Two Pointers

```java
// Pair sum in sorted array — O(n)
int lo = 0, hi = n - 1;
while (lo < hi) {
    int sum = arr[lo] + arr[hi];
    if (sum == target) return true;
    else if (sum < target) lo++;
    else                   hi--;
}

// Sliding window max sum — O(n)
for (int i = k; i < n; i++) {
    sum += arr[i] - arr[i - k];
    max = Math.max(max, sum);
}
```

### Kadane's (Maximum Subarray Sum)

```java
int maxEndingHere = arr[0], maxSoFar = arr[0];
for (int i = 1; i < n; i++) {
    maxEndingHere = Math.max(arr[i], maxEndingHere + arr[i]);
    maxSoFar      = Math.max(maxSoFar, maxEndingHere);
}
```

### Dynamic Programming

```java
// LCS — O(m*n)
dp[i][j] = a[i-1]==b[j-1] ? dp[i-1][j-1]+1 : max(dp[i-1][j], dp[i][j-1]);

// Knapsack 0/1 — O(n * W)
dp[i][w] = weight[i] <= w
    ? max(dp[i-1][w], dp[i-1][w-weight[i]] + value[i])
    : dp[i-1][w];

// LIS — O(n log n) patience sorting
for each x: binary search tails[] for insertion point; extend or replace

// Edit distance — O(m*n), O(min(m,n)) space with rolling array
if s[i-1]==t[j-1]: curr[j] = prev[j-1]
else:              curr[j] = 1 + min(prev[j-1], prev[j], curr[j-1])
```

### Greedy

```java
// Activity selection — sort by end time, greedily pick non-overlapping
Arrays.sort(activities, Comparator.comparingInt(a -> a[1]));
// Coin change — greedy works for canonical systems (US coins); use DP for arbitrary
```

### Backtracking template

```java
void backtrack(state, choices) {
    if (isComplete(state)) { result.add(copy(state)); return; }
    for (choice : choices) {
        if (isValid(state, choice)) {
            apply(state, choice);
            backtrack(state, remainingChoices);
            undo(state, choice);      // ← the key step
        }
    }
}
```

### Graph traversal

```java
// BFS — shortest path in unweighted graph
Queue<Integer> q = new ArrayDeque<>();
q.add(start); seen.add(start);
while (!q.isEmpty()) { int n = q.poll(); for (int nb : adj(n)) if (seen.add(nb)) q.add(nb); }

// Topological sort (DFS) — post-order reversal
// Detect cycle: node in current DFS path → cycle
```

### Bit Tricks

```java
isPowerOfTwo:  n > 0 && (n & (n-1)) == 0
countBits:     while (n != 0) { n &= n-1; count++; }   // Brian Kernighan
singleNumber:  XOR all elements — pairs cancel, lone element remains
setBit:        n | (1 << pos)
clearBit:      n & ~(1 << pos)
toggleBit:     n ^ (1 << pos)
```

---

## Complexity Quick Reference

| Structure | Access | Search | Insert | Delete |
|---|---|---|---|---|
| Array | O(1) | O(n) | O(n) | O(n) |
| Linked list | O(n) | O(n) | O(1) | O(1) |
| Stack / Queue | O(1) top | O(n) | O(1)* | O(1) |
| Hash map | — | O(1)* | O(1)* | O(1)* |
| BST (balanced) | O(log n) | O(log n) | O(log n) | O(log n) |
| Heap | O(1) min | O(n) | O(log n) | O(log n) |

*Amortised
