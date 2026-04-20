package com.javatraining.cleancode.solid.srp;

import java.util.ArrayList;
import java.util.List;

/**
 * SRP VIOLATION — "God class" that does too many things.
 *
 * <p>This class has at least four reasons to change:
 * <ol>
 *   <li>Validation rules change (business logic)</li>
 *   <li>Storage mechanism changes (database → message queue)</li>
 *   <li>Email template changes (marketing)</li>
 *   <li>Invoice format changes (legal/finance)</li>
 * </ol>
 *
 * <p>One class with four reasons to change = four teams stepping on each other.
 * See the refactored split in {@link OrderValidator}, {@link OrderRepository},
 * {@link NotificationService}, and {@link InvoiceGenerator}.
 */
public class OrderServiceGod {

    // Simulated "database"
    private final List<Order> database = new ArrayList<>();

    /**
     * Does everything: validate + save + notify + invoice.
     * Any change to any concern forces a re-test of the entire method.
     */
    public String processOrder(Order order) {
        // 1. Validate (business concern)
        if (order == null) throw new IllegalArgumentException("Order must not be null");
        if (order.items().isEmpty()) throw new IllegalArgumentException("Order must have items");
        if (order.total() <= 0) throw new IllegalArgumentException("Order total must be positive");

        // 2. Persist (infrastructure concern)
        database.add(order);

        // 3. Send email (communication concern)
        String email = "Dear " + order.customerName() + ", your order #" + order.id() + " is confirmed.";

        // 4. Generate invoice (financial concern)
        String invoice = "INVOICE #" + order.id() + " — Total: $" + order.total();

        return email + "\n" + invoice;
    }

    public List<Order> getDatabase() { return database; }
}
