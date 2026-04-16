package com.javatraining.nested;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InnerClassDemoTest {

    // ── NumberRange / RangeIterator ───────────────────────────────────────────

    @Nested
    class NumberRangeTests {

        @Test
        void iterate_default_step() {
            InnerClassDemo.NumberRange range = new InnerClassDemo.NumberRange(1, 5);
            List<Integer> result = new ArrayList<>();
            for (int n : range) result.add(n);
            assertEquals(List.of(1, 2, 3, 4, 5), result);
        }

        @Test
        void iterate_with_step() {
            InnerClassDemo.NumberRange evens = new InnerClassDemo.NumberRange(0, 10, 2);
            List<Integer> result = new ArrayList<>();
            for (int n : evens) result.add(n);
            assertEquals(List.of(0, 2, 4, 6, 8, 10), result);
        }

        @Test
        void empty_range_produces_no_elements() {
            InnerClassDemo.NumberRange r = new InnerClassDemo.NumberRange(5, 3);
            List<Integer> result = new ArrayList<>();
            for (int n : r) result.add(n);
            assertTrue(result.isEmpty());
        }

        @Test
        void size_matches_element_count() {
            InnerClassDemo.NumberRange r = new InnerClassDemo.NumberRange(1, 7, 2); // 1,3,5,7
            assertEquals(4, r.size());
        }

        @Test
        void contains_true_for_step_aligned_value() {
            InnerClassDemo.NumberRange r = new InnerClassDemo.NumberRange(0, 10, 3); // 0,3,6,9
            assertTrue(r.contains(6));
            assertFalse(r.contains(4));
        }

        @Test
        void invalid_step_throws() {
            assertThrows(IllegalArgumentException.class,
                () -> new InnerClassDemo.NumberRange(1, 5, 0));
        }

        @Test
        void next_past_end_throws() {
            InnerClassDemo.NumberRange r = new InnerClassDemo.NumberRange(1, 1);
            Iterator<Integer> it = r.iterator();
            it.next(); // consumes the only element
            assertThrows(NoSuchElementException.class, it::next);
        }

        @Test
        void two_independent_iterators_do_not_share_state() {
            InnerClassDemo.NumberRange r = new InnerClassDemo.NumberRange(1, 3);
            Iterator<Integer> a = r.iterator();
            Iterator<Integer> b = r.iterator();
            assertEquals(1, a.next());
            assertEquals(2, a.next());
            assertEquals(1, b.next()); // b still at start
        }
    }

    // ── EventBus / Subscription ───────────────────────────────────────────────

    @Nested
    class EventBusTests {

        @Test
        void published_event_reaches_subscriber() {
            InnerClassDemo.EventBus<String> bus = new InnerClassDemo.EventBus<>();
            List<String> received = new ArrayList<>();
            bus.subscribe(received::add);
            bus.publish("hello");
            assertEquals(List.of("hello"), received);
        }

        @Test
        void multiple_subscribers_all_receive() {
            InnerClassDemo.EventBus<Integer> bus = new InnerClassDemo.EventBus<>();
            List<Integer> a = new ArrayList<>(), b = new ArrayList<>();
            bus.subscribe(a::add);
            bus.subscribe(b::add);
            bus.publish(42);
            assertEquals(List.of(42), a);
            assertEquals(List.of(42), b);
        }

        @Test
        void cancel_removes_subscriber() {
            InnerClassDemo.EventBus<String> bus = new InnerClassDemo.EventBus<>();
            List<String> received = new ArrayList<>();
            InnerClassDemo.EventBus<String>.Subscription sub = bus.subscribe(received::add);
            sub.cancel();
            bus.publish("after cancel");
            assertTrue(received.isEmpty());
            assertEquals(0, bus.subscriberCount());
        }

        @Test
        void cancelled_subscription_is_inactive() {
            InnerClassDemo.EventBus<String> bus = new InnerClassDemo.EventBus<>();
            InnerClassDemo.EventBus<String>.Subscription sub = bus.subscribe(e -> {});
            assertTrue(sub.isActive());
            sub.cancel();
            assertFalse(sub.isActive());
        }

        @Test
        void subscriber_count_tracks_adds_and_cancels() {
            InnerClassDemo.EventBus<String> bus = new InnerClassDemo.EventBus<>();
            var s1 = bus.subscribe(e -> {});
            var s2 = bus.subscribe(e -> {});
            assertEquals(2, bus.subscriberCount());
            s1.cancel();
            assertEquals(1, bus.subscriberCount());
        }
    }

    // ── TextBuffer / Cursor ───────────────────────────────────────────────────

    @Nested
    class TextBufferTests {

        @Test
        void current_char_at_position() {
            InnerClassDemo.TextBuffer buf = new InnerClassDemo.TextBuffer("Hello");
            InnerClassDemo.TextBuffer.Cursor c = buf.cursor(1);
            assertEquals('e', c.current());
        }

        @Test
        void insert_modifies_buffer() {
            InnerClassDemo.TextBuffer buf = new InnerClassDemo.TextBuffer("Hello");
            buf.cursor(5).insert(" world");
            assertEquals("Hello world", buf.content());
        }

        @Test
        void delete_removes_characters() {
            InnerClassDemo.TextBuffer buf = new InnerClassDemo.TextBuffer("Hello world");
            buf.cursor(5).delete(6); // remove " world"
            assertEquals("Hello", buf.content());
        }

        @Test
        void advance_moves_position() {
            InnerClassDemo.TextBuffer buf = new InnerClassDemo.TextBuffer("Hello");
            InnerClassDemo.TextBuffer.Cursor c = buf.cursor(0);
            c.advance(3);
            assertEquals(3, c.position());
            assertEquals('l', c.current());
        }

        @Test
        void atEnd_true_when_past_content() {
            InnerClassDemo.TextBuffer buf = new InnerClassDemo.TextBuffer("Hi");
            InnerClassDemo.TextBuffer.Cursor c = buf.cursor(2);
            assertTrue(c.atEnd());
        }

        @Test
        void buffer_reference_returns_outer_instance() {
            InnerClassDemo.TextBuffer buf = new InnerClassDemo.TextBuffer("x");
            assertSame(buf, buf.cursor(0).buffer());
        }

        @Test
        void cursor_past_end_throws() {
            InnerClassDemo.TextBuffer buf = new InnerClassDemo.TextBuffer("Hi");
            assertThrows(IndexOutOfBoundsException.class, () -> buf.cursor(10));
        }

        @Test
        void current_at_end_throws() {
            InnerClassDemo.TextBuffer buf = new InnerClassDemo.TextBuffer("Hi");
            assertThrows(NoSuchElementException.class, () -> buf.cursor(2).current());
        }
    }
}
