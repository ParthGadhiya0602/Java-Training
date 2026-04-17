package com.javatraining.modernjava;

import com.javatraining.modernjava.PatternMatchingDemo.*;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PatternMatchingDemo")
class PatternMatchingDemoTest {

    @Nested
    @DisplayName("instanceof pattern variable")
    class InstanceofPattern {
        @Test void string_described() {
            assertEquals("String of length 5", PatternMatchingDemo.describe("hello"));
        }

        @Test void integer_described() {
            assertEquals("Integer: 42", PatternMatchingDemo.describe(42));
        }

        @Test void list_described() {
            assertEquals("List with 3 elements",
                PatternMatchingDemo.describe(List.of(1, 2, 3)));
        }

        @Test void double_described() {
            assertEquals("Double: 3.14", PatternMatchingDemo.describe(3.14));
        }

        @Test void unknown_type() {
            assertEquals("unknown", PatternMatchingDemo.describe(new java.util.Date()));
        }
    }

    @Nested
    @DisplayName("Pattern in compound conditions")
    class CompoundConditions {
        @Test void long_string_true() {
            assertTrue(PatternMatchingDemo.isLongString("hello world!"));
        }

        @Test void short_string_false() {
            assertFalse(PatternMatchingDemo.isLongString("hi"));
        }

        @Test void non_string_false() {
            assertFalse(PatternMatchingDemo.isLongString(42));
        }

        @Test void positive_int_true() {
            assertTrue(PatternMatchingDemo.isPositiveInt(5));
        }

        @Test void negative_int_false() {
            assertFalse(PatternMatchingDemo.isPositiveInt(-1));
        }
    }

    @Nested
    @DisplayName("Switch expression")
    class SwitchExpression {
        @Test void saturday_is_weekend() {
            assertEquals("weekend", PatternMatchingDemo.dayType("Saturday"));
        }

        @Test void monday_is_weekday() {
            assertEquals("weekday", PatternMatchingDemo.dayType("Monday"));
        }

        @Test void days_in_month_january() {
            assertEquals(31, PatternMatchingDemo.daysInMonth(1, 2024));
        }

        @Test void days_in_month_april() {
            assertEquals(30, PatternMatchingDemo.daysInMonth(4, 2024));
        }

        @Test void days_in_month_feb_leap() {
            assertEquals(29, PatternMatchingDemo.daysInMonth(2, 2024));
        }

        @Test void days_in_month_feb_non_leap() {
            assertEquals(28, PatternMatchingDemo.daysInMonth(2, 2023));
        }

        @Test void invalid_month_throws() {
            assertThrows(IllegalArgumentException.class,
                () -> PatternMatchingDemo.daysInMonth(13, 2024));
        }
    }

    @Nested
    @DisplayName("Sealed class eval")
    class SealedEval {
        @Test void num_eval() {
            assertEquals(5.0, PatternMatchingDemo.eval(new Num(5)), 1e-9);
        }

        @Test void add_eval() {
            assertEquals(7.0,
                PatternMatchingDemo.eval(new Add(new Num(3), new Num(4))), 1e-9);
        }

        @Test void mul_eval() {
            assertEquals(12.0,
                PatternMatchingDemo.eval(new Mul(new Num(3), new Num(4))), 1e-9);
        }

        @Test void neg_eval() {
            assertEquals(-5.0, PatternMatchingDemo.eval(new Neg(new Num(5))), 1e-9);
        }

        @Test void nested_eval() {
            // (2 + 3) * -(4)  = 5 * -4 = -20
            Expr expr = new Mul(new Add(new Num(2), new Num(3)), new Neg(new Num(4)));
            assertEquals(-20.0, PatternMatchingDemo.eval(expr), 1e-9);
        }

        @Test void pretty_print_add() {
            assertEquals("(3.0 + 4.0)",
                PatternMatchingDemo.pretty(new Add(new Num(3), new Num(4))));
        }
    }

    @Nested
    @DisplayName("Guarded patterns (classify)")
    class GuardedPatterns {
        @Test void null_value() {
            assertEquals("null value", PatternMatchingDemo.nullSafeClassify(null));
        }

        @Test void negative_integer() {
            assertTrue(PatternMatchingDemo.classify(-3).contains("negative int"));
        }

        @Test void zero() {
            assertEquals("zero", PatternMatchingDemo.classify(0));
        }

        @Test void positive_integer() {
            assertTrue(PatternMatchingDemo.classify(7).contains("positive int"));
        }

        @Test void blank_string() {
            assertEquals("blank string", PatternMatchingDemo.classify("   "));
        }

        @Test void non_blank_string() {
            assertTrue(PatternMatchingDemo.classify("hi").contains("string"));
        }

        @Test void int_array() {
            assertTrue(PatternMatchingDemo.classify(new int[]{1, 2, 3}).contains("length 3"));
        }
    }

    @Nested
    @DisplayName("HTTP result (sealed + record patterns)")
    class HttpResultTests {
        @Test void ok_result() {
            assertEquals("200 OK: body",
                PatternMatchingDemo.handleResult(new Ok("body")));
        }

        @Test void not_found_result() {
            assertEquals("404 Not Found: /foo",
                PatternMatchingDemo.handleResult(new NotFound("/foo")));
        }

        @Test void server_error_result() {
            assertEquals("500 Error: crash",
                PatternMatchingDemo.handleResult(new ServerError(500, "crash")));
        }
    }

    @Nested
    @DisplayName("Nested record patterns")
    class NestedRecordPatterns {
        @Test void describe_line() {
            PatternMatchingDemo.Point a = new PatternMatchingDemo.Point(1, 2);
            PatternMatchingDemo.Point b = new PatternMatchingDemo.Point(3, 4);
            PatternMatchingDemo.Line line = new PatternMatchingDemo.Line(a, b);
            assertEquals("(1,2) → (3,4)", PatternMatchingDemo.describeLine(line));
        }

        @Test void non_line_returns_default() {
            assertEquals("not a line", PatternMatchingDemo.describeLine("hello"));
        }
    }
}
