package com.javatraining.inheritance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SealedHierarchyTest {

    // -----------------------------------------------------------------------
    // Result<T>
    // -----------------------------------------------------------------------
    @Test
    void success_is_success() {
        SealedHierarchy.Result<Integer> r = SealedHierarchy.Result.ok(42);
        assertTrue(r.isSuccess());
    }

    @Test
    void failure_is_not_success() {
        SealedHierarchy.Result<Integer> r = SealedHierarchy.Result.fail("oops");
        assertFalse(r.isSuccess());
    }

    @Test
    void success_describe_contains_value() {
        assertTrue(SealedHierarchy.Result.ok("hello").describe().contains("hello"));
    }

    @Test
    void failure_describe_contains_reason() {
        assertTrue(SealedHierarchy.Result.fail("not found").describe().contains("not found"));
    }

    @Test
    void failure_with_cause_describe_contains_cause_message() {
        RuntimeException cause = new RuntimeException("timeout");
        String desc = SealedHierarchy.Result.fail("db error", cause).describe();
        assertTrue(desc.contains("timeout"));
    }

    @Test
    void get_or_throw_returns_value_on_success() {
        assertEquals(99, SealedHierarchy.Result.ok(99).getOrThrow());
    }

    @Test
    void get_or_throw_throws_on_failure() {
        assertThrows(RuntimeException.class,
            () -> SealedHierarchy.Result.<Integer>fail("gone").getOrThrow());
    }

    // -----------------------------------------------------------------------
    // PaymentEvent - describeEvent exhaustive switch
    // -----------------------------------------------------------------------
    @Test
    void describe_initiated_contains_amount() {
        String desc = SealedHierarchy.describeEvent(
            new SealedHierarchy.PaymentEvent.Initiated("P1", 1500, "INR"));
        assertTrue(desc.contains("INITIATED"));
        assertTrue(desc.contains("P1"));
    }

    @Test
    void describe_captured_contains_captured() {
        String desc = SealedHierarchy.describeEvent(
            new SealedHierarchy.PaymentEvent.Captured("P2", 2500));
        assertTrue(desc.contains("CAPTURED"));
    }

    @Test
    void describe_failed_contains_error_code() {
        String desc = SealedHierarchy.describeEvent(
            new SealedHierarchy.PaymentEvent.Failed("P3", "INSUFFICIENT_FUNDS", "declined"));
        assertTrue(desc.contains("FAILED"));
        assertTrue(desc.contains("INSUFFICIENT_FUNDS"));
    }

    @Test
    void total_captured_sums_only_captured_events() {
        List<SealedHierarchy.PaymentEvent> events = List.of(
            new SealedHierarchy.PaymentEvent.Initiated("P1", 5000, "INR"),
            new SealedHierarchy.PaymentEvent.Captured("P1", 5000),
            new SealedHierarchy.PaymentEvent.Captured("P2", 3000),
            new SealedHierarchy.PaymentEvent.Failed("P3", "ERR", "msg")
        );
        assertEquals(8000.0, SealedHierarchy.totalCaptured(events), 1e-9);
    }

    @Test
    void total_captured_no_captures_returns_zero() {
        List<SealedHierarchy.PaymentEvent> events = List.of(
            new SealedHierarchy.PaymentEvent.Initiated("P1", 100, "INR"),
            new SealedHierarchy.PaymentEvent.Failed("P1", "ERR", "msg")
        );
        assertEquals(0.0, SealedHierarchy.totalCaptured(events), 1e-9);
    }

    // -----------------------------------------------------------------------
    // Expr tree - eval and prettyPrint
    // -----------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "5.0",
        "0.0",
        "-3.0",
    })
    void eval_num(double n) {
        assertEquals(n, SealedHierarchy.eval(new SealedHierarchy.Expr.Num(n)), 1e-9);
    }

    @Test
    void eval_add() {
        SealedHierarchy.Expr e = new SealedHierarchy.Expr.Add(
            new SealedHierarchy.Expr.Num(3),
            new SealedHierarchy.Expr.Num(4));
        assertEquals(7.0, SealedHierarchy.eval(e), 1e-9);
    }

    @Test
    void eval_mul() {
        SealedHierarchy.Expr e = new SealedHierarchy.Expr.Mul(
            new SealedHierarchy.Expr.Num(6),
            new SealedHierarchy.Expr.Num(7));
        assertEquals(42.0, SealedHierarchy.eval(e), 1e-9);
    }

    @Test
    void eval_neg() {
        assertEquals(-5.0,
            SealedHierarchy.eval(new SealedHierarchy.Expr.Neg(
                new SealedHierarchy.Expr.Num(5))), 1e-9);
    }

    @Test
    void eval_div() {
        SealedHierarchy.Expr e = new SealedHierarchy.Expr.Div(
            new SealedHierarchy.Expr.Num(10),
            new SealedHierarchy.Expr.Num(4));
        assertEquals(2.5, SealedHierarchy.eval(e), 1e-9);
    }

    @Test
    void eval_div_by_zero_throws() {
        assertThrows(ArithmeticException.class, () ->
            SealedHierarchy.eval(new SealedHierarchy.Expr.Div(
                new SealedHierarchy.Expr.Num(5),
                new SealedHierarchy.Expr.Num(0))));
    }

    @Test
    void eval_nested_expression() {
        // (2 + 3) * 4 = 20
        SealedHierarchy.Expr e = new SealedHierarchy.Expr.Mul(
            new SealedHierarchy.Expr.Add(
                new SealedHierarchy.Expr.Num(2),
                new SealedHierarchy.Expr.Num(3)),
            new SealedHierarchy.Expr.Num(4));
        assertEquals(20.0, SealedHierarchy.eval(e), 1e-9);
    }

    @Test
    void pretty_print_add() {
        SealedHierarchy.Expr e = new SealedHierarchy.Expr.Add(
            new SealedHierarchy.Expr.Num(1),
            new SealedHierarchy.Expr.Num(2));
        assertEquals("(1.0 + 2.0)", SealedHierarchy.prettyPrint(e));
    }
}
