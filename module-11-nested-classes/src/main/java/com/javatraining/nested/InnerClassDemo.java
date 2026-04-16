package com.javatraining.nested;

import java.util.*;
import java.util.function.Consumer;

/**
 * TOPIC: Non-static inner classes
 *
 * An inner class is a non-static member class declared inside another class.
 * Every inner-class instance holds an implicit reference to its enclosing
 * outer-class instance.  That hidden reference is written Outer.this.
 *
 * Key facts:
 *   • Instantiation requires an outer instance:  outer.new Inner()
 *   • Inner class can read/write ALL outer fields (even private)
 *   • Serialising an inner class also drags in the outer — watch out
 *   • Use inner class when behaviour is tightly coupled to one outer instance
 *
 * Common real-world uses:
 *   • Custom Iterator implementations (see NumberRange below)
 *   • Event listeners tightly bound to one UI component
 *   • Cursor / position objects that walk over an outer collection
 */
public class InnerClassDemo {

    // -------------------------------------------------------------------------
    // 1. NumberRange — inner Iterator
    //    The iterator must remember which range it belongs to AND its cursor.
    //    Perfect fit for an inner class: iterator state + outer range access.
    // -------------------------------------------------------------------------
    static final class NumberRange implements Iterable<Integer> {

        private final int start;
        private final int end;    // inclusive
        private final int step;

        NumberRange(int start, int end, int step) {
            if (step <= 0) throw new IllegalArgumentException("step must be > 0");
            this.start = start;
            this.end   = end;
            this.step  = step;
        }

        NumberRange(int start, int end) { this(start, end, 1); }

        int start() { return start; }
        int end()   { return end; }
        int step()  { return step; }
        int size()  { return Math.max(0, (end - start) / step + 1); }

        boolean contains(int n) {
            return n >= start && n <= end && (n - start) % step == 0;
        }

        // The inner Iterator holds a reference back to the enclosing NumberRange
        // instance via the implicit 'NumberRange.this' reference, allowing it to
        // read start / end / step without copying them.
        @Override
        public Iterator<Integer> iterator() {
            return new RangeIterator();
        }

        // Non-static inner class — has access to the enclosing NumberRange
        private final class RangeIterator implements Iterator<Integer> {
            // cursor lives inside the inner class instance
            private int current = start;   // reads outer field directly

            @Override
            public boolean hasNext() {
                return current <= end;
            }

            @Override
            public Integer next() {
                if (!hasNext()) throw new NoSuchElementException();
                int value = current;
                current += step;           // reads outer 'step' field
                return value;
            }
        }

        @Override
        public String toString() {
            return "[" + start + ".." + end + " step " + step + "]";
        }
    }

    // -------------------------------------------------------------------------
    // 2. EventBus — inner Subscription
    //    Subscription is tied to the bus it was registered on (needs to remove
    //    itself from the bus's listener list when cancelled).
    //    Classic inner-class pattern: subscription carries a reference to its bus.
    // -------------------------------------------------------------------------
    static final class EventBus<T> {

        private final List<Subscription> subscriptions = new ArrayList<>();

        // Non-static inner class: each Subscription knows its parent bus
        final class Subscription {
            private final Consumer<T> handler;
            private       boolean     active = true;

            private Subscription(Consumer<T> handler) {
                this.handler = handler;
            }

            // Uses EventBus.this.subscriptions — outer class private field
            void cancel() {
                active = false;
                EventBus.this.subscriptions.remove(this);
            }

            boolean isActive() { return active; }
        }

        Subscription subscribe(Consumer<T> handler) {
            Subscription sub = new Subscription(handler);
            subscriptions.add(sub);
            return sub;
        }

        void publish(T event) {
            // snapshot copy to avoid ConcurrentModificationException if a
            // handler cancels itself during delivery
            new ArrayList<>(subscriptions)
                .forEach(s -> { if (s.active) s.handler.accept(event); });
        }

        int subscriberCount() { return subscriptions.size(); }
    }

    // -------------------------------------------------------------------------
    // 3. TextBuffer — inner Cursor
    //    Cursor holds a position inside the buffer and can read/modify it.
    //    Position is per-cursor; the text itself belongs to the outer buffer.
    // -------------------------------------------------------------------------
    static final class TextBuffer {

        private final StringBuilder text;

        TextBuffer(String initial) {
            this.text = new StringBuilder(initial);
        }

        // Non-static inner class — accesses outer 'text' field
        final class Cursor {
            private int position;

            Cursor(int position) {
                if (position < 0 || position > text.length())
                    throw new IndexOutOfBoundsException("position " + position);
                this.position = position;
            }

            char current() {
                if (position >= text.length())
                    throw new NoSuchElementException("past end of buffer");
                return text.charAt(position);   // uses outer text
            }

            void advance(int n) {
                position = Math.min(position + n, text.length());
            }

            void insert(String s) {
                text.insert(position, s);       // modifies outer text
                position += s.length();
            }

            void delete(int n) {
                int end = Math.min(position + n, text.length());
                text.delete(position, end);     // modifies outer text
            }

            int position() { return position; }
            boolean atEnd() { return position >= text.length(); }

            // Access outer instance explicitly when needed
            TextBuffer buffer() { return TextBuffer.this; }
        }

        Cursor cursor(int position) { return new Cursor(position); }
        Cursor cursor()             { return new Cursor(0); }
        String content()            { return text.toString(); }
        int    length()             { return text.length(); }

        @Override public String toString() { return text.toString(); }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void rangeDemo() {
        System.out.println("=== NumberRange (inner RangeIterator) ===");
        NumberRange evens = new NumberRange(2, 10, 2);
        System.out.print("Evens: ");
        for (int n : evens) System.out.print(n + " ");
        System.out.println();

        NumberRange countdown = new NumberRange(5, 5);  // single element
        System.out.println("Single element: " + countdown.iterator().next());
        System.out.println("Contains 6: " + evens.contains(6));
        System.out.println("Contains 7: " + evens.contains(7));
        System.out.println("Size: " + evens.size());
    }

    static void eventBusDemo() {
        System.out.println("\n=== EventBus (inner Subscription) ===");
        EventBus<String> bus = new EventBus<>();

        List<String> received = new ArrayList<>();
        EventBus<String>.Subscription sub1 = bus.subscribe(received::add);
        EventBus<String>.Subscription sub2 = bus.subscribe(e ->
            System.out.println("  handler2 got: " + e));

        bus.publish("hello");
        bus.publish("world");
        System.out.println("received by sub1: " + received);

        sub2.cancel();
        System.out.println("subscribers after cancel: " + bus.subscriberCount());

        bus.publish("after cancel");   // only sub1 still active
        System.out.println("sub1 active: " + sub1.isActive());
        System.out.println("sub2 active: " + sub2.isActive());
    }

    static void textBufferDemo() {
        System.out.println("\n=== TextBuffer (inner Cursor) ===");
        TextBuffer buf = new TextBuffer("Hello world");

        TextBuffer.Cursor c = buf.cursor(6);
        System.out.println("char at 6: " + c.current());

        c.insert("beautiful ");
        System.out.println("after insert: " + buf.content());

        TextBuffer.Cursor c2 = buf.cursor(0);
        c2.delete(5);   // removes "Hello"
        System.out.println("after delete: " + buf.content());

        System.out.println("buffer() same instance: "
            + (c2.buffer() == buf));
    }

    public static void main(String[] args) {
        rangeDemo();
        eventBusDemo();
        textBufferDemo();
    }
}
