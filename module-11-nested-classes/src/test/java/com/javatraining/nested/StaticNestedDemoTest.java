package com.javatraining.nested;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StaticNestedDemoTest {

    // ── LinkedStack ───────────────────────────────────────────────────────────

    @Nested
    class LinkedStackTests {

        StaticNestedDemo.LinkedStack<Integer> stack;

        @BeforeEach
        void setUp() { stack = new StaticNestedDemo.LinkedStack<>(); }

        @Test
        void newStack_isEmpty() {
            assertTrue(stack.isEmpty());
            assertEquals(0, stack.size());
        }

        @Test
        void push_increments_size() {
            stack.push(1);
            stack.push(2);
            assertEquals(2, stack.size());
            assertFalse(stack.isEmpty());
        }

        @Test
        void peek_returns_top_without_removing() {
            stack.push(10);
            stack.push(20);
            assertEquals(20, stack.peek());
            assertEquals(2, stack.size());   // not removed
        }

        @Test
        void pop_returns_LIFO_order() {
            stack.push(1);
            stack.push(2);
            stack.push(3);
            assertEquals(3, stack.pop());
            assertEquals(2, stack.pop());
            assertEquals(1, stack.pop());
        }

        @Test
        void pop_on_empty_throws() {
            assertThrows(NoSuchElementException.class, stack::pop);
        }

        @Test
        void peek_on_empty_throws() {
            assertThrows(NoSuchElementException.class, stack::peek);
        }

        @Test
        void toList_returns_top_first() {
            stack.push(1);
            stack.push(2);
            stack.push(3);
            assertEquals(List.of(3, 2, 1), stack.toList());
        }

        @Test
        void toString_shows_top_first() {
            stack.push(1);
            stack.push(2);
            assertEquals("[2, 1]", stack.toString());
        }
    }

    // ── ReadOnlyMap ───────────────────────────────────────────────────────────

    @Nested
    class ReadOnlyMapTests {

        StaticNestedDemo.ReadOnlyMap<String, Integer> map;

        @BeforeEach
        void setUp() {
            map = new StaticNestedDemo.ReadOnlyMap<>(
                new StaticNestedDemo.ReadOnlyMap.Entry<String, Integer>("OK",      200),
                new StaticNestedDemo.ReadOnlyMap.Entry<String, Integer>("Created", 201),
                new StaticNestedDemo.ReadOnlyMap.Entry<String, Integer>("NotFound",404)
            );
        }

        @Test
        void size_matches_entry_count() { assertEquals(3, map.size()); }

        @Test
        void get_returns_present_value() {
            assertEquals(Optional.of(200), map.get("OK"));
        }

        @Test
        void get_returns_empty_for_missing_key() {
            assertTrue(map.get("Unknown").isEmpty());
        }

        @Test
        void contains_key_present() { assertTrue(map.contains("Created")); }

        @Test
        void contains_key_absent()  { assertFalse(map.contains("Moved")); }

        @Test
        void entries_preserves_insertion_order() {
            List<String> keys = map.entries().stream()
                .map(StaticNestedDemo.ReadOnlyMap.Entry::key)
                .toList();
            assertEquals(List.of("OK", "Created", "NotFound"), keys);
        }

        @Test
        void entry_null_key_throws() {
            assertThrows(NullPointerException.class,
                () -> new StaticNestedDemo.ReadOnlyMap.Entry<>(null, 200));
        }

        @Test
        void entry_null_value_throws() {
            assertThrows(NullPointerException.class,
                () -> new StaticNestedDemo.ReadOnlyMap.Entry<>("x", null));
        }
    }

    // ── HttpClient ────────────────────────────────────────────────────────────

    @Nested
    class HttpClientTests {

        @Test
        void builder_defaults_to_GET() {
            StaticNestedDemo.HttpClient.Request req =
                new StaticNestedDemo.HttpClient.Request.Builder()
                    .get("https://example.com")
                    .build();
            assertEquals("GET", req.method());
        }

        @Test
        void builder_post_sets_method() {
            StaticNestedDemo.HttpClient.Request req =
                new StaticNestedDemo.HttpClient.Request.Builder()
                    .post("https://example.com/create")
                    .build();
            assertEquals("POST", req.method());
        }

        @Test
        void headers_are_stored() {
            StaticNestedDemo.HttpClient.Request req =
                new StaticNestedDemo.HttpClient.Request.Builder()
                    .get("https://example.com")
                    .header("Accept", "application/json")
                    .build();
            assertEquals("application/json", req.headers().get("Accept"));
        }

        @Test
        void blank_url_throws() {
            assertThrows(IllegalStateException.class,
                () -> new StaticNestedDemo.HttpClient.Request.Builder().build());
        }

        @Test
        void send_returns_200_success() {
            StaticNestedDemo.HttpClient client = new StaticNestedDemo.HttpClient();
            StaticNestedDemo.HttpClient.Request req =
                new StaticNestedDemo.HttpClient.Request.Builder()
                    .get("https://example.com")
                    .build();
            StaticNestedDemo.HttpClient.Response resp = client.send(req);
            assertEquals(200, resp.statusCode());
            assertTrue(resp.isSuccess());
        }

        @Test
        void response_isSuccess_false_for_4xx() {
            StaticNestedDemo.HttpClient.Response resp =
                new StaticNestedDemo.HttpClient.Response(404, "Not Found", Map.of());
            assertFalse(resp.isSuccess());
        }
    }
}
