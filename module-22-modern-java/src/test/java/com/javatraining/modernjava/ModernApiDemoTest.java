package com.javatraining.modernjava;

import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ModernApiDemo")
class ModernApiDemoTest {

    @Nested
    @DisplayName("Immutable collections")
    class ImmutableCollections {
        @Test void immutable_list_has_correct_elements() {
            List<String> list = ModernApiDemo.immutableList("a", "b", "c");
            assertEquals(3, list.size());
            assertTrue(list.contains("a"));
        }

        @Test void immutable_list_is_unmodifiable() {
            assertThrows(UnsupportedOperationException.class, () ->
                ModernApiDemo.immutableList("x").add("y"));
        }

        @Test void immutable_set_no_duplicates() {
            Set<Integer> set = ModernApiDemo.immutableSet(1, 2, 3);
            assertEquals(3, set.size());
        }

        @Test void immutable_map_values() {
            Map<String, Integer> map = ModernApiDemo.immutableMap("a", 1, "b", 2);
            assertEquals(1, map.get("a"));
            assertEquals(2, map.get("b"));
        }

        @Test void defensive_copy_is_unmodifiable() {
            List<String> orig = new ArrayList<>(List.of("x", "y"));
            List<String> copy = ModernApiDemo.defensiveCopy(orig);
            orig.add("z");
            assertEquals(2, copy.size());  // copy is independent
            assertThrows(UnsupportedOperationException.class, () -> copy.add("w"));
        }
    }

    @Nested
    @DisplayName("String API (Java 11+)")
    class StringApi {
        @Test void isBlank_whitespace_only() {
            assertTrue(ModernApiDemo.isBlankLine("   "));
        }

        @Test void isBlank_empty() {
            assertTrue(ModernApiDemo.isBlankLine(""));
        }

        @Test void isBlank_non_blank() {
            assertFalse(ModernApiDemo.isBlankLine("hi"));
        }

        @Test void strip_removes_whitespace() {
            assertEquals("hello", ModernApiDemo.stripUnicode("  hello  "));
        }

        @Test void strip_leading() {
            assertEquals("hello  ", ModernApiDemo.stripLeading("  hello  "));
        }

        @Test void strip_trailing() {
            assertEquals("  hello", ModernApiDemo.stripTrailing("  hello  "));
        }

        @Test void repeat() {
            assertEquals("abababab", ModernApiDemo.repeat("ab", 4));
        }

        @Test void split_lines() {
            List<String> lines = ModernApiDemo.splitLines("a\nb\nc");
            assertEquals(List.of("a", "b", "c"), lines);
        }

        @Test void transform_chain() {
            assertEquals("HELLO", ModernApiDemo.transformChain("  hello  "));
        }
    }

    @Nested
    @DisplayName("Optional additions (Java 9–11)")
    class OptionalAdditions {
        @Test void first_non_empty_returns_primary() {
            Optional<String> result = ModernApiDemo.firstNonEmpty(
                Optional.of("primary"), Optional.of("fallback"));
            assertEquals("primary", result.get());
        }

        @Test void first_non_empty_falls_back() {
            Optional<String> result = ModernApiDemo.firstNonEmpty(
                Optional.of("  "), Optional.of("fallback"));
            assertEquals("fallback", result.get());
        }

        @Test void first_non_empty_both_empty() {
            Optional<String> result = ModernApiDemo.firstNonEmpty(
                Optional.empty(), Optional.empty());
            assertTrue(result.isEmpty());
        }

        @Test void describe_present() {
            assertEquals("present: hi",
                ModernApiDemo.describeOptional(Optional.of("hi")));
        }

        @Test void describe_absent() {
            assertEquals("absent", ModernApiDemo.describeOptional(Optional.empty()));
        }

        @Test void flatten_optionals_skips_empty() {
            List<Optional<String>> opts = List.of(
                Optional.of("a"), Optional.empty(), Optional.of("b"));
            assertEquals(List.of("a", "b"), ModernApiDemo.flattenOptionals(opts));
        }

        @Test void isEmpty_on_empty() {
            assertTrue(ModernApiDemo.isEmpty(Optional.empty()));
        }

        @Test void isEmpty_on_present() {
            assertFalse(ModernApiDemo.isEmpty(Optional.of("x")));
        }
    }

    @Nested
    @DisplayName("Stream additions (Java 9+)")
    class StreamAdditions {
        @Test void takeWhile() {
            List<Integer> result = ModernApiDemo.takeWhileLessThan(
                List.of(1, 2, 3, 4, 5), 4);
            assertEquals(List.of(1, 2, 3), result);
        }

        @Test void dropWhile() {
            List<Integer> result = ModernApiDemo.dropWhileLessThan(
                List.of(1, 2, 3, 4, 5), 3);
            assertEquals(List.of(3, 4, 5), result);
        }

        @Test void generate_range() {
            assertEquals(List.of(0, 1, 2, 3, 4), ModernApiDemo.generateRange(0, 5));
        }

        @Test void ofNullable_with_value() {
            assertEquals(List.of("HELLO"), ModernApiDemo.processNullable("hello"));
        }

        @Test void ofNullable_with_null() {
            assertTrue(ModernApiDemo.processNullable(null).isEmpty());
        }

        @Test void toList_stream() {
            List<Integer> result = ModernApiDemo.toList(Stream.of(1, 2, 3));
            assertEquals(List.of(1, 2, 3), result);
        }
    }

    @Nested
    @DisplayName("var local type inference")
    class VarInference {
        @Test void group_by_length() {
            Map<String, List<Integer>> groups =
                ModernApiDemo.groupByLength(List.of("a", "bb", "ccc", "dd"));
            assertTrue(groups.containsKey("1"));
            assertTrue(groups.containsKey("2"));
            assertTrue(groups.containsKey("3"));
        }
    }

    @Nested
    @DisplayName("Text blocks")
    class TextBlocks {
        @Test void json_template_contains_name_and_age() {
            String json = ModernApiDemo.jsonTemplate("Alice", 30);
            assertTrue(json.contains("\"name\": \"Alice\""));
            assertTrue(json.contains("\"age\": 30"));
        }

        @Test void html_snippet_contains_p_tag() {
            String html = ModernApiDemo.htmlSnippet();
            assertTrue(html.contains("<p>Hello, world!</p>"));
        }
    }

    @Nested
    @DisplayName("Sequenced collections (Java 21)")
    class SequencedCollections {
        @Test void get_first() {
            assertEquals("a",
                ModernApiDemo.getFirstElement(new ArrayList<>(List.of("a", "b", "c"))));
        }

        @Test void get_last() {
            assertEquals("c",
                ModernApiDemo.getLastElement(new ArrayList<>(List.of("a", "b", "c"))));
        }

        @Test void reversed() {
            List<String> rev = ModernApiDemo.reversed(
                new ArrayList<>(List.of("a", "b", "c")));
            assertEquals(List.of("c", "b", "a"), rev);
        }

        @Test void first_map_entry() {
            LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
            map.put("x", 1); map.put("y", 2);
            Map.Entry<String, Integer> entry = ModernApiDemo.firstMapEntry(map);
            assertEquals("x", entry.getKey());
        }
    }
}
