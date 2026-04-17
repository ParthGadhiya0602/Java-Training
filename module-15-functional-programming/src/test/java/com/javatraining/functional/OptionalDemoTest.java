package com.javatraining.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OptionalDemo")
class OptionalDemoTest {

    @Nested
    @DisplayName("Creation")
    class Creation {
        @Test void wrap_non_null_returns_present() {
            assertTrue(OptionalDemo.wrap("hello").isPresent());
        }
        @Test void wrap_null_returns_empty() {
            assertTrue(OptionalDemo.wrap(null).isEmpty());
        }
        @Test void safeDivide_normal() {
            assertEquals(Optional.of(5), OptionalDemo.safeDivide(10, 2));
        }
        @Test void safeDivide_by_zero_returns_empty() {
            assertTrue(OptionalDemo.safeDivide(10, 0).isEmpty());
        }
    }

    @Nested
    @DisplayName("map and flatMap")
    class MapAndFlatMap {
        @Test void lengthOrMinus1_present() {
            assertEquals(5, OptionalDemo.lengthOrMinus1(Optional.of("hello")));
        }
        @Test void lengthOrMinus1_empty_returns_minus1() {
            assertEquals(-1, OptionalDemo.lengthOrMinus1(Optional.empty()));
        }
        @Test void parseIntSafe_valid() {
            assertEquals(Optional.of(42), OptionalDemo.parseIntSafe("42"));
        }
        @Test void parseIntSafe_invalid_returns_empty() {
            assertTrue(OptionalDemo.parseIntSafe("abc").isEmpty());
        }
        @Test void lookupAndParse_key_present_value_parseable() {
            Map<String, String> m = Map.of("k", "99");
            assertEquals(Optional.of(99), OptionalDemo.lookupAndParse(m, "k"));
        }
        @Test void lookupAndParse_missing_key_returns_empty() {
            assertTrue(OptionalDemo.lookupAndParse(Map.of(), "missing").isEmpty());
        }
        @Test void lookupAndParse_unparseable_value_returns_empty() {
            Map<String, String> m = Map.of("k", "NaN");
            assertTrue(OptionalDemo.lookupAndParse(m, "k").isEmpty());
        }
    }

    @Nested
    @DisplayName("filter")
    class Filter {
        @Test void nonBlank_non_blank_string_present() {
            assertEquals(Optional.of("hi"), OptionalDemo.nonBlank("hi"));
        }
        @Test void nonBlank_blank_string_empty() {
            assertTrue(OptionalDemo.nonBlank("   ").isEmpty());
        }
        @Test void nonBlank_null_empty() {
            assertTrue(OptionalDemo.nonBlank(null).isEmpty());
        }
        @Test void positiveEven_match() {
            assertEquals(Optional.of(4), OptionalDemo.positiveEven(4));
        }
        @Test void positiveEven_odd_returns_empty() {
            assertTrue(OptionalDemo.positiveEven(3).isEmpty());
        }
        @Test void positiveEven_negative_returns_empty() {
            assertTrue(OptionalDemo.positiveEven(-2).isEmpty());
        }
    }

    @Nested
    @DisplayName("Terminal operations")
    class Terminal {
        @Test void orElseDemo_present_returns_value() {
            assertEquals("x", OptionalDemo.orElseDemo(Optional.of("x"), "default"));
        }
        @Test void orElseDemo_empty_returns_default() {
            assertEquals("default", OptionalDemo.orElseDemo(Optional.empty(), "default"));
        }
        @Test void orElseGetDemo_present_does_not_call_supplier() {
            assertEquals("hello", OptionalDemo.orElseGetDemo(Optional.of("hello")));
        }
        @Test void orElseGetDemo_empty_calls_supplier() {
            assertTrue(OptionalDemo.orElseGetDemo(Optional.empty()).startsWith("generated-"));
        }
        @Test void orElseThrowDemo_present_returns_value() {
            assertEquals("ok", OptionalDemo.orElseThrowDemo(Optional.of("ok")));
        }
        @Test void orElseThrowDemo_empty_throws() {
            assertThrows(IllegalStateException.class,
                () -> OptionalDemo.orElseThrowDemo(Optional.empty()));
        }
        @Test void describeOptional_present() {
            assertEquals("present: foo", OptionalDemo.describeOptional(Optional.of("foo")));
        }
        @Test void describeOptional_empty() {
            assertEquals("empty", OptionalDemo.describeOptional(Optional.empty()));
        }
    }

    @Nested
    @DisplayName("or and stream")
    class OrAndStream {
        @Test void firstNonEmpty_primary_present() {
            assertEquals(Optional.of("a"),
                OptionalDemo.firstNonEmpty(Optional.of("a"), Optional.of("b")));
        }
        @Test void firstNonEmpty_primary_empty_uses_fallback() {
            assertEquals(Optional.of("b"),
                OptionalDemo.firstNonEmpty(Optional.empty(), Optional.of("b")));
        }
        @Test void firstNonEmpty_both_empty_returns_empty() {
            assertTrue(OptionalDemo.firstNonEmpty(Optional.empty(), Optional.empty()).isEmpty());
        }
        @Test void parseAll_mixed_input() {
            List<Integer> result = OptionalDemo.parseAll(List.of("1", "bad", "3", "nope", "5"));
            assertEquals(List.of(1, 3, 5), result);
        }
        @Test void parseAll_all_invalid_returns_empty_list() {
            assertTrue(OptionalDemo.parseAll(List.of("a", "b")).isEmpty());
        }
    }

    @Nested
    @DisplayName("Chained pipeline")
    class Pipeline {
        @Test void resolveUsername_found() {
            Map<String, String> profiles = Map.of("u1", "  Alice  ");
            assertEquals("alice", OptionalDemo.resolveUsername(profiles, "u1"));
        }
        @Test void resolveUsername_missing_id_returns_anonymous() {
            assertEquals("anonymous", OptionalDemo.resolveUsername(Map.of(), "u99"));
        }
        @Test void resolveUsername_null_id_returns_anonymous() {
            assertEquals("anonymous", OptionalDemo.resolveUsername(Map.of(), null));
        }
        @Test void resolveUsername_blank_id_returns_anonymous() {
            assertEquals("anonymous", OptionalDemo.resolveUsername(Map.of(), "  "));
        }
    }
}
