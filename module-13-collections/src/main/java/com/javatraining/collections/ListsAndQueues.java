package com.javatraining.collections;

import java.util.*;

/**
 * TOPIC: List and Queue family
 *
 * List - ordered, indexed, duplicates allowed
 *   ArrayList  - backed by an array; O(1) random access, O(n) insert/remove in middle
 *   LinkedList - doubly-linked; O(1) front/back ops, O(n) random access; also implements Deque
 *
 * Queue / Deque - first-in-first-out (or double-ended)
 *   ArrayDeque  - resizable array; O(1) amortised push/pop/offer/poll from both ends
 *                 Preferred over Stack (legacy) and LinkedList for both stack and queue use
 *   PriorityQueue - min-heap; poll() always returns the smallest element (natural order
 *                   or a custom Comparator)
 *
 * Queue method pairs - two flavours for each operation:
 *   throws exception   returns special value
 *   add(e)             offer(e)       - insert
 *   remove()           poll()         - remove head
 *   element()          peek()         - inspect head
 */
public class ListsAndQueues {

    // -------------------------------------------------------------------------
    // 1. ArrayList - random access, bulk operations
    // -------------------------------------------------------------------------

    /**
     * Removes every occurrence of {@code value} from a list, in-place.
     * Iterates backwards to avoid index-shift bugs.
     */
    static <T> void removeAll(List<T> list, T value) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (Objects.equals(list.get(i), value)) list.remove(i);
        }
    }

    /** Returns a new list containing only elements at even indices (0, 2, 4, …). */
    static <T> List<T> everyOther(List<T> list) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += 2) result.add(list.get(i));
        return result;
    }

    /**
     * Rotates the list left by {@code k} positions IN-PLACE.
     * [1,2,3,4,5] rotated left by 2 → [3,4,5,1,2]
     */
    static <T> void rotateLeft(List<T> list, int k) {
        if (list.isEmpty()) return;
        k = k % list.size();
        if (k < 0) k += list.size();
        Collections.rotate(list, -k);
    }

    // -------------------------------------------------------------------------
    // 2. Deque (ArrayDeque) - stack and queue operations
    //    ArrayDeque is preferred over java.util.Stack (which is synchronized/legacy)
    //    and over LinkedList (better cache locality, no node overhead).
    // -------------------------------------------------------------------------

    /**
     * Returns true if {@code s} is a valid bracket string.
     * Uses an ArrayDeque as a stack.
     * Valid: "([]{})", "[{()}]"   Invalid: "(]", "([)]"
     */
    static boolean isBalanced(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '(', '[', '{' -> stack.push(c);
                case ')' -> { if (stack.isEmpty() || stack.pop() != '(') return false; }
                case ']' -> { if (stack.isEmpty() || stack.pop() != '[') return false; }
                case '}' -> { if (stack.isEmpty() || stack.pop() != '{') return false; }
            }
        }
        return stack.isEmpty();
    }

    /**
     * Simulates a print queue: enqueues jobs then processes them FIFO,
     * returning the order in which they were printed.
     */
    static List<String> processPrintQueue(List<String> jobs) {
        Queue<String> queue = new ArrayDeque<>(jobs);
        List<String> printed = new ArrayList<>();
        while (!queue.isEmpty()) printed.add(queue.poll());
        return printed;
    }

    /**
     * Returns the last {@code k} elements seen in a stream, preserving order.
     * Maintains a sliding window deque of size k.
     */
    static <T> List<T> lastK(Iterable<T> stream, int k) {
        if (k <= 0) return List.of();
        Deque<T> window = new ArrayDeque<>(k + 1);
        for (T item : stream) {
            window.addLast(item);
            if (window.size() > k) window.pollFirst();
        }
        return new ArrayList<>(window);
    }

    // -------------------------------------------------------------------------
    // 3. PriorityQueue - always poll the "best" element
    //    Default: natural order (min-heap for numbers, lexicographic for strings)
    //    Custom:  pass a Comparator to the constructor
    // -------------------------------------------------------------------------

    /**
     * Returns the k smallest elements from the list, in ascending order.
     * Uses a max-heap of size k to run in O(n log k).
     */
    static <T extends Comparable<T>> List<T> topKSmallest(List<T> items, int k) {
        if (k <= 0) return List.of();
        // max-heap of size k: if new element < current max, swap in
        PriorityQueue<T> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        for (T item : items) {
            maxHeap.offer(item);
            if (maxHeap.size() > k) maxHeap.poll();   // evict largest
        }
        List<T> result = new ArrayList<>(maxHeap);
        Collections.sort(result);
        return result;
    }

    /**
     * Merges k sorted lists into one sorted list.
     * Classic PriorityQueue merge: O(n log k) where n = total elements.
     */
    static List<Integer> mergeKSorted(List<List<Integer>> lists) {
        // each entry: [value, list-index, element-index]
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        for (int i = 0; i < lists.size(); i++) {
            if (!lists.get(i).isEmpty()) pq.offer(new int[]{lists.get(i).get(0), i, 0});
        }
        List<Integer> result = new ArrayList<>();
        while (!pq.isEmpty()) {
            int[] top = pq.poll();
            result.add(top[0]);
            int li = top[1], ei = top[2] + 1;
            if (ei < lists.get(li).size()) pq.offer(new int[]{lists.get(li).get(ei), li, ei});
        }
        return result;
    }

    /**
     * Task scheduler: given tasks with priorities, returns them in priority order
     * (higher number = higher priority).
     */
    record Task(String name, int priority) {}

    static List<String> scheduleTasks(List<Task> tasks) {
        // max-heap by priority
        PriorityQueue<Task> pq = new PriorityQueue<>(
            Comparator.comparingInt(Task::priority).reversed());
        pq.addAll(tasks);
        List<String> order = new ArrayList<>();
        while (!pq.isEmpty()) order.add(pq.poll().name());
        return order;
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void arrayListDemo() {
        System.out.println("=== ArrayList operations ===");
        List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 2, 4, 2, 5));
        removeAll(nums, 2);
        System.out.println("after removeAll(2): " + nums);
        System.out.println("everyOther:         " + everyOther(nums));

        List<Integer> rot = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        rotateLeft(rot, 2);
        System.out.println("rotateLeft by 2:    " + rot);
    }

    static void dequeDemo() {
        System.out.println("\n=== ArrayDeque (stack / queue) ===");
        System.out.println("isBalanced \"([{}])\": " + isBalanced("([{}])"));
        System.out.println("isBalanced \"([)]\": "  + isBalanced("([)]"));
        System.out.println("isBalanced \"\": "       + isBalanced(""));

        System.out.println("printQueue: " + processPrintQueue(List.of("doc1","doc2","doc3")));
        System.out.println("lastK(3):   " + lastK(List.of(1,2,3,4,5,6,7), 3));
    }

    static void priorityQueueDemo() {
        System.out.println("\n=== PriorityQueue ===");
        List<Integer> data = List.of(5, 1, 9, 3, 7, 2, 8);
        System.out.println("top 3 smallest: " + topKSmallest(data, 3));

        List<List<Integer>> sorted = List.of(
            List.of(1, 4, 7),
            List.of(2, 5, 8),
            List.of(3, 6, 9)
        );
        System.out.println("mergeKSorted: " + mergeKSorted(sorted));

        List<Task> tasks = List.of(
            new Task("low",    1),
            new Task("high",   5),
            new Task("medium", 3),
            new Task("urgent", 7)
        );
        System.out.println("task order: " + scheduleTasks(tasks));
    }

    public static void main(String[] args) {
        arrayListDemo();
        dequeDemo();
        priorityQueueDemo();
    }
}
