package com.javatraining.nested;

import java.util.*;

/**
 * TOPIC: Static nested classes
 *
 * A static nested class is declared with the 'static' keyword inside another class.
 * It has NO implicit reference to the outer class instance.
 * It CAN access the outer class's private static members.
 *
 * Use a static nested class when:
 *   • The class is logically tied to the outer but doesn't need outer-instance state
 *   • You want to group helper types with the type they serve
 *   • You are implementing the Builder pattern (builder lives inside the target type)
 *
 * Instantiation: new Outer.Nested() - no outer instance required.
 */
public class StaticNestedDemo {

    // -------------------------------------------------------------------------
    // 1. Linked list with a private static nested Node
    //    Node is an implementation detail - hidden from outside world.
    //    Making it static avoids the memory leak of holding outer references.
    // -------------------------------------------------------------------------
    static final class LinkedStack<T> {

        // private - callers never see this class; it's an internal detail
        private static final class Node<T> {
            final T       value;
            final Node<T> next;

            Node(T value, Node<T> next) {
                this.value = value;
                this.next  = next;
            }
        }

        private Node<T> top  = null;
        private int     size = 0;

        void push(T value) {
            top  = new Node<>(value, top);
            size++;
        }

        T pop() {
            if (isEmpty()) throw new NoSuchElementException("Stack is empty");
            T value = top.value;
            top     = top.next;
            size--;
            return value;
        }

        T peek() {
            if (isEmpty()) throw new NoSuchElementException("Stack is empty");
            return top.value;
        }

        boolean isEmpty() { return top == null; }
        int     size()    { return size; }

        List<T> toList() {
            List<T> list = new ArrayList<>();
            for (Node<T> n = top; n != null; n = n.next) list.add(n.value);
            return list;
        }

        @Override
        public String toString() { return toList().toString(); }
    }

    // -------------------------------------------------------------------------
    // 2. Immutable Key-Value Entry - static nested record (Java 16+)
    //    Used as the building block of a simple read-only map.
    // -------------------------------------------------------------------------
    static final class ReadOnlyMap<K, V> {

        // Public static nested record - part of the public API surface
        record Entry<K, V>(K key, V value) {
            Entry {
                Objects.requireNonNull(key,   "key");
                Objects.requireNonNull(value, "value");
            }
        }

        private final Map<K, V> store;

        @SafeVarargs
        ReadOnlyMap(Entry<K, V>... entries) {
            Map<K, V> m = new LinkedHashMap<>();
            for (Entry<K, V> e : entries) m.put(e.key(), e.value());
            this.store = Collections.unmodifiableMap(m);
        }

        Optional<V> get(K key)   { return Optional.ofNullable(store.get(key)); }
        boolean     contains(K k){ return store.containsKey(k); }
        int         size()       { return store.size(); }

        // Returns a stream of entries in insertion order
        List<Entry<K, V>> entries() {
            return store.entrySet().stream()
                .map(e -> new Entry<>(e.getKey(), e.getValue()))
                .toList();
        }

        @Override public String toString() { return store.toString(); }
    }

    // -------------------------------------------------------------------------
    // 3. Request / Response - static nested classes as a related cluster
    //    HTTP-style request/response pair; both tightly scoped to HttpClient.
    // -------------------------------------------------------------------------
    static final class HttpClient {

        // Static nested - models a request; no need to touch the client instance
        static final class Request {
            private final String              method;
            private final String              url;
            private final Map<String, String> headers;
            private final String              body;

            private Request(Builder b) {
                this.method  = b.method;
                this.url     = b.url;
                this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(b.headers));
                this.body    = b.body;
            }

            String              method()  { return method; }
            String              url()     { return url; }
            Map<String, String> headers() { return headers; }
            String              body()    { return body; }

            @Override
            public String toString() {
                return method + " " + url + " headers=" + headers;
            }

            // Builder is itself a static nested class inside Request
            static final class Builder {
                private String              method  = "GET";
                private String              url;
                private Map<String, String> headers = new LinkedHashMap<>();
                private String              body    = null;

                Builder get(String url)  { method = "GET";  this.url = url; return this; }
                Builder post(String url) { method = "POST"; this.url = url; return this; }
                Builder header(String k, String v) { headers.put(k, v);    return this; }
                Builder body(String b)   { body = b;                        return this; }

                Request build() {
                    if (url == null || url.isBlank())
                        throw new IllegalStateException("url required");
                    return new Request(this);
                }
            }
        }

        // Static nested - models a response
        static final class Response {
            private final int    statusCode;
            private final String body;
            private final Map<String, String> headers;

            Response(int statusCode, String body, Map<String, String> headers) {
                this.statusCode = statusCode;
                this.body       = body;
                this.headers    = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
            }

            int    statusCode() { return statusCode; }
            String body()       { return body; }
            Map<String, String> headers() { return headers; }
            boolean isSuccess() { return statusCode >= 200 && statusCode < 300; }

            @Override
            public String toString() {
                return "Response{status=" + statusCode + ", body=" + body + "}";
            }
        }

        // Simulated send (no real network)
        Response send(Request request) {
            // In production: open connection, write headers, read response
            Map<String, String> respHeaders = Map.of(
                "Content-Type", "application/json",
                "X-Request-Id", UUID.randomUUID().toString()
            );
            return new Response(200, "{\"status\":\"ok\"}", respHeaders);
        }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void linkedStackDemo() {
        System.out.println("=== LinkedStack (private static nested Node) ===");
        LinkedStack<String> stack = new LinkedStack<>();
        stack.push("first");
        stack.push("second");
        stack.push("third");

        System.out.println("Stack: " + stack);
        System.out.println("peek: " + stack.peek());
        System.out.println("pop:  " + stack.pop());
        System.out.println("pop:  " + stack.pop());
        System.out.println("size: " + stack.size());

        try { new LinkedStack<>().pop(); }
        catch (NoSuchElementException e) { System.out.println("Caught: " + e.getMessage()); }
    }

    static void readOnlyMapDemo() {
        System.out.println("\n=== ReadOnlyMap (static nested Entry) ===");
        ReadOnlyMap<String, Integer> httpCodes = new ReadOnlyMap<>(
            new ReadOnlyMap.Entry<>("OK",              200),
            new ReadOnlyMap.Entry<>("Created",         201),
            new ReadOnlyMap.Entry<>("Not Found",       404),
            new ReadOnlyMap.Entry<>("Internal Error",  500)
        );

        System.out.println("size: " + httpCodes.size());
        System.out.println("get(OK): " + httpCodes.get("OK").orElse(-1));
        System.out.println("contains(404): " + httpCodes.contains("Not Found"));
        System.out.println("entries:");
        httpCodes.entries().forEach(e ->
            System.out.printf("  %-16s → %d%n", e.key(), e.value()));
    }

    static void httpClientDemo() {
        System.out.println("\n=== HttpClient (static nested Request/Response) ===");
        HttpClient client = new HttpClient();

        HttpClient.Request req = new HttpClient.Request.Builder()
            .get("https://api.example.com/users")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer token123")
            .build();

        System.out.println("Request:  " + req);

        HttpClient.Response resp = client.send(req);
        System.out.println("Response: " + resp);
        System.out.println("Success:  " + resp.isSuccess());
    }

    public static void main(String[] args) {
        linkedStackDemo();
        readOnlyMapDemo();
        httpClientDemo();
    }
}
