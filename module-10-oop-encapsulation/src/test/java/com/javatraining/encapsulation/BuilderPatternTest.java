package com.javatraining.encapsulation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuilderPatternTest {

    // -----------------------------------------------------------------------
    // Pizza — classic builder
    // -----------------------------------------------------------------------
    @Test
    void required_fields_set_correctly() {
        BuilderPattern.Pizza p = new BuilderPattern.Pizza.Builder("large", "thin").build();
        assertEquals("LARGE", p.size());
        assertEquals("THIN",  p.crust());
    }

    @Test
    void optional_fields_default_off() {
        BuilderPattern.Pizza p = new BuilderPattern.Pizza.Builder("medium", "thick").build();
        assertFalse(p.extraCheese());
        assertFalse(p.extraSauce());
        assertTrue(p.toppings().isEmpty());
        assertNull(p.notes());
    }

    @Test
    void optional_fields_set_via_builder() {
        BuilderPattern.Pizza p = new BuilderPattern.Pizza.Builder("small", "thin")
            .extraCheese()
            .extraSauce()
            .topping("Mushroom")
            .topping("Onion")
            .notes("Less spicy")
            .build();

        assertTrue(p.extraCheese());
        assertTrue(p.extraSauce());
        assertEquals(2, p.toppings().size());
        assertEquals("Less spicy", p.notes());
    }

    @Test
    void toppings_returns_defensive_copy() {
        BuilderPattern.Pizza p = new BuilderPattern.Pizza.Builder("large", "thin")
            .topping("Cheese")
            .build();
        p.toppings().add("Hacked");    // mutate returned copy
        assertEquals(1, p.toppings().size());  // original unchanged
    }

    @Test
    void copy_builder_creates_independent_instance() {
        BuilderPattern.Pizza original = new BuilderPattern.Pizza.Builder("large", "thin")
            .extraCheese()
            .topping("Tomato")
            .build();
        BuilderPattern.Pizza copy = original.toBuilder().noExtraCheese().build();

        assertTrue(original.extraCheese());   // original unchanged
        assertFalse(copy.extraCheese());
        assertEquals(original.toppings(), copy.toppings());
    }

    @Test
    void blank_size_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> new BuilderPattern.Pizza.Builder("", "thin"));
    }

    // -----------------------------------------------------------------------
    // Employee — step builder
    // -----------------------------------------------------------------------
    @Test
    void required_fields_enforced_by_chain() {
        BuilderPattern.Employee e = BuilderPattern.Employee.builder()
            .name("Alice")
            .email("alice@x.com")
            .department("Eng")
            .build();

        assertEquals("Alice",       e.name());
        assertEquals("alice@x.com", e.email());
        assertEquals("Eng",         e.department());
    }

    @Test
    void optional_fields_default_values() {
        BuilderPattern.Employee e = BuilderPattern.Employee.builder()
            .name("Bob")
            .email("bob@x.com")
            .department("HR")
            .build();

        assertNull(e.phone());
        assertNull(e.title());
        assertEquals(0.0,   e.salary(), 1e-9);
        assertFalse(e.remote());
    }

    @Test
    void optional_fields_set() {
        BuilderPattern.Employee e = BuilderPattern.Employee.builder()
            .name("Carol")
            .email("carol@x.com")
            .department("Sales")
            .phone("+91-9999999999")
            .title("Manager")
            .salary(90_000)
            .remote(true)
            .build();

        assertEquals("+91-9999999999", e.phone());
        assertEquals("Manager",        e.title());
        assertEquals(90_000.0,         e.salary(), 1e-9);
        assertTrue(e.remote());
    }

    @Test
    void blank_name_throws_in_step_builder() {
        assertThrows(IllegalArgumentException.class, () ->
            BuilderPattern.Employee.builder()
                .name("")
                .email("x@y.com")
                .department("D")
                .build());
    }

    // -----------------------------------------------------------------------
    // HttpRequest — builder with validation + copy builder
    // -----------------------------------------------------------------------
    @Test
    void get_request_defaults() {
        BuilderPattern.HttpRequest req = new BuilderPattern.HttpRequest.Builder()
            .get("https://api.example.com/items")
            .build();
        assertEquals("GET", req.method());
        assertEquals("https://api.example.com/items", req.url());
        assertEquals(5_000, req.timeoutMs());
    }

    @Test
    void post_with_json_body() {
        BuilderPattern.HttpRequest req = new BuilderPattern.HttpRequest.Builder()
            .post("https://api.example.com/items")
            .json()
            .body("{\"name\":\"test\"}")
            .build();
        assertEquals("POST", req.method());
        assertEquals("application/json", req.headers().get("Content-Type"));
        assertNotNull(req.body());
    }

    @Test
    void bearer_token_sets_authorization_header() {
        BuilderPattern.HttpRequest req = new BuilderPattern.HttpRequest.Builder()
            .get("https://api.example.com")
            .bearer("token123")
            .build();
        assertEquals("Bearer token123", req.headers().get("Authorization"));
    }

    @Test
    void missing_url_throws() {
        assertThrows(IllegalStateException.class,
            () -> new BuilderPattern.HttpRequest.Builder().build());
    }

    @Test
    void copy_builder_changes_url_and_method() {
        BuilderPattern.HttpRequest post = new BuilderPattern.HttpRequest.Builder()
            .post("https://api.example.com/items")
            .json()
            .bearer("tok")
            .body("{}")
            .build();

        BuilderPattern.HttpRequest put = post.toBuilder()
            .put("https://api.example.com/items/1")
            .body("{\"updated\":true}")
            .build();

        assertEquals("PUT", put.method());
        assertEquals("https://api.example.com/items/1", put.url());
        // Headers preserved from original
        assertEquals("application/json", put.headers().get("Content-Type"));
        // Original unchanged
        assertEquals("POST", post.method());
    }

    @Test
    void headers_returns_unmodifiable_map() {
        BuilderPattern.HttpRequest req = new BuilderPattern.HttpRequest.Builder()
            .get("https://x.com")
            .header("X-Custom", "value")
            .build();
        Map<String, String> headers = req.headers();
        assertThrows(UnsupportedOperationException.class, () -> headers.put("X", "Y"));
    }
}
