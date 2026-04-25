package com.javatraining.enums;

import java.time.LocalDateTime;
import java.util.*;

/**
 * TOPIC: Practical integration - Order Processing Pipeline
 *
 * Combines every enum technique from this module:
 *   • OrderStatus  - state machine with validated transitions
 *   • PaymentMethod - abstract method (fee calculation per method)
 *   • Priority      - EnumSet filtering for high-priority orders
 *   • Category      - EnumMap revenue aggregation
 *   • Switch expressions for exhaustive enum dispatch
 *
 * Design principle: enums replace class hierarchies when the set of
 * variants is closed and finite.  The compiler guarantees exhaustiveness;
 * there is nothing to null-check or cast.
 */
public class OrderProcessor {

    // -------------------------------------------------------------------------
    // 1. State machine - only allowed transitions are hardcoded
    // -------------------------------------------------------------------------
    enum OrderStatus {
        PENDING {
            @Override
            public EnumSet<OrderStatus> allowedTransitions() {
                return EnumSet.of(CONFIRMED, CANCELLED);
            }
        },
        CONFIRMED {
            @Override
            public EnumSet<OrderStatus> allowedTransitions() {
                return EnumSet.of(PROCESSING, CANCELLED);
            }
        },
        PROCESSING {
            @Override
            public EnumSet<OrderStatus> allowedTransitions() {
                return EnumSet.of(SHIPPED, CANCELLED);
            }
        },
        SHIPPED {
            @Override
            public EnumSet<OrderStatus> allowedTransitions() {
                return EnumSet.of(DELIVERED);
            }
        },
        DELIVERED {
            @Override
            public EnumSet<OrderStatus> allowedTransitions() {
                return EnumSet.of(REFUNDED);
            }
        },
        CANCELLED {
            @Override
            public EnumSet<OrderStatus> allowedTransitions() {
                return EnumSet.noneOf(OrderStatus.class); // terminal
            }
        },
        REFUNDED {
            @Override
            public EnumSet<OrderStatus> allowedTransitions() {
                return EnumSet.noneOf(OrderStatus.class); // terminal
            }
        };

        public abstract EnumSet<OrderStatus> allowedTransitions();

        /** Returns true and transitions; throws if the move is illegal. */
        public OrderStatus transitionTo(OrderStatus next) {
            if (!allowedTransitions().contains(next)) {
                throw new IllegalStateException(
                    "Cannot move from " + this + " → " + next
                    + ".  Allowed: " + allowedTransitions());
            }
            return next;
        }

        public boolean isTerminal() {
            return allowedTransitions().isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // 2. Payment method - abstract fee calculation
    // -------------------------------------------------------------------------
    enum PaymentMethod {
        CREDIT_CARD("Credit Card") {
            @Override
            public double transactionFee(double amount) {
                return amount * 0.020; // 2% fee
            }
        },
        DEBIT_CARD("Debit Card") {
            @Override
            public double transactionFee(double amount) {
                return amount * 0.010; // 1% fee
            }
        },
        UPI("UPI") {
            @Override
            public double transactionFee(double amount) {
                return 0.0; // free
            }
        },
        NET_BANKING("Net Banking") {
            @Override
            public double transactionFee(double amount) {
                return Math.min(amount * 0.005, 50.0); // 0.5%, capped at ₹50
            }
        },
        WALLET("Wallet") {
            @Override
            public double transactionFee(double amount) {
                return amount * 0.015; // 1.5%
            }
        },
        COD("Cash on Delivery") {
            @Override
            public double transactionFee(double amount) {
                return 40.0; // flat ₹40 handling
            }
        };

        private final String displayName;

        PaymentMethod(String displayName) { this.displayName = displayName; }

        public abstract double transactionFee(double amount);

        public double totalCharge(double amount) {
            return amount + transactionFee(amount);
        }

        @Override public String toString() { return displayName; }
    }

    // -------------------------------------------------------------------------
    // 3. Priority - used with EnumSet for filtering
    // -------------------------------------------------------------------------
    enum Priority {
        LOW(1), NORMAL(2), HIGH(3), URGENT(4), CRITICAL(5);

        private final int level;

        Priority(int level) { this.level = level; }

        public int level() { return level; }

        public boolean isHigherThan(Priority other) {
            return this.level > other.level;
        }

        /** All priorities above the given threshold (exclusive). */
        public static EnumSet<Priority> above(Priority threshold) {
            EnumSet<Priority> result = EnumSet.noneOf(Priority.class);
            for (Priority p : values()) {
                if (p.level > threshold.level) result.add(p);
            }
            return result;
        }
    }

    // -------------------------------------------------------------------------
    // 4. Product category - EnumMap revenue aggregation
    // -------------------------------------------------------------------------
    enum Category {
        ELECTRONICS, CLOTHING, BOOKS, FOOD, HEALTH, HOME, SPORTS, TOYS
    }

    // -------------------------------------------------------------------------
    // 5. Order domain object
    // -------------------------------------------------------------------------
    static class Order {
        private final String       id;
        private final Category     category;
        private final Priority     priority;
        private final PaymentMethod payment;
        private final double       amount;
        private       OrderStatus  status;
        private final List<String> statusHistory = new ArrayList<>();

        Order(String id, Category category, Priority priority,
              PaymentMethod payment, double amount) {
            this.id       = id;
            this.category = category;
            this.priority = priority;
            this.payment  = payment;
            this.amount   = amount;
            this.status   = OrderStatus.PENDING;
            statusHistory.add("PENDING @ created");
        }

        public boolean advance(OrderStatus next) {
            try {
                status = status.transitionTo(next);
                statusHistory.add(next + " @ " + LocalDateTime.now());
                return true;
            } catch (IllegalStateException e) {
                System.out.println("  [BLOCKED] " + e.getMessage());
                return false;
            }
        }

        // getters
        public String        id()       { return id; }
        public Category      category() { return category; }
        public Priority      priority() { return priority; }
        public PaymentMethod payment()  { return payment; }
        public double        amount()   { return amount; }
        public OrderStatus   status()   { return status; }
        public List<String>  history()  { return Collections.unmodifiableList(statusHistory); }
    }

    // -------------------------------------------------------------------------
    // 6. Processor - switch expressions for exhaustive dispatch
    // -------------------------------------------------------------------------
    static class Processor {

        private final List<Order> orders = new ArrayList<>();

        void add(Order o) { orders.add(o); }

        /** Switch expression: map status → user-facing label */
        static String statusLabel(OrderStatus s) {
            return switch (s) {
                case PENDING    -> "Awaiting confirmation";
                case CONFIRMED  -> "Order confirmed";
                case PROCESSING -> "Being prepared";
                case SHIPPED    -> "On the way";
                case DELIVERED  -> "Delivered successfully";
                case CANCELLED  -> "Order cancelled";
                case REFUNDED   -> "Refund processed";
            };
        }

        /** Switch expression: SLA hours by priority */
        static int slaHours(Priority p) {
            return switch (p) {
                case CRITICAL -> 2;
                case URGENT   -> 6;
                case HIGH     -> 24;
                case NORMAL   -> 48;
                case LOW      -> 72;
            };
        }

        /** Filter orders whose priority is in the given EnumSet */
        List<Order> filterByPriority(EnumSet<Priority> priorities) {
            return orders.stream()
                         .filter(o -> priorities.contains(o.priority()))
                         .toList();
        }

        /** Aggregate total revenue per category using EnumMap */
        EnumMap<Category, Double> revenueByCategory() {
            EnumMap<Category, Double> map = new EnumMap<>(Category.class);
            for (Order o : orders) {
                if (o.status() == OrderStatus.DELIVERED) {
                    map.merge(o.category(), o.amount(), Double::sum);
                }
            }
            return map;
        }

        /** Total transaction fees collected */
        double totalFees() {
            return orders.stream()
                         .filter(o -> o.status() == OrderStatus.DELIVERED)
                         .mapToDouble(o -> o.payment().transactionFee(o.amount()))
                         .sum();
        }

        void printSummary() {
            System.out.printf("%n  %-10s  %-10s  %-8s  %-14s  %-8s  %s%n",
                "ID", "Category", "Priority", "Payment", "Amount", "Status");
            System.out.println("  " + "─".repeat(72));
            for (Order o : orders) {
                System.out.printf("  %-10s  %-10s  %-8s  %-14s  ₹%7.0f  %s%n",
                    o.id(), o.category(), o.priority(),
                    o.payment(), o.amount(), statusLabel(o.status()));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void stateMachineDemo() {
        System.out.println("=== State Machine Demo ===");

        Order order = new Order("ORD-001", Category.ELECTRONICS,
                                Priority.HIGH, PaymentMethod.UPI, 15_000.0);

        System.out.println("Initial: " + order.status());

        // valid transitions
        order.advance(OrderStatus.CONFIRMED);
        order.advance(OrderStatus.PROCESSING);
        order.advance(OrderStatus.SHIPPED);

        // invalid transition - try to go back
        System.out.print("Attempt SHIPPED → CONFIRMED: ");
        order.advance(OrderStatus.CONFIRMED);  // blocked

        // continue valid path
        order.advance(OrderStatus.DELIVERED);
        order.advance(OrderStatus.REFUNDED);

        // attempt to move from terminal state
        System.out.print("Attempt REFUNDED → DELIVERED: ");
        order.advance(OrderStatus.DELIVERED);  // blocked

        System.out.println("\nStatus history for " + order.id() + ":");
        order.history().forEach(h -> System.out.println("  " + h));
    }

    static void paymentFeeDemo() {
        System.out.println("\n=== Payment Fees on ₹10,000 order ===");
        double amount = 10_000.0;
        System.out.printf("  %-16s  %8s  %10s%n", "Method", "Fee", "Total");
        System.out.println("  " + "─".repeat(38));
        for (PaymentMethod pm : PaymentMethod.values()) {
            System.out.printf("  %-16s  ₹%6.2f  ₹%8.2f%n",
                pm, pm.transactionFee(amount), pm.totalCharge(amount));
        }
    }

    static void priorityFilterDemo(Processor proc) {
        System.out.println("\n=== High-Priority Orders (HIGH and above) ===");
        EnumSet<Priority> highAndAbove = Priority.above(Priority.NORMAL);
        System.out.println("  Filtering for: " + highAndAbove);
        List<Order> urgent = proc.filterByPriority(highAndAbove);
        urgent.forEach(o -> System.out.printf("  %s | %s | SLA: %d hrs%n",
            o.id(), o.priority(), Processor.slaHours(o.priority())));
    }

    static void revenueDemo(Processor proc) {
        System.out.println("\n=== Revenue by Category (delivered orders) ===");
        EnumMap<Category, Double> rev = proc.revenueByCategory();
        if (rev.isEmpty()) {
            System.out.println("  No delivered orders yet.");
            return;
        }
        rev.forEach((cat, total) ->
            System.out.printf("  %-12s  ₹%,.0f%n", cat, total));
        System.out.printf("  %-12s  ₹%,.0f%n", "Total fees",  proc.totalFees());
    }

    public static void main(String[] args) {
        stateMachineDemo();
        paymentFeeDemo();

        // Build a batch of orders
        Processor proc = new Processor();

        Order o1 = new Order("ORD-101", Category.ELECTRONICS,
                             Priority.URGENT, PaymentMethod.CREDIT_CARD, 45_000.0);
        Order o2 = new Order("ORD-102", Category.BOOKS,
                             Priority.LOW, PaymentMethod.UPI, 850.0);
        Order o3 = new Order("ORD-103", Category.CLOTHING,
                             Priority.HIGH, PaymentMethod.DEBIT_CARD, 3_200.0);
        Order o4 = new Order("ORD-104", Category.FOOD,
                             Priority.NORMAL, PaymentMethod.COD, 650.0);
        Order o5 = new Order("ORD-105", Category.HEALTH,
                             Priority.CRITICAL, PaymentMethod.NET_BANKING, 12_000.0);

        // Advance most orders to DELIVERED
        for (Order o : List.of(o1, o2, o3, o5)) {
            o.advance(OrderStatus.CONFIRMED);
            o.advance(OrderStatus.PROCESSING);
            o.advance(OrderStatus.SHIPPED);
            o.advance(OrderStatus.DELIVERED);
        }
        // o4 stays at PENDING for variety

        proc.add(o1); proc.add(o2); proc.add(o3); proc.add(o4); proc.add(o5);

        System.out.println("\n=== Order Summary ===");
        proc.printSummary();

        priorityFilterDemo(proc);
        revenueDemo(proc);
    }
}
