package com.javatraining.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderProcessorTest {

    // -----------------------------------------------------------------------
    // OrderStatus - state machine transitions
    // -----------------------------------------------------------------------
    @Test
    void valid_happy_path_PENDING_to_DELIVERED() {
        OrderProcessor.OrderStatus s = OrderProcessor.OrderStatus.PENDING;
        s = s.transitionTo(OrderProcessor.OrderStatus.CONFIRMED);
        s = s.transitionTo(OrderProcessor.OrderStatus.PROCESSING);
        s = s.transitionTo(OrderProcessor.OrderStatus.SHIPPED);
        s = s.transitionTo(OrderProcessor.OrderStatus.DELIVERED);
        assertSame(OrderProcessor.OrderStatus.DELIVERED, s);
    }

    @Test
    void cancel_from_PENDING_is_valid() {
        OrderProcessor.OrderStatus s =
            OrderProcessor.OrderStatus.PENDING.transitionTo(
                OrderProcessor.OrderStatus.CANCELLED);
        assertSame(OrderProcessor.OrderStatus.CANCELLED, s);
    }

    @Test
    void cancel_from_CONFIRMED_is_valid() {
        OrderProcessor.OrderStatus s =
            OrderProcessor.OrderStatus.CONFIRMED.transitionTo(
                OrderProcessor.OrderStatus.CANCELLED);
        assertSame(OrderProcessor.OrderStatus.CANCELLED, s);
    }

    @Test
    void invalid_transition_throws_IllegalStateException() {
        // Cannot go from SHIPPED back to CONFIRMED
        assertThrows(IllegalStateException.class, () ->
            OrderProcessor.OrderStatus.SHIPPED.transitionTo(
                OrderProcessor.OrderStatus.CONFIRMED));
    }

    @Test
    void terminal_state_CANCELLED_has_no_transitions() {
        assertTrue(OrderProcessor.OrderStatus.CANCELLED.isTerminal());
        assertThrows(IllegalStateException.class, () ->
            OrderProcessor.OrderStatus.CANCELLED.transitionTo(
                OrderProcessor.OrderStatus.PENDING));
    }

    @Test
    void terminal_state_REFUNDED_has_no_transitions() {
        assertTrue(OrderProcessor.OrderStatus.REFUNDED.isTerminal());
    }

    @Test
    void non_terminal_states_are_not_terminal() {
        for (OrderProcessor.OrderStatus s : new OrderProcessor.OrderStatus[]{
            OrderProcessor.OrderStatus.PENDING,
            OrderProcessor.OrderStatus.CONFIRMED,
            OrderProcessor.OrderStatus.PROCESSING,
            OrderProcessor.OrderStatus.SHIPPED,
            OrderProcessor.OrderStatus.DELIVERED
        }) {
            assertFalse(s.isTerminal(), s + " should not be terminal");
        }
    }

    // -----------------------------------------------------------------------
    // PaymentMethod - fee calculations
    // -----------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "CREDIT_CARD,  10000, 200.0",
        "DEBIT_CARD,   10000, 100.0",
        "UPI,          10000,   0.0",
        "COD,          10000,  40.0",
    })
    void paymentMethod_transactionFee(String name, double amount, double fee) {
        OrderProcessor.PaymentMethod pm = OrderProcessor.PaymentMethod.valueOf(name);
        assertEquals(fee, pm.transactionFee(amount), 1e-9);
    }

    @Test
    void netBanking_fee_is_capped_at_50() {
        // 0.5% of 20000 = 100, but capped at 50
        assertEquals(50.0,
            OrderProcessor.PaymentMethod.NET_BANKING.transactionFee(20_000), 1e-9);
    }

    @Test
    void netBanking_fee_below_cap() {
        // 0.5% of 5000 = 25 - below cap
        assertEquals(25.0,
            OrderProcessor.PaymentMethod.NET_BANKING.transactionFee(5_000), 1e-9);
    }

    @Test
    void upi_total_charge_equals_amount() {
        double amount = 3500.0;
        assertEquals(amount,
            OrderProcessor.PaymentMethod.UPI.totalCharge(amount), 1e-9);
    }

    // -----------------------------------------------------------------------
    // Priority - above() EnumSet helper
    // -----------------------------------------------------------------------
    @Test
    void above_NORMAL_returns_HIGH_URGENT_CRITICAL() {
        EnumSet<OrderProcessor.Priority> result =
            OrderProcessor.Priority.above(OrderProcessor.Priority.NORMAL);
        assertTrue(result.contains(OrderProcessor.Priority.HIGH));
        assertTrue(result.contains(OrderProcessor.Priority.URGENT));
        assertTrue(result.contains(OrderProcessor.Priority.CRITICAL));
        assertFalse(result.contains(OrderProcessor.Priority.LOW));
        assertFalse(result.contains(OrderProcessor.Priority.NORMAL));
    }

    @Test
    void above_CRITICAL_is_empty() {
        EnumSet<OrderProcessor.Priority> result =
            OrderProcessor.Priority.above(OrderProcessor.Priority.CRITICAL);
        assertTrue(result.isEmpty());
    }

    @Test
    void priority_isHigherThan_is_correct() {
        assertTrue(OrderProcessor.Priority.URGENT.isHigherThan(
            OrderProcessor.Priority.HIGH));
        assertFalse(OrderProcessor.Priority.LOW.isHigherThan(
            OrderProcessor.Priority.NORMAL));
    }

    // -----------------------------------------------------------------------
    // Processor - statusLabel and slaHours (switch expressions)
    // -----------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "PENDING,    Awaiting confirmation",
        "CONFIRMED,  Order confirmed",
        "PROCESSING, Being prepared",
        "SHIPPED,    On the way",
        "DELIVERED,  Delivered successfully",
        "CANCELLED,  Order cancelled",
        "REFUNDED,   Refund processed",
    })
    void statusLabel_covers_all_states(String statusName, String label) {
        OrderProcessor.OrderStatus s =
            OrderProcessor.OrderStatus.valueOf(statusName);
        assertEquals(label, OrderProcessor.Processor.statusLabel(s));
    }

    @ParameterizedTest
    @CsvSource({
        "CRITICAL,  2",
        "URGENT,    6",
        "HIGH,     24",
        "NORMAL,   48",
        "LOW,      72",
    })
    void slaHours_maps_priority_correctly(String priorityName, int hours) {
        OrderProcessor.Priority p = OrderProcessor.Priority.valueOf(priorityName);
        assertEquals(hours, OrderProcessor.Processor.slaHours(p));
    }

    // -----------------------------------------------------------------------
    // Order - advance() tracks history; blocked transitions return false
    // -----------------------------------------------------------------------
    @Test
    void order_advance_records_history() {
        OrderProcessor.Order o = new OrderProcessor.Order(
            "T-001", OrderProcessor.Category.ELECTRONICS,
            OrderProcessor.Priority.HIGH,
            OrderProcessor.PaymentMethod.UPI, 5000.0);

        assertEquals(OrderProcessor.OrderStatus.PENDING, o.status());
        assertTrue(o.advance(OrderProcessor.OrderStatus.CONFIRMED));
        assertEquals(OrderProcessor.OrderStatus.CONFIRMED, o.status());
        assertEquals(2, o.history().size()); // PENDING + CONFIRMED
    }

    @Test
    void order_advance_returns_false_on_invalid_transition() {
        OrderProcessor.Order o = new OrderProcessor.Order(
            "T-002", OrderProcessor.Category.BOOKS,
            OrderProcessor.Priority.LOW,
            OrderProcessor.PaymentMethod.COD, 200.0);

        // Try invalid skip: PENDING → SHIPPED
        boolean moved = o.advance(OrderProcessor.OrderStatus.SHIPPED);
        assertFalse(moved);
        assertSame(OrderProcessor.OrderStatus.PENDING, o.status());
    }

    @Test
    void processor_filterByPriority_returns_matching_orders() {
        OrderProcessor.Processor proc = new OrderProcessor.Processor();
        OrderProcessor.Order high = new OrderProcessor.Order(
            "H1", OrderProcessor.Category.FOOD,
            OrderProcessor.Priority.HIGH,
            OrderProcessor.PaymentMethod.UPI, 100.0);
        OrderProcessor.Order low = new OrderProcessor.Order(
            "L1", OrderProcessor.Category.FOOD,
            OrderProcessor.Priority.LOW,
            OrderProcessor.PaymentMethod.UPI, 50.0);
        proc.add(high); proc.add(low);

        List<OrderProcessor.Order> filtered = proc.filterByPriority(
            EnumSet.of(OrderProcessor.Priority.HIGH));
        assertEquals(1, filtered.size());
        assertEquals("H1", filtered.get(0).id());
    }

    @Test
    void processor_revenueByCategory_sums_only_delivered_orders() {
        OrderProcessor.Processor proc = new OrderProcessor.Processor();

        OrderProcessor.Order delivered = new OrderProcessor.Order(
            "D1", OrderProcessor.Category.ELECTRONICS,
            OrderProcessor.Priority.NORMAL,
            OrderProcessor.PaymentMethod.UPI, 10_000.0);
        // advance to DELIVERED
        delivered.advance(OrderProcessor.OrderStatus.CONFIRMED);
        delivered.advance(OrderProcessor.OrderStatus.PROCESSING);
        delivered.advance(OrderProcessor.OrderStatus.SHIPPED);
        delivered.advance(OrderProcessor.OrderStatus.DELIVERED);

        OrderProcessor.Order pending = new OrderProcessor.Order(
            "P1", OrderProcessor.Category.ELECTRONICS,
            OrderProcessor.Priority.NORMAL,
            OrderProcessor.PaymentMethod.UPI, 5_000.0);
        // stays PENDING

        proc.add(delivered); proc.add(pending);

        var revenue = proc.revenueByCategory();
        assertEquals(10_000.0,
            revenue.getOrDefault(OrderProcessor.Category.ELECTRONICS, 0.0), 1e-9);
    }
}
