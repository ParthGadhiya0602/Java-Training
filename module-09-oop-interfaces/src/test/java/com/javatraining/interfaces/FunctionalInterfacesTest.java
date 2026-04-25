package com.javatraining.interfaces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.function.*;

import static org.junit.jupiter.api.Assertions.*;

class FunctionalInterfacesTest {

    // -----------------------------------------------------------------------
    // Custom Transformer - andThen composition
    // -----------------------------------------------------------------------
    @Test
    void transformer_single_application() {
        FunctionalInterfaces.Transformer<String> upper = String::toUpperCase;
        assertEquals("HELLO", upper.transform("hello"));
    }

    @Test
    void transformer_andThen_chains() {
        FunctionalInterfaces.Transformer<String> trim   = String::trim;
        FunctionalInterfaces.Transformer<String> upper  = String::toUpperCase;
        FunctionalInterfaces.Transformer<String> chain  = trim.andThen(upper);
        assertEquals("HELLO", chain.transform("  hello  "));
    }

    @Test
    void transformer_three_stage_chain() {
        FunctionalInterfaces.Transformer<String> trim    = String::trim;
        FunctionalInterfaces.Transformer<String> upper   = String::toUpperCase;
        FunctionalInterfaces.Transformer<String> exclaim = s -> s + "!";
        FunctionalInterfaces.Transformer<String> chain   = trim.andThen(upper).andThen(exclaim);
        assertEquals("HELLO!", chain.transform("  hello  "));
    }

    // -----------------------------------------------------------------------
    // TriFunction
    // -----------------------------------------------------------------------
    @Test
    void tri_function_applies_all_three_args() {
        FunctionalInterfaces.TriFunction<Integer, Integer, Integer, Integer> sum3 =
            (a, b, c) -> a + b + c;
        assertEquals(6, sum3.apply(1, 2, 3));
    }

    // -----------------------------------------------------------------------
    // ThrowingSupplier
    // -----------------------------------------------------------------------
    @Test
    void throwing_supplier_wrap_success() {
        Supplier<Integer> s = FunctionalInterfaces.ThrowingSupplier.wrap(
            () -> Integer.parseInt("42"));
        assertEquals(42, s.get());
    }

    @Test
    void throwing_supplier_wrap_wraps_exception() {
        Supplier<Integer> s = FunctionalInterfaces.ThrowingSupplier.wrap(
            () -> Integer.parseInt("not-a-number"));
        assertThrows(RuntimeException.class, s::get);
    }

    // -----------------------------------------------------------------------
    // Standard Function composition
    // -----------------------------------------------------------------------
    @Test
    void function_andThen() {
        Function<String, Integer> length  = String::length;
        Function<Integer, String> asHex   = Integer::toHexString;
        Function<String, String>  chain   = length.andThen(asHex);
        assertEquals("5", chain.apply("Hello"));   // length=5, hex(5)="5"
        assertEquals("4", chain.apply("Java"));    // length=4, hex(4)="4"
    }

    @Test
    void function_compose_right_to_left() {
        Function<String, Integer> length = String::length;
        Function<Integer, String> asHex  = Integer::toHexString;
        // compose: asHex(length(x)) - same logical result as andThen above
        Function<String, String>  chain  = asHex.compose(length);
        assertEquals("5", chain.apply("Hello"));
    }

    @Test
    void unary_operator_andThen() {
        UnaryOperator<String> trim    = String::trim;
        UnaryOperator<String> toLower = String::toLowerCase;
        UnaryOperator<String> both    = s -> toLower.apply(trim.apply(s));
        assertEquals("hello world", both.apply("  HELLO WORLD  "));
    }

    // -----------------------------------------------------------------------
    // Predicate composition
    // -----------------------------------------------------------------------
    @Test
    void predicate_and() {
        Predicate<Integer> gt3  = n -> n > 3;
        Predicate<Integer> lt10 = n -> n < 10;
        Predicate<Integer> both = gt3.and(lt10);
        assertTrue(both.test(5));
        assertFalse(both.test(3));
        assertFalse(both.test(10));
    }

    @Test
    void predicate_or() {
        Predicate<Integer> even  = n -> n % 2 == 0;
        Predicate<Integer> neg   = n -> n < 0;
        Predicate<Integer> either = even.or(neg);
        assertTrue(either.test(4));
        assertTrue(either.test(-3));
        assertFalse(either.test(7));
    }

    @Test
    void predicate_negate() {
        Predicate<String> blank    = String::isBlank;
        Predicate<String> nonBlank = blank.negate();
        assertTrue(nonBlank.test("hi"));
        assertFalse(nonBlank.test("  "));
    }

    @ParameterizedTest
    @CsvSource({
        "alice@x.com, true",
        "bob,         false",
        "'',          false",
    })
    void predicate_email_like(String input, boolean expected) {
        Predicate<String> nonEmpty   = s -> !s.isEmpty();
        Predicate<String> hasAt      = s -> s.contains("@");
        Predicate<String> longEnough = s -> s.length() >= 5;
        Predicate<String> emailLike  = nonEmpty.and(hasAt).and(longEnough);
        assertEquals(expected, emailLike.test(input));
    }

    // -----------------------------------------------------------------------
    // Consumer
    // -----------------------------------------------------------------------
    @Test
    void consumer_andThen_both_run() {
        List<String> log = new java.util.ArrayList<>();
        Consumer<String> first  = s -> log.add("first:" + s);
        Consumer<String> second = s -> log.add("second:" + s);
        first.andThen(second).accept("x");
        assertEquals(List.of("first:x", "second:x"), log);
    }

    // -----------------------------------------------------------------------
    // BiFunction + BinaryOperator
    // -----------------------------------------------------------------------
    @Test
    void binary_operator_sum() {
        BinaryOperator<Integer> sum = Integer::sum;
        assertEquals(10, sum.apply(3, 7));
    }

    @Test
    void bi_function_concat() {
        BiFunction<String, String, String> concat = (a, b) -> a + b;
        assertEquals("HelloWorld", concat.apply("Hello", "World"));
    }
}
