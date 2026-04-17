package com.javatraining.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FunctionalPatterns")
class FunctionalPatternsTest {

    @Nested
    @DisplayName("Higher-order functions")
    class HigherOrder {
        @Test void applyTwice_doubles_application() {
            UnaryOperator<Integer> addThree = x -> x + 3;
            assertEquals(11, FunctionalPatterns.applyTwice(addThree).apply(5));
        }
        @Test void pipeline_empty_is_identity() {
            assertEquals("hello", FunctionalPatterns.pipeline(List.of()).apply("hello"));
        }
        @Test void pipeline_applies_steps_in_order() {
            List<UnaryOperator<String>> steps = List.of(
                String::trim,
                String::toUpperCase,
                s -> s + "!"
            );
            assertEquals("HELLO!", FunctionalPatterns.pipeline(steps).apply("  hello  "));
        }
    }

    @Nested
    @DisplayName("Currying and partial application")
    class Currying {
        @Test void curry_first_then_second_arg() {
            Function<Integer, Function<Integer, Integer>> curriedAdd =
                FunctionalPatterns.curry(Integer::sum);
            assertEquals(7, curriedAdd.apply(3).apply(4));
        }
        @Test void partial_fixes_first_arg() {
            Function<Integer, Integer> add10 = FunctionalPatterns.partial(Integer::sum, 10);
            assertEquals(15, add10.apply(5));
        }
        @Test void adder_creates_reusable_function() {
            Function<Integer, Integer> add5 = FunctionalPatterns.adder(5);
            assertEquals(8,  add5.apply(3));
            assertEquals(15, add5.apply(10));
        }
    }

    @Nested
    @DisplayName("Memoization")
    class Memoization {
        @Test void memoize_returns_correct_result() {
            int[] callCount = {0};
            Function<Integer, Integer> expensive = n -> { callCount[0]++; return n * n; };
            Function<Integer, Integer> memo = FunctionalPatterns.memoize(expensive);
            assertEquals(25, memo.apply(5));
            assertEquals(25, memo.apply(5));
            assertEquals(1,  callCount[0]);
        }
        @Test void memoize_different_inputs_computed_separately() {
            Function<Integer, Integer> memo = FunctionalPatterns.memoize(n -> n * 2);
            assertEquals(4, memo.apply(2));
            assertEquals(6, memo.apply(3));
        }
        @Test void fibMemo_correct_values() {
            assertEquals(0,    FunctionalPatterns.fibMemo(0));
            assertEquals(1,    FunctionalPatterns.fibMemo(1));
            assertEquals(1,    FunctionalPatterns.fibMemo(2));
            assertEquals(55,   FunctionalPatterns.fibMemo(10));
            assertEquals(6765, FunctionalPatterns.fibMemo(20));
        }
    }

    @Nested
    @DisplayName("Strategy pattern")
    class Strategy {
        @Test void no_discount_for_standard_tier() {
            FunctionalPatterns.Order o = new FunctionalPatterns.Order("1", 100.0, "STANDARD");
            assertEquals(100.0, FunctionalPatterns.applyDiscount(o), 0.001);
        }
        @Test void ten_pct_discount_for_silver() {
            FunctionalPatterns.Order o = new FunctionalPatterns.Order("2", 100.0, "SILVER");
            assertEquals(90.0, FunctionalPatterns.applyDiscount(o), 0.001);
        }
        @Test void twenty_pct_discount_for_gold() {
            FunctionalPatterns.Order o = new FunctionalPatterns.Order("3", 100.0, "GOLD");
            assertEquals(80.0, FunctionalPatterns.applyDiscount(o), 0.001);
        }
    }

    @Nested
    @DisplayName("Decorator pattern")
    class Decorator {
        @Test void withLogging_returns_correct_result() {
            Function<Integer, Integer> logged =
                FunctionalPatterns.withLogging(x -> x * 2, "double");
            assertEquals(10, logged.apply(5));
        }
        @Test void withRetry_succeeds_on_first_try() {
            Function<String, Integer> fn = FunctionalPatterns.withRetry(Integer::parseInt, 3);
            assertEquals(42, fn.apply("42"));
        }
        @Test void withRetry_succeeds_after_failure() {
            int[] attempts = {0};
            Function<String, String> flaky = FunctionalPatterns.withRetry(s -> {
                attempts[0]++;
                if (attempts[0] < 3) throw new RuntimeException("transient");
                return s.toUpperCase();
            }, 5);
            assertEquals("HELLO", flaky.apply("hello"));
            assertEquals(3, attempts[0]);
        }
        @Test void withRetry_throws_after_max_attempts() {
            Function<String, String> alwaysFails = FunctionalPatterns.withRetry(
                s -> { throw new RuntimeException("always fails"); }, 3);
            assertThrows(RuntimeException.class, () -> alwaysFails.apply("x"));
        }
    }

    @Nested
    @DisplayName("Validation combinator")
    class Validation {
        @Test void passwordValidator_valid_password() {
            assertTrue(FunctionalPatterns.passwordValidator().validate("securepwd").isEmpty());
        }
        @Test void passwordValidator_too_short() {
            List<String> errors = FunctionalPatterns.passwordValidator().validate("abc");
            assertEquals(1, errors.size());
            assertTrue(errors.get(0).contains("at least 8"));
        }
        @Test void passwordValidator_has_spaces() {
            List<String> errors = FunctionalPatterns.passwordValidator().validate("my password");
            assertEquals(1, errors.size());
            assertTrue(errors.get(0).contains("spaces"));
        }
        @Test void passwordValidator_short_and_spaces_returns_both_errors() {
            assertEquals(2, FunctionalPatterns.passwordValidator().validate("ab cd").size());
        }
    }

    @Nested
    @DisplayName("Lazy evaluation")
    class LazyTest {
        @Test void lazy_computes_on_first_get() {
            int[] count = {0};
            FunctionalPatterns.Lazy<String> lazy =
                FunctionalPatterns.Lazy.of(() -> { count[0]++; return "hello"; });
            assertEquals(0, count[0]);
            assertEquals("hello", lazy.get());
            assertEquals(1, count[0]);
        }
        @Test void lazy_caches_after_first_call() {
            int[] count = {0};
            FunctionalPatterns.Lazy<Integer> lazy =
                FunctionalPatterns.Lazy.of(() -> { count[0]++; return 42; });
            lazy.get();
            lazy.get();
            lazy.get();
            assertEquals(1, count[0]);
        }
    }
}
