package com.javatraining.inheritance;

import java.util.List;

/**
 * TOPIC: Sealed classes and interfaces (Java 17+)
 *
 * A sealed type restricts which classes may extend/implement it.
 * The compiler knows the COMPLETE set of subtypes at compile time.
 * This makes switch expressions on sealed types EXHAUSTIVE — no default needed.
 *
 * Permitted subtypes must be:
 *   • final      — no further subclassing
 *   • sealed     — further restricted subhierarchy
 *   • non-sealed — re-opens for arbitrary subclassing
 *
 * Why sealed?
 *   1. Exhaustive pattern matching  — safer than instanceof chains
 *   2. Domain modelling             — express "there are exactly N variants"
 *   3. API design                   — prevent uncontrolled extension
 *
 * Java 17+: sealed classes/interfaces
 * Java 21:  pattern matching in switch (production-ready)
 */
public class SealedHierarchy {

    // -------------------------------------------------------------------------
    // 1. Result type — sealed to express success-or-failure cleanly
    //    Replaces the checked-exception/null-return antipatterns.
    // -------------------------------------------------------------------------
    sealed interface Result<T> permits Result.Success, Result.Failure {

        record Success<T>(T value) implements Result<T> {}
        record Failure<T>(String reason, Throwable cause) implements Result<T> {
            Failure(String reason) { this(reason, null); }
        }

        // Factory helpers
        static <T> Result<T> ok(T value)           { return new Success<>(value); }
        static <T> Result<T> fail(String reason)   { return new Failure<>(reason); }
        static <T> Result<T> fail(String reason, Throwable t) {
            return new Failure<>(reason, t);
        }

        default boolean isSuccess() { return this instanceof Success; }

        /** Switch expression — exhaustive, no default branch */
        default String describe() {
            return switch (this) {
                case Success<T> s -> "OK: " + s.value();
                case Failure<T> f -> "FAIL: " + f.reason()
                    + (f.cause() != null ? " (caused by " + f.cause().getMessage() + ")" : "");
            };
        }

        /** Unwrap or throw */
        default T getOrThrow() {
            return switch (this) {
                case Success<T> s -> s.value();
                case Failure<T> f -> throw new RuntimeException(f.reason(), f.cause());
            };
        }
    }

    // -------------------------------------------------------------------------
    // 2. Payment event hierarchy — sealed interface with multiple permits
    // -------------------------------------------------------------------------
    sealed interface PaymentEvent
        permits PaymentEvent.Initiated, PaymentEvent.Authorised,
                PaymentEvent.Captured, PaymentEvent.Failed, PaymentEvent.Refunded {

        record Initiated(String paymentId, double amount, String currency)
            implements PaymentEvent {}

        record Authorised(String paymentId, String authCode)
            implements PaymentEvent {}

        record Captured(String paymentId, double capturedAmount)
            implements PaymentEvent {}

        record Failed(String paymentId, String errorCode, String message)
            implements PaymentEvent {}

        record Refunded(String paymentId, double refundAmount, String reason)
            implements PaymentEvent {}
    }

    /** Process any payment event — switch is exhaustive, no default */
    static String describeEvent(PaymentEvent event) {
        return switch (event) {
            case PaymentEvent.Initiated  e -> String.format(
                "[INITIATED]  %s  %.2f %s", e.paymentId(), e.amount(), e.currency());
            case PaymentEvent.Authorised e -> String.format(
                "[AUTHORISED] %s  auth=%s",  e.paymentId(), e.authCode());
            case PaymentEvent.Captured   e -> String.format(
                "[CAPTURED]   %s  ₹%.2f",   e.paymentId(), e.capturedAmount());
            case PaymentEvent.Failed     e -> String.format(
                "[FAILED]     %s  %s: %s",  e.paymentId(), e.errorCode(), e.message());
            case PaymentEvent.Refunded   e -> String.format(
                "[REFUNDED]   %s  ₹%.2f (%s)", e.paymentId(), e.refundAmount(), e.reason());
        };
    }

    /** Compute total captured revenue — pattern matching extracts fields directly */
    static double totalCaptured(List<PaymentEvent> events) {
        double total = 0;
        for (PaymentEvent e : events) {
            if (e instanceof PaymentEvent.Captured c) {
                total += c.capturedAmount();
            }
        }
        return total;
    }

    // -------------------------------------------------------------------------
    // 3. Expression tree — sealed for safe recursive evaluation
    //    Classic example from functional programming, now idiomatic in Java 21.
    // -------------------------------------------------------------------------
    sealed interface Expr
        permits Expr.Num, Expr.Add, Expr.Mul, Expr.Neg, Expr.Div {

        record Num(double value)          implements Expr {}
        record Add(Expr left, Expr right) implements Expr {}
        record Mul(Expr left, Expr right) implements Expr {}
        record Neg(Expr expr)             implements Expr {}
        record Div(Expr left, Expr right) implements Expr {}
    }

    static double eval(Expr expr) {
        return switch (expr) {
            case Expr.Num n       -> n.value();
            case Expr.Add a       -> eval(a.left()) + eval(a.right());
            case Expr.Mul m       -> eval(m.left()) * eval(m.right());
            case Expr.Neg n       -> -eval(n.expr());
            case Expr.Div d       -> {
                double divisor = eval(d.right());
                if (divisor == 0) throw new ArithmeticException("Division by zero");
                yield eval(d.left()) / divisor;
            }
        };
    }

    static String prettyPrint(Expr expr) {
        return switch (expr) {
            case Expr.Num n -> String.valueOf(n.value());
            case Expr.Add a -> "(" + prettyPrint(a.left()) + " + " + prettyPrint(a.right()) + ")";
            case Expr.Mul m -> "(" + prettyPrint(m.left()) + " * " + prettyPrint(m.right()) + ")";
            case Expr.Neg n -> "(-" + prettyPrint(n.expr()) + ")";
            case Expr.Div d -> "(" + prettyPrint(d.left()) + " / " + prettyPrint(d.right()) + ")";
        };
    }

    // -------------------------------------------------------------------------
    // 4. Guarded patterns (Java 21) — case Type t when t.field() > x
    // -------------------------------------------------------------------------
    static String classifyPayment(PaymentEvent event) {
        return switch (event) {
            case PaymentEvent.Captured c when c.capturedAmount() >= 10_000 ->
                "HIGH-VALUE capture: ₹" + c.capturedAmount();
            case PaymentEvent.Captured c ->
                "normal capture: ₹" + c.capturedAmount();
            case PaymentEvent.Failed f when f.errorCode().startsWith("FRAUD") ->
                "FRAUD alert: " + f.message();
            case PaymentEvent.Failed f ->
                "payment failure: " + f.errorCode();
            default -> "other event: " + event.getClass().getSimpleName();
        };
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void resultDemo() {
        System.out.println("=== Result<T> (sealed interface) ===");

        Result<Integer> ok   = Result.ok(42);
        Result<Integer> fail = Result.fail("not found");
        Result<Integer> err  = Result.fail("db error",
            new RuntimeException("connection refused"));

        System.out.println(ok.describe());
        System.out.println(fail.describe());
        System.out.println(err.describe());
        System.out.println("isSuccess: " + ok.isSuccess() + " / " + fail.isSuccess());

        try { fail.getOrThrow(); }
        catch (RuntimeException e) { System.out.println("getOrThrow threw: " + e.getMessage()); }
    }

    static void paymentEventDemo() {
        System.out.println("\n=== Payment Events ===");

        List<PaymentEvent> events = List.of(
            new PaymentEvent.Initiated("PAY-001", 15_000, "INR"),
            new PaymentEvent.Authorised("PAY-001", "AUTH-XYZ"),
            new PaymentEvent.Captured("PAY-001", 15_000),
            new PaymentEvent.Initiated("PAY-002", 500, "INR"),
            new PaymentEvent.Failed("PAY-002", "INSUFFICIENT_FUNDS", "Card declined"),
            new PaymentEvent.Initiated("PAY-003", 25_000, "INR"),
            new PaymentEvent.Captured("PAY-003", 25_000),
            new PaymentEvent.Refunded("PAY-001", 5_000, "Partial cancellation")
        );

        events.forEach(e -> System.out.println("  " + describeEvent(e)));
        System.out.printf("%nTotal captured: ₹%.0f%n", totalCaptured(events));

        System.out.println("\n--- Guarded patterns ---");
        events.forEach(e -> System.out.println("  " + classifyPayment(e)));
    }

    static void exprTreeDemo() {
        System.out.println("\n=== Expression Tree (sealed Expr) ===");

        // (2 + 3) * (10 / 5) = 5 * 2 = 10
        Expr e1 = new Expr.Mul(
            new Expr.Add(new Expr.Num(2), new Expr.Num(3)),
            new Expr.Div(new Expr.Num(10), new Expr.Num(5))
        );
        System.out.println(prettyPrint(e1) + " = " + eval(e1));

        // -(4 * 3) + 20 = -12 + 20 = 8
        Expr e2 = new Expr.Add(
            new Expr.Neg(new Expr.Mul(new Expr.Num(4), new Expr.Num(3))),
            new Expr.Num(20)
        );
        System.out.println(prettyPrint(e2) + " = " + eval(e2));

        // Division by zero
        try {
            eval(new Expr.Div(new Expr.Num(5), new Expr.Num(0)));
        } catch (ArithmeticException ex) {
            System.out.println("Caught: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        resultDemo();
        paymentEventDemo();
        exprTreeDemo();
    }
}
