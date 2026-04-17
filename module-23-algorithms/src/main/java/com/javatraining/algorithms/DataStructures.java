package com.javatraining.algorithms;

import java.util.*;

/**
 * Module 23 — Core Data Structures (hand-rolled implementations)
 *
 * Understanding internals helps choose the right JDK collection.
 *
 * Structure        | Access  | Search  | Insert  | Delete  | Notes
 * -----------------|---------|---------|---------|---------|------
 * Dynamic array    | O(1)    | O(n)    | O(1)*   | O(n)    | amortised
 * Linked list      | O(n)    | O(n)    | O(1)    | O(1)    | given node ref
 * Stack (array)    | O(1)    | O(n)    | O(1)*   | O(1)    |
 * Queue (circular) | O(1)    | O(n)    | O(1)*   | O(1)    |
 * Hash map         | —       | O(1)*   | O(1)*   | O(1)*   | amortised
 * Binary search tree| O(log n)| O(log n)| O(log n)| O(log n)| balanced
 * Min-heap         | O(1) min| O(n)    | O(log n)| O(log n)|
 */
public class DataStructures {

    // ── Stack (array-backed) ──────────────────────────────────────────────────

    /**
     * LIFO stack backed by an int array with doubling resize.
     */
    public static class IntStack {
        private int[] data;
        private int   size;

        public IntStack(int initialCapacity) {
            data = new int[Math.max(1, initialCapacity)];
        }

        public void push(int value) {
            if (size == data.length) data = Arrays.copyOf(data, data.length * 2);
            data[size++] = value;
        }

        public int pop() {
            if (size == 0) throw new EmptyStackException();
            return data[--size];
        }

        public int peek() {
            if (size == 0) throw new EmptyStackException();
            return data[size - 1];
        }

        public boolean isEmpty() { return size == 0; }
        public int     size()    { return size; }
    }

    // ── Queue (circular array) ────────────────────────────────────────────────

    /**
     * FIFO queue backed by a circular array with doubling resize.
     */
    public static class CircularQueue<T> {
        private Object[] data;
        private int head, tail, size;

        public CircularQueue(int capacity) {
            data = new Object[Math.max(1, capacity)];
        }

        public void enqueue(T value) {
            if (size == data.length) grow();
            data[tail] = value;
            tail = (tail + 1) % data.length;
            size++;
        }

        @SuppressWarnings("unchecked")
        public T dequeue() {
            if (size == 0) throw new NoSuchElementException("queue is empty");
            T value = (T) data[head];
            data[head] = null;
            head = (head + 1) % data.length;
            size--;
            return value;
        }

        @SuppressWarnings("unchecked")
        public T peek() {
            if (size == 0) throw new NoSuchElementException("queue is empty");
            return (T) data[head];
        }

        public boolean isEmpty() { return size == 0; }
        public int     size()    { return size; }

        private void grow() {
            Object[] newData = new Object[data.length * 2];
            for (int i = 0; i < size; i++) newData[i] = data[(head + i) % data.length];
            head = 0;
            tail = size;
            data = newData;
        }
    }

    // ── Singly linked list ────────────────────────────────────────────────────

    public static class LinkedList<T> {
        private static class Node<T> {
            T data; Node<T> next;
            Node(T data) { this.data = data; }
        }

        private Node<T> head;
        private int size;

        public void addFirst(T value) {
            Node<T> node = new Node<>(value);
            node.next = head;
            head = node;
            size++;
        }

        public void addLast(T value) {
            Node<T> node = new Node<>(value);
            if (head == null) { head = node; }
            else {
                Node<T> cur = head;
                while (cur.next != null) cur = cur.next;
                cur.next = node;
            }
            size++;
        }

        public T removeFirst() {
            if (head == null) throw new NoSuchElementException();
            T value = head.data;
            head = head.next;
            size--;
            return value;
        }

        /** Reverses the list in-place. O(n) time, O(1) space. */
        public void reverse() {
            Node<T> prev = null, cur = head;
            while (cur != null) {
                Node<T> next = cur.next;
                cur.next = prev;
                prev = cur;
                cur = next;
            }
            head = prev;
        }

        /** Returns the element at position index (0-based). O(n). */
        public T get(int index) {
            Node<T> cur = head;
            for (int i = 0; i < index; i++) cur = cur.next;
            return cur.data;
        }

        public boolean contains(T value) {
            for (Node<T> cur = head; cur != null; cur = cur.next) {
                if (Objects.equals(cur.data, value)) return true;
            }
            return false;
        }

        public int  size()    { return size; }
        public boolean isEmpty() { return size == 0; }

        /** Returns true if there is a cycle (Floyd's tortoise and hare). */
        public boolean hasCycle() {
            Node<T> slow = head, fast = head;
            while (fast != null && fast.next != null) {
                slow = slow.next;
                fast = fast.next.next;
                if (slow == fast) return true;
            }
            return false;
        }
    }

    // ── Binary Search Tree ────────────────────────────────────────────────────

    public static class BST {
        private static class Node {
            int val; Node left, right;
            Node(int val) { this.val = val; }
        }

        private Node root;

        public void insert(int val) {
            root = insert(root, val);
        }

        private Node insert(Node node, int val) {
            if (node == null) return new Node(val);
            if      (val < node.val) node.left  = insert(node.left,  val);
            else if (val > node.val) node.right = insert(node.right, val);
            return node;
        }

        public boolean contains(int val) {
            Node cur = root;
            while (cur != null) {
                if      (val == cur.val) return true;
                else if (val <  cur.val) cur = cur.left;
                else                     cur = cur.right;
            }
            return false;
        }

        public void delete(int val) { root = delete(root, val); }

        private Node delete(Node node, int val) {
            if (node == null) return null;
            if      (val < node.val) node.left  = delete(node.left,  val);
            else if (val > node.val) node.right = delete(node.right, val);
            else {
                if (node.left  == null) return node.right;
                if (node.right == null) return node.left;
                // Replace with in-order successor (min of right subtree)
                Node min = node.right;
                while (min.left != null) min = min.left;
                node.val   = min.val;
                node.right = delete(node.right, min.val);
            }
            return node;
        }

        /** In-order traversal (ascending order for BST). */
        public List<Integer> inOrder() {
            List<Integer> result = new ArrayList<>();
            inOrder(root, result);
            return result;
        }

        private void inOrder(Node node, List<Integer> result) {
            if (node == null) return;
            inOrder(node.left, result);
            result.add(node.val);
            inOrder(node.right, result);
        }

        public int height() { return height(root); }

        private int height(Node node) {
            if (node == null) return 0;
            return 1 + Math.max(height(node.left), height(node.right));
        }
    }

    // ── Min-Heap ──────────────────────────────────────────────────────────────

    /**
     * Min-heap backed by an array.
     * Parent of i: (i-1)/2   Left child: 2i+1   Right child: 2i+2
     */
    public static class MinHeap {
        private int[] data;
        private int   size;

        public MinHeap(int capacity) {
            data = new int[Math.max(1, capacity)];
        }

        public void insert(int val) {
            if (size == data.length) data = Arrays.copyOf(data, data.length * 2);
            data[size] = val;
            siftUp(size++);
        }

        public int peek() {
            if (size == 0) throw new NoSuchElementException("heap is empty");
            return data[0];
        }

        public int poll() {
            if (size == 0) throw new NoSuchElementException("heap is empty");
            int min = data[0];
            data[0] = data[--size];
            siftDown(0);
            return min;
        }

        public boolean isEmpty() { return size == 0; }
        public int     size()    { return size; }

        private void siftUp(int i) {
            while (i > 0) {
                int parent = (i - 1) / 2;
                if (data[i] >= data[parent]) break;
                swap(i, parent);
                i = parent;
            }
        }

        private void siftDown(int i) {
            while (true) {
                int smallest = i;
                int left  = 2 * i + 1;
                int right = 2 * i + 2;
                if (left  < size && data[left]  < data[smallest]) smallest = left;
                if (right < size && data[right] < data[smallest]) smallest = right;
                if (smallest == i) break;
                swap(i, smallest);
                i = smallest;
            }
        }

        private void swap(int i, int j) {
            int tmp = data[i]; data[i] = data[j]; data[j] = tmp;
        }
    }

    // ── HashMap (separate chaining) ───────────────────────────────────────────

    /**
     * Simple open-addressing-free hash map using separate chaining.
     * Resizes when load factor exceeds 0.75.
     */
    public static class SimpleHashMap<K, V> {
        private static final int DEFAULT_CAPACITY = 16;
        private static final double LOAD_FACTOR   = 0.75;

        private Object[][] buckets;
        private int size;

        @SuppressWarnings("unchecked")
        public SimpleHashMap() {
            buckets = new Object[DEFAULT_CAPACITY][];
        }

        public void put(K key, V value) {
            int idx = bucketIndex(key);
            if (buckets[idx] == null) {
                buckets[idx] = new Object[]{key, value};
            } else {
                // Linear probe within bucket (stored as flat key-value pairs)
                Object[] bucket = buckets[idx];
                for (int i = 0; i < bucket.length; i += 2) {
                    if (Objects.equals(bucket[i], key)) { bucket[i + 1] = value; return; }
                }
                Object[] grown = Arrays.copyOf(bucket, bucket.length + 2);
                grown[bucket.length]     = key;
                grown[bucket.length + 1] = value;
                buckets[idx] = grown;
            }
            size++;
            if ((double) size / buckets.length > LOAD_FACTOR) resize();
        }

        @SuppressWarnings("unchecked")
        public V get(K key) {
            int idx = bucketIndex(key);
            Object[] bucket = buckets[idx];
            if (bucket == null) return null;
            for (int i = 0; i < bucket.length; i += 2) {
                if (Objects.equals(bucket[i], key)) return (V) bucket[i + 1];
            }
            return null;
        }

        public boolean containsKey(K key) { return get(key) != null; }
        public int     size()             { return size; }

        private int bucketIndex(K key) {
            return (key.hashCode() & 0x7fff_ffff) % buckets.length;
        }

        @SuppressWarnings("unchecked")
        private void resize() {
            Object[][] old = buckets;
            buckets = new Object[old.length * 2][];
            size = 0;
            for (Object[] bucket : old) {
                if (bucket == null) continue;
                for (int i = 0; i < bucket.length; i += 2) {
                    put((K) bucket[i], (V) bucket[i + 1]);
                }
            }
        }
    }
}
