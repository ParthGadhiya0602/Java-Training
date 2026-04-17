package com.javatraining.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MethodReferences")
class MethodReferencesTest {

    @Nested
    @DisplayName("Static method references")
    class Static {
        @Test void staticRef_doubles_value() {
            assertEquals(10, MethodReferences.staticRef().applyAsInt(5));
        }
        @Test void naturalOrderComparator_orders_correctly() {
            assertTrue(MethodReferences.naturalOrderComparator().compare(1, 2) < 0);
            assertTrue(MethodReferences.naturalOrderComparator().compare(2, 1) > 0);
            assertEquals(0, MethodReferences.naturalOrderComparator().compare(3, 3));
        }
        @Test void parseInts_converts_strings() {
            assertEquals(List.of(1, 2, 3), MethodReferences.parseInts(List.of("1", "2", "3")));
        }
    }

    @Nested
    @DisplayName("Bound instance method references")
    class Bound {
        @Test void boundToStringRef_returns_sb_content() {
            StringBuilder sb = new StringBuilder("hello");
            Supplier<String> ref = MethodReferences.boundToStringRef(sb);
            assertEquals("hello", ref.get());
            sb.append(" world");
            assertEquals("hello world", ref.get());
        }
        @Test void printlnRef_is_callable_consumer() {
            Consumer<String> c = MethodReferences.printlnRef();
            assertNotNull(c);
            assertDoesNotThrow(() -> c.accept("test"));
        }
    }

    @Nested
    @DisplayName("Unbound instance method references")
    class Unbound {
        @Test void toUpperCaseRef_uppercases() {
            assertEquals("HELLO", MethodReferences.toUpperCaseRef().apply("hello"));
        }
        @Test void sortByLength_sorts_ascending() {
            List<String> sorted = MethodReferences.sortByLength(List.of("banana", "fig", "apple"));
            assertEquals(List.of("fig", "apple", "banana"), sorted);
        }
        @Test void containsRef_true_when_contains() {
            assertTrue(MethodReferences.containsRef().apply("hello world", "world"));
        }
        @Test void containsRef_false_when_absent() {
            assertFalse(MethodReferences.containsRef().apply("hello", "xyz"));
        }
    }

    @Nested
    @DisplayName("Constructor references")
    class Constructor {
        @Test void sbSupplier_creates_empty_builder() {
            assertEquals("", MethodReferences.sbSupplier().get().toString());
        }
        @Test void sbFromString_creates_builder_with_content() {
            assertEquals("hi", MethodReferences.sbFromString().apply("hi").toString());
        }
        @Test void intArrayFactory_creates_array_of_given_size() {
            int[] arr = MethodReferences.intArrayFactory().apply(5);
            assertEquals(5, arr.length);
        }
    }

    @Nested
    @DisplayName("Pipeline combining all kinds")
    class Pipeline {
        @Test void processNumbers_filters_parses_doubles_sorts() {
            List<String> raw = List.of("3", " ", "1", "bad", "2");
            assertEquals(List.of(2, 4, 6), MethodReferences.processNumbers(raw));
        }
        @Test void processNumbers_all_invalid_returns_empty() {
            assertTrue(MethodReferences.processNumbers(List.of("a", "b")).isEmpty());
        }
        @Test void processNumbers_blank_entries_excluded() {
            assertEquals(List.of(10), MethodReferences.processNumbers(List.of("  ", "5")));
        }
    }

    @Nested
    @DisplayName("Comparator composition")
    class ComparatorComposition {
        @Test void sortPeople_by_age_then_name() {
            List<MethodReferences.Person> people = List.of(
                new MethodReferences.Person("Bob",   30),
                new MethodReferences.Person("Alice", 25),
                new MethodReferences.Person("Carol", 25)
            );
            List<MethodReferences.Person> sorted = MethodReferences.sortPeople(people);
            assertEquals("Alice", sorted.get(0).name());
            assertEquals("Carol", sorted.get(1).name());
            assertEquals("Bob",   sorted.get(2).name());
        }
    }
}
