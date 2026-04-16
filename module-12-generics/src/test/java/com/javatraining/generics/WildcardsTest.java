package com.javatraining.generics;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WildcardsTest {

    // ── unbounded wildcard ────────────────────────────────────────────────────

    @Nested
    class UnboundedTests {

        @Test
        void countNulls_none() {
            assertEquals(0, Wildcards.countNulls(List.of(1, 2, 3)));
        }

        @Test
        void countNulls_some() {
            assertEquals(2, Wildcards.countNulls(Arrays.asList(null, "a", null, "b")));
        }

        @Test
        void sameSize_equal_lengths() {
            assertTrue(Wildcards.sameSize(List.of(1, 2), List.of("a", "b")));
        }

        @Test
        void sameSize_different_lengths() {
            assertFalse(Wildcards.sameSize(List.of(1), List.of("a", "b")));
        }
    }

    // ── upper-bounded wildcard ────────────────────────────────────────────────

    @Nested
    class UpperBoundedTests {

        @Test
        void sumNumbers_integers() {
            assertEquals(15.0, Wildcards.sumNumbers(List.of(1, 2, 3, 4, 5)));
        }

        @Test
        void sumNumbers_doubles() {
            assertEquals(6.0, Wildcards.sumNumbers(List.of(1.5, 2.5, 2.0)));
        }

        @Test
        void sumNumbers_mixed_number_subtype() {
            // Mix: Long and Integer both extend Number
            List<Long> longs = List.of(10L, 20L, 30L);
            assertEquals(60.0, Wildcards.sumNumbers(longs));
        }

        @Test
        void maxOf_integers() {
            assertEquals(9, Wildcards.maxOf(List.of(3, 7, 9, 1)));
        }

        @Test
        void maxOf_strings() {
            assertEquals("zebra", Wildcards.maxOf(List.of("apple", "zebra", "mango")));
        }

        @Test
        void copyOf_produces_independent_list() {
            List<Integer> src  = List.of(1, 2, 3);
            List<Integer> copy = Wildcards.copyOf(src);
            assertEquals(src, copy);
            // copy is mutable and independent
            copy.add(4);
            assertEquals(3, src.size());
        }
    }

    // ── lower-bounded wildcard ────────────────────────────────────────────────

    @Nested
    class LowerBoundedTests {

        @Test
        void fillRange_populates_list() {
            List<Integer> list = new ArrayList<>();
            Wildcards.fillRange(list, 1, 5);
            assertEquals(List.of(1, 2, 3, 4, 5), list);
        }

        @Test
        void fillRange_into_number_list() {
            List<Number> list = new ArrayList<>();
            Wildcards.fillRange(list, 10, 12);
            assertEquals(3, list.size());
            assertEquals(10, list.get(0).intValue());
        }

        @Test
        void copy_from_integers_to_numbers() {
            List<Integer> src = List.of(1, 2, 3);
            List<Number>  dst = new ArrayList<>();
            Wildcards.copy(src, dst);
            assertEquals(3, dst.size());
            assertEquals(1, dst.get(0).intValue());
        }

        @Test
        void copy_appends_to_existing_content() {
            List<String> src = List.of("x", "y");
            List<Object> dst = new ArrayList<>(List.of("existing"));
            Wildcards.copy(src, dst);
            assertEquals(3, dst.size());
            assertEquals("existing", dst.get(0));
        }
    }

    // ── Stack PECS ────────────────────────────────────────────────────────────

    @Nested
    class StackPecsTests {

        @Test
        void pushAll_from_subtype_list() {
            Wildcards.Stack<Number> stack = new Wildcards.Stack<>();
            stack.pushAll(List.of(1, 2, 3));
            assertEquals(3, stack.size());
        }

        @Test
        void drainTo_into_supertype_list() {
            Wildcards.Stack<Integer> stack = new Wildcards.Stack<>();
            stack.push(10); stack.push(20);
            List<Number> sink = new ArrayList<>();
            stack.drainTo(sink);
            assertEquals(2, sink.size());
            assertTrue(stack.isEmpty());
        }

        @Test
        void drainTo_preserves_elements() {
            Wildcards.Stack<Integer> stack = new Wildcards.Stack<>();
            stack.push(1); stack.push(2); stack.push(3);
            List<Object> sink = new ArrayList<>();
            stack.drainTo(sink);
            assertTrue(sink.contains(1) && sink.contains(2) && sink.contains(3));
        }
    }

    // ── wildcard capture swap ─────────────────────────────────────────────────

    @Nested
    class SwapTests {

        @Test
        void swap_exchanges_elements() {
            List<String> list = new ArrayList<>(List.of("a", "b", "c"));
            Wildcards.swap(list, 0, 2);
            assertEquals("c", list.get(0));
            assertEquals("a", list.get(2));
        }

        @Test
        void swap_same_index_no_change() {
            List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
            Wildcards.swap(list, 1, 1);
            assertEquals(List.of(1, 2, 3), list);
        }
    }
}
