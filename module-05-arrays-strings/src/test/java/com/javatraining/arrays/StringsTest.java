package com.javatraining.arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Strings - immutability, pool, API, StringBuilder")
class StringsTest {

    @Test
    @DisplayName("Modifying operation returns new String - original unchanged")
    void immutability() {
        String original = "hello";
        original.toUpperCase(); // result discarded
        assertEquals("hello", original);
    }

    @Test
    @DisplayName("String literals with same content share pool reference")
    void stringPoolLiterals() {
        String s1 = "java";
        String s2 = "java";
        assertSame(s1, s2, "Pool literals must be the same reference");
    }

    @Test
    @DisplayName("new String() bypasses pool - different reference, same content")
    void newStringBypassesPool() {
        String s1 = "java";
        String s2 = new String("java");
        assertNotSame(s1, s2, "new String() must be a different object");
        assertEquals(s1, s2, "But content must be equal");
    }

    @Test
    @DisplayName("intern() returns the pool reference")
    void intern() {
        String s1 = "java";
        String s2 = new String("java").intern();
        assertSame(s1, s2, "intern() must return the pool reference");
    }

    @ParameterizedTest(name = "strip(\"{0}\") = \"{1}\"")
    @CsvSource({
        "'  hello  ', hello",
        "hello, hello",
    })
    void stripRemovesWhitespace(String input, String expected) {
        assertEquals(expected, input.strip());
    }

    @Test
    @DisplayName("strip removes tabs and newlines (Unicode whitespace)")
    void stripUnicodeWhitespace() {
        assertEquals("hello", "\t\n hello \t".strip());
    }

    @Test
    @DisplayName("isBlank returns true for whitespace-only strings")
    void isBlank() {
        assertTrue("   ".isBlank());
        assertTrue("\t\n".isBlank());
        assertFalse("a ".isBlank());
        assertFalse(" a".isBlank());
    }

    @Test
    @DisplayName("split with -1 limit preserves trailing empty strings")
    void splitWithLimit() {
        String[] noLimit = "a,,b,,".split(",");
        String[] withLimit = "a,,b,,".split(",", -1);
        // default drops trailing empties
        assertEquals(3, noLimit.length);
        // -1 keeps them
        assertEquals(5, withLimit.length);
    }

    @Test
    @DisplayName("String.join concatenates with delimiter")
    void join() {
        assertEquals("a, b, c", String.join(", ", "a", "b", "c"));
        assertEquals("",         String.join(", "));
    }

    @Test
    @DisplayName("StringBuilder chaining builds string correctly")
    void stringBuilder() {
        String result = new StringBuilder()
            .append("Hello")
            .append(", ")
            .append("World")
            .append('!')
            .toString();
        assertEquals("Hello, World!", result);
    }

    @Test
    @DisplayName("StringBuilder.reverse reverses correctly")
    void reverse() {
        assertEquals("edcba", new StringBuilder("abcde").reverse().toString());
    }

    @Test
    @DisplayName("StringBuilder.insert adds at correct position")
    void insert() {
        StringBuilder sb = new StringBuilder("Hello World");
        sb.insert(5, " Beautiful");
        assertEquals("Hello Beautiful World", sb.toString());
    }

    @Test
    @DisplayName("repeat produces correct number of copies")
    void repeat() {
        assertEquals("abababab", "ab".repeat(4));
        assertEquals("",          "x".repeat(0));
    }

    @Test
    @DisplayName("lines() splits on any line terminator")
    void lines() {
        long count = "line1\nline2\r\nline3\rline4".lines().count();
        assertEquals(4, count);
    }

    @Test
    @DisplayName("format produces correctly padded output")
    void format() {
        String result = String.format("%-10s %5d", "item", 42);
        assertEquals("item          42", result);
    }
}
