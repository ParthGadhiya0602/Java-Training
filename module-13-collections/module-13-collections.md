# Module 13 — Collections Framework

## What You Will Learn

| Interface | Common Implementations | Use When |
|---|---|---|
| `List<E>` | `ArrayList`, `LinkedList` | Ordered, index-accessible, duplicates allowed |
| `Set<E>` | `HashSet`, `LinkedHashSet`, `TreeSet` | No duplicates; hash / insertion / sorted order |
| `Map<K,V>` | `HashMap`, `LinkedHashMap`, `TreeMap` | Key→value; hash / insertion / sorted order |
| `Queue<E>` | `LinkedList`, `ArrayDeque`, `PriorityQueue` | FIFO; or priority-based retrieval |
| `Deque<E>` | `ArrayDeque`, `LinkedList` | Double-ended: stack + queue operations |

---

## Complexity Cheat Sheet

```
                 add/offer   remove/poll  get(i)   contains
ArrayList          O(1)*       O(n)        O(1)      O(n)
LinkedList         O(1)        O(1)        O(n)      O(n)
ArrayDeque         O(1)*       O(1)         —         O(n)
PriorityQueue      O(log n)    O(log n)     —         O(n)
HashSet            O(1)*       O(1)*        —         O(1)*
LinkedHashSet      O(1)*       O(1)*        —         O(1)*
TreeSet            O(log n)    O(log n)     —         O(log n)
HashMap            O(1)*       O(1)*        —        key O(1)*
LinkedHashMap      O(1)*       O(1)*        —        key O(1)*
TreeMap            O(log n)    O(log n)     —        key O(log n)

* amortised
```

---

## Choosing the Right Collection

```
Need indexed access?           → ArrayList
Need fast front/rear add?      → ArrayDeque
Need priority ordering?        → PriorityQueue
No duplicates, any order?      → HashSet
No duplicates, insertion order?→ LinkedHashSet
No duplicates, sorted?         → TreeSet
Fast key lookup?               → HashMap
Ordered by insertion?          → LinkedHashMap
Sorted by key?                 → TreeMap
```

---

## Factory Methods (Java 9+)

```java
List<String>        list = List.of("a", "b", "c");       // immutable
Set<Integer>        set  = Set.of(1, 2, 3);              // immutable, no duplicates
Map<String,Integer> map  = Map.of("a", 1, "b", 2);       // immutable, ≤ 10 entries
Map<String,Integer> big  = Map.ofEntries(                 // immutable, any size
    Map.entry("a", 1), Map.entry("b", 2));
```

---

## Source Files

| File | What it Demonstrates |
|---|---|
| `ListsAndQueues.java` | ArrayList vs LinkedList, ArrayDeque as stack/queue, PriorityQueue |
| `SetsAndMaps.java` | HashSet/LinkedHashSet/TreeSet, HashMap/LinkedHashMap/TreeMap, NavigableMap |
| `CollectionAlgorithms.java` | `Collections` utility: sort, binarySearch, shuffle, frequency, disjoint, unmodifiable/synchronized views |
| `CollectionPatterns.java` | Frequency map, groupBy, multimap, index inversion, sliding window, top-K |

---

## Running

```bash
cd module-13-collections
mvn test
```
