package com.javatraining.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FunctionalInterfaces")
class FunctionalInterfacesTest {

    @Nested
    @DisplayName("Function composition")
    class FunctionComposition {
        @Test void trimThenUpper_trims_and_uppercases() {
            assertEquals("HELLO", FunctionalInterfaces.trimThenUpper().apply("  hello  "));
        }
        @Test void squareThenDouble_squares_first() {
            // 3^2 = 9, 9*2 = 18
            assertEquals(18, FunctionalInterfaces.squareThenDouble().apply(3));
        }
        @Test void identityFn_returns_input_unchanged() {
            assertEquals("abc", FunctionalInterfaces.<String>identityFn().apply("abc"));
        }
    }

    @Nested
    @DisplayName("Predicate composition")
    class PredicateComposition {
        @Test void nonBlankAndLong_passes_long_non_blank() {
            assertTrue(FunctionalInterfaces.nonBlankAndLong(3).test("hello"));
        }
        @Test void nonBlankAndLong_fails_blank() {
            assertFalse(FunctionalInterfaces.nonBlankAndLong(3).test("   "));
        }
        @Test void nonBlankAndLong_fails_too_short() {
            assertFalse(FunctionalInterfaces.nonBlankAndLong(5).test("hi"));
        }
        @Test void zeroOrNegative_zero_is_true() {
            assertTrue(FunctionalInterfaces.zeroOrNegative().test(0));
        }
        @Test void zeroOrNegative_negative_is_true() {
            assertTrue(FunctionalInterfaces.zeroOrNegative().test(-5));
        }
        @Test void zeroOrNegative_positive_is_false() {
            assertFalse(FunctionalInterfaces.zeroOrNegative().test(1));
        }
        @Test void isNotEmpty_non_empty_is_true() {
            assertTrue(FunctionalInterfaces.isNotEmpty().test("x"));
        }
        @Test void isNotEmpty_empty_is_false() {
            assertFalse(FunctionalInterfaces.isNotEmpty().test(""));
        }
        @Test void filterWords_returns_matching_words() {
            List<String> result = FunctionalInterfaces.filterWords(
                List.of("hi", "hello", "  ", "world"), 4);
            assertEquals(List.of("hello", "world"), result);
        }
    }

    @Nested
    @DisplayName("Consumer composition")
    class ConsumerComposition {
        @Test void logAndStore_stores_value() {
            List<String> store = new ArrayList<>();
            Consumer<String> c = FunctionalInterfaces.logAndStore(store);
            c.accept("alpha");
            c.accept("beta");
            assertEquals(List.of("alpha", "beta"), store);
        }
    }

    @Nested
    @DisplayName("Supplier")
    class SupplierTest {
        @Test void getOrCompute_returns_value_when_non_null() {
            assertEquals("real", FunctionalInterfaces.getOrCompute("real", () -> "default"));
        }
        @Test void getOrCompute_calls_supplier_when_null() {
            assertEquals("default", FunctionalInterfaces.getOrCompute(null, () -> "default"));
        }
    }

    @Nested
    @DisplayName("BiFunction")
    class BiFunctionTest {
        @Test void repeatAndUpperCase_repeats_then_uppercases() {
            assertEquals("ABAB", FunctionalInterfaces.repeatAndUpperCase().apply("ab", 2));
        }
    }

    @Nested
    @DisplayName("UnaryOperator and BinaryOperator")
    class Operators {
        @Test void wrapInBrackets_wraps() {
            assertEquals("[hello]", FunctionalInterfaces.wrapInBrackets().apply("hello"));
        }
        @Test void safeDivide_normal() {
            assertEquals(5, FunctionalInterfaces.safeDivide().apply(10, 2));
        }
        @Test void safeDivide_by_zero_returns_zero() {
            assertEquals(0, FunctionalInterfaces.safeDivide().apply(10, 0));
        }
    }

    @Nested
    @DisplayName("Primitive specialisations")
    class Primitives {
        @Test void clamp_within_range() {
            assertEquals(5, FunctionalInterfaces.clamp(1, 10).applyAsInt(5));
        }
        @Test void clamp_below_min() {
            assertEquals(1, FunctionalInterfaces.clamp(1, 10).applyAsInt(-5));
        }
        @Test void clamp_above_max() {
            assertEquals(10, FunctionalInterfaces.clamp(1, 10).applyAsInt(100));
        }
        @Test void isEven_even_true() {
            assertTrue(FunctionalInterfaces.isEven().test(4));
        }
        @Test void isEven_odd_false() {
            assertFalse(FunctionalInterfaces.isEven().test(3));
        }
        @Test void stringLength_returns_length() {
            assertEquals(5, FunctionalInterfaces.stringLength().applyAsInt("hello"));
        }
    }

    @Nested
    @DisplayName("Custom Transformer interface")
    class TransformerTest {
        @Test void wordCount_counts_words() {
            assertEquals(3, FunctionalInterfaces.wordCount().transform("one two three"));
        }
        @Test void wordCount_blank_returns_zero() {
            assertEquals(0, FunctionalInterfaces.wordCount().transform("   "));
        }
        @Test void truncate_short_string_unchanged() {
            assertEquals("hi", FunctionalInterfaces.truncate(5).transform("hi"));
        }
        @Test void truncate_long_string_truncated() {
            assertEquals("hel...", FunctionalInterfaces.truncate(3).transform("hello world"));
        }
        @Test void transformer_andThen_composition() {
            FunctionalInterfaces.Transformer<String, Integer> doubled =
                FunctionalInterfaces.wordCount().andThen(n -> n * 2);
            assertEquals(6, doubled.transform("a b c"));
        }
    }
}
