package com.javatraining.build.gradle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnglishGreeterTest {

    Greeter greeter = new EnglishGreeter();

    @Test
    void greets_with_name() {
        assertEquals("Hello, World!", greeter.greet("World"));
    }

    @Test
    void trims_whitespace_from_name() {
        assertEquals("Hello, Alice!", greeter.greet("  Alice  "));
    }

    @Test
    void blank_name_throws() {
        assertThrows(IllegalArgumentException.class, () -> greeter.greet(""));
        assertThrows(IllegalArgumentException.class, () -> greeter.greet("  "));
    }

    @Test
    void null_name_throws() {
        assertThrows(IllegalArgumentException.class, () -> greeter.greet(null));
    }
}
