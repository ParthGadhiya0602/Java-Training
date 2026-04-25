package com.javatraining.testing;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Order management service - used as the subject-under-test for Mockito demos.
 * Depends on three collaborators (repository, payment gateway, notifications),
 * none of which have real implementations; they are replaced by mocks in tests.
 */
public class OrderService {

    // ── Domain types ─────────────────────────────────────────────────────────

    public enum OrderStatus { PENDING, CONFIRMED, SHIPPED, CANCELLED }

    public record Order(
        String id,
        String customerId,
        List<String> items,
        double total,
        OrderStatus status
    ) {}

    public record PaymentResult(boolean success, String transactionId, String errorMessage) {
        public static PaymentResult success(String txnId)  { return new PaymentResult(true,  txnId, null); }
        public static PaymentResult failure(String reason) { return new PaymentResult(false, null,  reason); }
    }

    // ── Collaborator interfaces ───────────────────────────────────────────────

    public interface OrderRepository {
        void            save(Order order);
        Optional<Order> findById(String id);
        List<Order>     findByCustomer(String customerId);
        void            delete(String id);
    }

    public interface PaymentGateway {
        PaymentResult charge(String customerId, double amount);
        void          refund(String transactionId);
    }

    public interface NotificationService {
        void sendOrderConfirmation(String customerId, String orderId);
        void sendOrderCancellation(String customerId, String orderId);
        void sendPaymentFailure(String customerId, String reason);
    }

    // ── Service ───────────────────────────────────────────────────────────────

    private final OrderRepository    repository;
    private final PaymentGateway     paymentGateway;
    private final NotificationService notifications;

    public OrderService(OrderRepository repository,
                        PaymentGateway paymentGateway,
                        NotificationService notifications) {
        this.repository    = repository;
        this.paymentGateway = paymentGateway;
        this.notifications = notifications;
    }

    /**
     * Charges payment, persists, and notifies the customer.
     * @throws IllegalStateException if the payment is declined
     */
    public Order placeOrder(String customerId, List<String> items, double total) {
        PaymentResult payment = paymentGateway.charge(customerId, total);
        if (!payment.success()) {
            notifications.sendPaymentFailure(customerId, payment.errorMessage());
            throw new IllegalStateException("Payment failed: " + payment.errorMessage());
        }
        Order order = new Order(
            UUID.randomUUID().toString(),
            customerId,
            List.copyOf(items),
            total,
            OrderStatus.CONFIRMED
        );
        repository.save(order);
        notifications.sendOrderConfirmation(customerId, order.id());
        return order;
    }

    /**
     * Cancels a PENDING or CONFIRMED order.
     * @throws NoSuchElementException  if the order does not exist
     * @throws IllegalStateException   if the order has already shipped
     */
    public void cancelOrder(String orderId) {
        Order order = repository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        if (order.status() == OrderStatus.SHIPPED)
            throw new IllegalStateException("Cannot cancel a shipped order");

        Order cancelled = new Order(
            order.id(), order.customerId(), order.items(), order.total(), OrderStatus.CANCELLED);
        repository.save(cancelled);
        notifications.sendOrderCancellation(order.customerId(), orderId);
    }

    public Optional<Order> findOrder(String orderId) {
        return repository.findById(orderId);
    }

    public List<Order> findOrdersByCustomer(String customerId) {
        return repository.findByCustomer(customerId);
    }
}
