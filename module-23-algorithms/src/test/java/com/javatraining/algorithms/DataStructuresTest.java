package com.javatraining.algorithms;

import com.javatraining.algorithms.DataStructures.*;
import org.junit.jupiter.api.*;

import java.util.EmptyStackException;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DataStructures")
class DataStructuresTest {

    @Nested
    @DisplayName("IntStack")
    class StackTests {
        @Test void push_and_pop() {
            IntStack s = new IntStack(4);
            s.push(1); s.push(2); s.push(3);
            assertEquals(3, s.pop());
            assertEquals(2, s.pop());
        }

        @Test void peek_does_not_remove() {
            IntStack s = new IntStack(4);
            s.push(42);
            assertEquals(42, s.peek());
            assertEquals(1, s.size());
        }

        @Test void pop_empty_throws() {
            assertThrows(EmptyStackException.class, () -> new IntStack(1).pop());
        }

        @Test void stack_resizes() {
            IntStack s = new IntStack(2);
            for (int i = 0; i < 100; i++) s.push(i);
            assertEquals(100, s.size());
            assertEquals(99, s.pop());
        }
    }

    @Nested
    @DisplayName("CircularQueue")
    class QueueTests {
        @Test void enqueue_dequeue_fifo() {
            CircularQueue<Integer> q = new CircularQueue<>(4);
            q.enqueue(1); q.enqueue(2); q.enqueue(3);
            assertEquals(1, q.dequeue());
            assertEquals(2, q.dequeue());
        }

        @Test void peek_does_not_remove() {
            CircularQueue<String> q = new CircularQueue<>(4);
            q.enqueue("hi");
            assertEquals("hi", q.peek());
            assertEquals(1, q.size());
        }

        @Test void dequeue_empty_throws() {
            assertThrows(NoSuchElementException.class, () -> new CircularQueue<>(1).dequeue());
        }

        @Test void wrap_around_works() {
            CircularQueue<Integer> q = new CircularQueue<>(4);
            q.enqueue(1); q.enqueue(2); q.enqueue(3); q.enqueue(4);
            q.dequeue(); q.dequeue();
            q.enqueue(5); q.enqueue(6);  // wraps around
            assertEquals(3, q.dequeue());
            assertEquals(4, q.dequeue());
            assertEquals(5, q.dequeue());
        }

        @Test void queue_grows_when_full() {
            CircularQueue<Integer> q = new CircularQueue<>(2);
            for (int i = 0; i < 100; i++) q.enqueue(i);
            assertEquals(100, q.size());
            assertEquals(0, q.dequeue());
        }
    }

    @Nested
    @DisplayName("LinkedList")
    class LinkedListTests {
        @Test void add_and_get() {
            LinkedList<Integer> list = new LinkedList<>();
            list.addLast(1); list.addLast(2); list.addLast(3);
            assertEquals(1, list.get(0));
            assertEquals(3, list.get(2));
        }

        @Test void add_first() {
            LinkedList<String> list = new LinkedList<>();
            list.addLast("b"); list.addFirst("a");
            assertEquals("a", list.get(0));
        }

        @Test void remove_first() {
            LinkedList<Integer> list = new LinkedList<>();
            list.addLast(10); list.addLast(20);
            assertEquals(10, list.removeFirst());
            assertEquals(1, list.size());
        }

        @Test void contains() {
            LinkedList<String> list = new LinkedList<>();
            list.addLast("x"); list.addLast("y");
            assertTrue(list.contains("x"));
            assertFalse(list.contains("z"));
        }

        @Test void reverse() {
            LinkedList<Integer> list = new LinkedList<>();
            list.addLast(1); list.addLast(2); list.addLast(3);
            list.reverse();
            assertEquals(3, list.get(0));
            assertEquals(1, list.get(2));
        }

        @Test void no_cycle_in_normal_list() {
            LinkedList<Integer> list = new LinkedList<>();
            list.addLast(1); list.addLast(2);
            assertFalse(list.hasCycle());
        }
    }

    @Nested
    @DisplayName("BST")
    class BSTTests {
        @Test void insert_and_contains() {
            BST bst = new BST();
            bst.insert(5); bst.insert(3); bst.insert(7);
            assertTrue(bst.contains(3));
            assertFalse(bst.contains(4));
        }

        @Test void in_order_returns_sorted() {
            BST bst = new BST();
            for (int v : new int[]{5, 3, 7, 1, 4}) bst.insert(v);
            assertEquals(java.util.List.of(1, 3, 4, 5, 7), bst.inOrder());
        }

        @Test void delete_leaf() {
            BST bst = new BST();
            bst.insert(5); bst.insert(3); bst.insert(7);
            bst.delete(3);
            assertFalse(bst.contains(3));
            assertTrue(bst.contains(5));
        }

        @Test void delete_node_with_two_children() {
            BST bst = new BST();
            for (int v : new int[]{5, 3, 7, 1, 4}) bst.insert(v);
            bst.delete(3);
            assertFalse(bst.contains(3));
            assertEquals(java.util.List.of(1, 4, 5, 7), bst.inOrder());
        }

        @Test void height_of_balanced_tree() {
            BST bst = new BST();
            bst.insert(4); bst.insert(2); bst.insert(6); bst.insert(1); bst.insert(3);
            assertEquals(3, bst.height());
        }
    }

    @Nested
    @DisplayName("MinHeap")
    class MinHeapTests {
        @Test void peek_returns_minimum() {
            MinHeap h = new MinHeap(8);
            h.insert(5); h.insert(1); h.insert(3);
            assertEquals(1, h.peek());
        }

        @Test void poll_extracts_in_sorted_order() {
            MinHeap h = new MinHeap(8);
            for (int v : new int[]{5, 2, 8, 1, 9}) h.insert(v);
            assertEquals(1, h.poll());
            assertEquals(2, h.poll());
            assertEquals(5, h.poll());
        }

        @Test void poll_empty_throws() {
            assertThrows(NoSuchElementException.class, () -> new MinHeap(4).poll());
        }

        @Test void heap_grows_dynamically() {
            MinHeap h = new MinHeap(2);
            for (int i = 100; i >= 1; i--) h.insert(i);
            assertEquals(1, h.poll());
        }
    }

    @Nested
    @DisplayName("SimpleHashMap")
    class HashMapTests {
        @Test void put_and_get() {
            SimpleHashMap<String, Integer> map = new SimpleHashMap<>();
            map.put("a", 1); map.put("b", 2);
            assertEquals(1, map.get("a"));
            assertEquals(2, map.get("b"));
        }

        @Test void overwrite_existing_key() {
            SimpleHashMap<String, Integer> map = new SimpleHashMap<>();
            map.put("k", 1); map.put("k", 99);
            assertEquals(99, map.get("k"));
        }

        @Test void missing_key_returns_null() {
            SimpleHashMap<String, Integer> map = new SimpleHashMap<>();
            assertNull(map.get("missing"));
        }

        @Test void contains_key() {
            SimpleHashMap<String, Integer> map = new SimpleHashMap<>();
            map.put("x", 10);
            assertTrue(map.containsKey("x"));
            assertFalse(map.containsKey("y"));
        }

        @Test void handles_many_entries_with_resize() {
            SimpleHashMap<Integer, Integer> map = new SimpleHashMap<>();
            for (int i = 0; i < 200; i++) map.put(i, i * 2);
            for (int i = 0; i < 200; i++) assertEquals(i * 2, map.get(i));
        }
    }
}
