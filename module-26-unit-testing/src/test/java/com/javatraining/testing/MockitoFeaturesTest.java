package com.javatraining.testing;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * A guided tour of Mockito 5 with JUnit Jupiter.
 *
 * Topics covered:
 *   @Mock / @InjectMocks  — create and wire collaborator mocks
 *   @Spy                  — partial mock that calls real methods by default
 *   @Captor               — capture arguments passed to a mock method
 *   when/thenReturn        — stub a return value
 *   thenReturn chaining    — different return value on consecutive calls
 *   thenThrow              — stub an exception
 *   thenAnswer             — dynamic response via lambda
 *   doReturn / doThrow     — alternative stubbing syntax for void/spy
 *   verify                 — assert a method was called with specific arguments
 *   times / never / atLeastOnce — verify call count
 *   InOrder                — assert ordering of cross-mock calls
 *   verifyNoInteractions   — assert a mock was never touched
 *   ArgumentCaptor         — inspect values passed to a mock
 *   Argument matchers      — any(), eq(), anyString(), argThat()
 *
 * MockitoExtension uses STRICT_STUBS by default:
 *   - unused stubbings fail the test
 *   - argument mismatches in stubbings are reported
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Mockito Features Demo")
class MockitoFeaturesTest {

    // @Mock creates a pure mock (all methods return defaults; void methods do nothing)
    @Mock OrderService.OrderRepository    repository;
    @Mock OrderService.PaymentGateway     paymentGateway;
    @Mock OrderService.NotificationService notifications;

    // @InjectMocks creates the real OrderService and injects the three mocks above
    @InjectMocks OrderService orderService;

    // @Captor creates an ArgumentCaptor typed to Order
    @Captor ArgumentCaptor<OrderService.Order> orderCaptor;

    // @Spy wraps a real object; real methods run unless individually stubbed
    @Spy
    BankAccount spiedAccount = new BankAccount("spy-acc", 200.0);

    // ── Basic Stubbing ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Successful payment returns a CONFIRMED order")
    void place_order_successful_payment() {
        when(paymentGateway.charge("cust-1", 100.0))
            .thenReturn(OrderService.PaymentResult.success("txn-1"));

        OrderService.Order order = orderService.placeOrder("cust-1", List.of("book"), 100.0);

        assertAll("order",
            () -> assertEquals(OrderService.OrderStatus.CONFIRMED, order.status()),
            () -> assertEquals("cust-1", order.customerId()),
            () -> assertNotNull(order.id())
        );
    }

    // ── thenReturn with argument matchers ─────────────────────────────────────

    @Test
    void place_order_saves_to_repository() {
        when(paymentGateway.charge(any(), anyDouble()))
            .thenReturn(OrderService.PaymentResult.success("txn-2"));

        orderService.placeOrder("cust-2", List.of("pen"), 5.0);

        verify(repository).save(any(OrderService.Order.class));
    }

    // ── ArgumentCaptor ────────────────────────────────────────────────────────

    @Test
    void argument_captor_inspects_saved_order() {
        when(paymentGateway.charge(any(), anyDouble()))
            .thenReturn(OrderService.PaymentResult.success("txn-3"));

        orderService.placeOrder("cust-3", List.of("a", "b"), 75.0);

        verify(repository).save(orderCaptor.capture());
        OrderService.Order saved = orderCaptor.getValue();

        assertAll("saved order",
            () -> assertEquals(OrderService.OrderStatus.CONFIRMED, saved.status()),
            () -> assertEquals(2, saved.items().size()),
            () -> assertEquals(75.0, saved.total())
        );
    }

    // ── verify / never ────────────────────────────────────────────────────────

    @Test
    void failed_payment_never_saves_or_confirms() {
        when(paymentGateway.charge(any(), anyDouble()))
            .thenReturn(OrderService.PaymentResult.failure("Card declined"));

        assertThrows(IllegalStateException.class,
            () -> orderService.placeOrder("cust-4", List.of("x"), 10.0));

        verify(repository,    never()).save(any());
        verify(notifications, never()).sendOrderConfirmation(any(), any());
    }

    // ── Payment failure notifies the customer ─────────────────────────────────

    @Test
    void failed_payment_sends_failure_notification() {
        when(paymentGateway.charge(any(), anyDouble()))
            .thenReturn(OrderService.PaymentResult.failure("Insufficient credit"));

        assertThrows(IllegalStateException.class,
            () -> orderService.placeOrder("cust-5", List.of("item"), 50.0));

        verify(notifications).sendPaymentFailure("cust-5", "Insufficient credit");
    }

    // ── InOrder: verify cross-mock call ordering ───────────────────────────────

    @Test
    void place_order_charge_then_save_then_notify() {
        when(paymentGateway.charge(any(), anyDouble()))
            .thenReturn(OrderService.PaymentResult.success("txn-4"));

        orderService.placeOrder("cust-6", List.of("item"), 30.0);

        InOrder inOrder = inOrder(paymentGateway, repository, notifications);
        inOrder.verify(paymentGateway).charge("cust-6", 30.0);
        inOrder.verify(repository).save(any());
        inOrder.verify(notifications).sendOrderConfirmation(eq("cust-6"), any());
    }

    // ── thenAnswer: dynamic response ─────────────────────────────────────────

    @Test
    void cancel_existing_order_updates_status() {
        OrderService.Order existing = new OrderService.Order(
            "order-1", "cust-7", List.of("pen"), 20.0, OrderService.OrderStatus.CONFIRMED);

        when(repository.findById("order-1"))
            .thenAnswer(inv -> Optional.of(existing));

        orderService.cancelOrder("order-1");

        verify(repository).save(orderCaptor.capture());
        assertEquals(OrderService.OrderStatus.CANCELLED, orderCaptor.getValue().status());
        verify(notifications).sendOrderCancellation("cust-7", "order-1");
    }

    // ── verifyNoInteractions ─────────────────────────────────────────────────

    @Test
    void find_order_never_touches_payment_or_notifications() {
        when(repository.findById("order-2")).thenReturn(Optional.empty());

        Optional<OrderService.Order> result = orderService.findOrder("order-2");

        assertTrue(result.isEmpty());
        verify(repository).findById("order-2");
        verifyNoInteractions(paymentGateway, notifications);
    }

    // ── Exception stubs ───────────────────────────────────────────────────────

    @Test
    void cancel_shipped_order_throws_and_does_not_update() {
        OrderService.Order shipped = new OrderService.Order(
            "s-1", "c-1", List.of(), 100.0, OrderService.OrderStatus.SHIPPED);

        when(repository.findById("s-1")).thenReturn(Optional.of(shipped));

        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder("s-1"));

        verify(repository, never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void cancel_nonexistent_order_throws_NoSuchElementException() {
        // Mockito returns Optional.empty() by default for Optional return types
        assertThrows(NoSuchElementException.class,
            () -> orderService.cancelOrder("ghost-order"));
    }

    // ── thenReturn chaining: different values on consecutive calls ─────────────

    @Test
    void consecutive_calls_return_different_values() {
        when(paymentGateway.charge(any(), anyDouble()))
            .thenReturn(OrderService.PaymentResult.failure("First attempt failed"))
            .thenReturn(OrderService.PaymentResult.success("txn-retry"));

        // First call fails
        assertThrows(IllegalStateException.class,
            () -> orderService.placeOrder("cust-8", List.of("item"), 10.0));

        // Second call succeeds
        OrderService.Order order = orderService.placeOrder("cust-8", List.of("item"), 10.0);
        assertEquals(OrderService.OrderStatus.CONFIRMED, order.status());
    }

    // ── @Spy: real object with interaction tracking ───────────────────────────

    @Test
    void spy_calls_real_deposit_and_tracks_it() {
        spiedAccount.deposit(50.0);             // real method runs

        verify(spiedAccount).deposit(50.0);     // interaction tracked
        assertEquals(250.0, spiedAccount.balance()); // real state changed
    }

    @Test
    void spy_can_stub_individual_methods() {
        doReturn(9999.0).when(spiedAccount).balance();

        assertEquals(9999.0, spiedAccount.balance()); // returns stubbed value

        // deposit is still the real method
        spiedAccount.deposit(1.0);
        verify(spiedAccount).deposit(1.0);
    }

    // ── times / atLeastOnce ───────────────────────────────────────────────────

    @Test
    void exactly_one_confirmation_per_order() {
        when(paymentGateway.charge(any(), anyDouble()))
            .thenReturn(OrderService.PaymentResult.success("txn-5"));

        orderService.placeOrder("cust-9", List.of("x"), 15.0);

        verify(notifications, times(1)).sendOrderConfirmation(any(), any());
        verify(notifications, never()).sendOrderCancellation(any(), any());
    }

    // ── argThat: custom argument matcher ─────────────────────────────────────

    @Test
    void saved_order_has_positive_total() {
        when(paymentGateway.charge(any(), anyDouble()))
            .thenReturn(OrderService.PaymentResult.success("txn-6"));

        orderService.placeOrder("cust-10", List.of("item"), 99.0);

        verify(repository).save(argThat(order -> order.total() > 0));
    }
}
