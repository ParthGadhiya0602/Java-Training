package com.javatraining.interfaces;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * TOPIC: Practical integration — Order processing pipeline
 *
 * Uses every concept from this module:
 *   • @FunctionalInterface for pipeline stages
 *   • default method composition (andThen)
 *   • Predicate for validation and filtering
 *   • Function for transformation
 *   • Consumer for side-effects (logging, persisting)
 *   • Abstract class for shared infrastructure
 *   • Interface for pluggable strategies (pricing, tax, discount)
 *
 * Pipeline stages:
 *   RawOrder → [validate] → [enrich] → [price] → [tax] → [discount] → PricedOrder
 */
public class OrderPipeline {

    // -------------------------------------------------------------------------
    // Domain objects
    // -------------------------------------------------------------------------
    record RawOrder(String orderId, String customerId, String productCode,
                    int quantity, String couponCode) {}

    record Customer(String id, String name, String tier) {}  // tier: STANDARD/GOLD/PLATINUM

    record Product(String code, String name, double basePrice, String category) {}

    record PricedOrder(String orderId, Customer customer, Product product,
                       int quantity, double subtotal, double taxAmount,
                       double discountAmount, double total, List<String> auditLog) {

        String summary() {
            return String.format(
                "Order %-8s | %-15s | %-20s | qty=%d | sub=₹%.0f | tax=₹%.0f | disc=₹%.0f | total=₹%.0f",
                orderId, customer.name(), product.name(),
                quantity, subtotal, taxAmount, discountAmount, total);
        }
    }

    // -------------------------------------------------------------------------
    // @FunctionalInterface pipeline stage
    // -------------------------------------------------------------------------
    @FunctionalInterface
    interface PipelineStage<I, O> {
        O process(I input) throws PipelineException;

        default <R> PipelineStage<I, R> andThen(PipelineStage<O, R> after) {
            return input -> after.process(this.process(input));
        }
    }

    static class PipelineException extends RuntimeException {
        private final String stage;
        PipelineException(String stage, String message) {
            super("[" + stage + "] " + message);
            this.stage = stage;
        }
        String stage() { return stage; }
    }

    // -------------------------------------------------------------------------
    // Strategy interfaces — pluggable behaviour
    // -------------------------------------------------------------------------
    interface TaxStrategy {
        double taxRate(Product product);
        default double taxAmount(Product product, double subtotal) {
            return subtotal * taxRate(product);
        }
    }

    interface DiscountStrategy {
        double discountRate(Customer customer, String couponCode);
        default double discountAmount(Customer customer, String couponCode, double subtotal) {
            return subtotal * discountRate(customer, couponCode);
        }
    }

    // -------------------------------------------------------------------------
    // Abstract pipeline base — shared audit log + error wrapping
    // -------------------------------------------------------------------------
    static abstract class AuditedPipeline<I, O> {

        private final String name;
        private final List<String> log = new ArrayList<>();

        AuditedPipeline(String name) { this.name = name; }

        // Template method
        final O execute(I input) {
            audit("START " + name);
            try {
                O result = doProcess(input);
                audit("DONE  " + name);
                return result;
            } catch (PipelineException e) {
                audit("ERROR " + e.getMessage());
                throw e;
            }
        }

        protected abstract O doProcess(I input);

        protected void audit(String message) { log.add(message); }
        List<String> auditLog() { return Collections.unmodifiableList(log); }
    }

    // -------------------------------------------------------------------------
    // Concrete pipeline implementation
    // -------------------------------------------------------------------------
    static class OrderProcessingPipeline extends AuditedPipeline<RawOrder, PricedOrder> {

        private final Map<String, Customer> customers;
        private final Map<String, Product>  products;
        private final TaxStrategy           taxStrategy;
        private final DiscountStrategy      discountStrategy;
        private final Consumer<PricedOrder> onComplete;

        OrderProcessingPipeline(
                Map<String, Customer> customers,
                Map<String, Product>  products,
                TaxStrategy           taxStrategy,
                DiscountStrategy      discountStrategy,
                Consumer<PricedOrder> onComplete) {
            super("OrderPipeline");
            this.customers        = customers;
            this.products         = products;
            this.taxStrategy      = taxStrategy;
            this.discountStrategy = discountStrategy;
            this.onComplete       = onComplete;
        }

        @Override
        protected PricedOrder doProcess(RawOrder raw) {
            // Stage 1: validate
            validate(raw);

            // Stage 2: enrich
            Customer customer = enrich(raw);
            Product  product  = lookupProduct(raw);

            // Stage 3: price
            double subtotal = product.basePrice() * raw.quantity();
            audit("  subtotal = ₹" + subtotal);

            // Stage 4: tax (strategy)
            double tax = taxStrategy.taxAmount(product, subtotal);
            audit("  tax = ₹" + String.format("%.2f", tax)
                + " (" + (taxStrategy.taxRate(product) * 100) + "%)");

            // Stage 5: discount (strategy)
            double discount = discountStrategy.discountAmount(
                customer, raw.couponCode(), subtotal);
            audit("  discount = ₹" + String.format("%.2f", discount));

            double total = subtotal + tax - discount;

            PricedOrder order = new PricedOrder(
                raw.orderId(), customer, product,
                raw.quantity(), subtotal, tax, discount, total,
                new ArrayList<>(auditLog()));

            // Side-effect stage — Consumer
            onComplete.accept(order);
            return order;
        }

        private void validate(RawOrder raw) {
            audit("  validating order " + raw.orderId());
            if (raw.orderId()    == null || raw.orderId().isBlank())
                throw new PipelineException("VALIDATE", "orderId is blank");
            if (raw.customerId() == null || raw.customerId().isBlank())
                throw new PipelineException("VALIDATE", "customerId is blank");
            if (raw.quantity()   <= 0)
                throw new PipelineException("VALIDATE", "quantity must be > 0");
        }

        private Customer enrich(RawOrder raw) {
            return Optional.ofNullable(customers.get(raw.customerId()))
                .orElseThrow(() -> new PipelineException("ENRICH",
                    "customer not found: " + raw.customerId()));
        }

        private Product lookupProduct(RawOrder raw) {
            return Optional.ofNullable(products.get(raw.productCode()))
                .orElseThrow(() -> new PipelineException("ENRICH",
                    "product not found: " + raw.productCode()));
        }
    }

    // -------------------------------------------------------------------------
    // Strategy implementations
    // -------------------------------------------------------------------------
    static class GstTaxStrategy implements TaxStrategy {
        private static final Map<String, Double> RATES = Map.of(
            "ELECTRONICS", 0.18,
            "CLOTHING",    0.05,
            "FOOD",        0.00,
            "SERVICES",    0.18
        );

        @Override
        public double taxRate(Product product) {
            return RATES.getOrDefault(product.category(), 0.12);
        }
    }

    static class TieredDiscountStrategy implements DiscountStrategy {
        private static final Map<String, Double> TIER_RATES = Map.of(
            "STANDARD",  0.00,
            "GOLD",      0.05,
            "PLATINUM",  0.10
        );
        private static final Map<String, Double> COUPON_RATES = Map.of(
            "SAVE10",    0.10,
            "SAVE20",    0.20,
            "WELCOME",   0.15
        );

        @Override
        public double discountRate(Customer customer, String couponCode) {
            double tierRate   = TIER_RATES.getOrDefault(customer.tier(), 0.0);
            double couponRate = couponCode != null
                ? COUPON_RATES.getOrDefault(couponCode.toUpperCase(), 0.0)
                : 0.0;
            // Take the better of the two (not cumulative)
            return Math.max(tierRate, couponRate);
        }
    }

    // -------------------------------------------------------------------------
    // Functional post-processing — pure Function / Predicate / Collector usage
    // -------------------------------------------------------------------------
    static Map<String, Double> revenueByCategory(List<PricedOrder> orders) {
        return orders.stream()
            .collect(Collectors.groupingBy(
                o -> o.product().category(),
                Collectors.summingDouble(PricedOrder::total)));
    }

    static List<PricedOrder> highValueOrders(List<PricedOrder> orders, double threshold) {
        Predicate<PricedOrder> isHighValue = o -> o.total() >= threshold;
        return orders.stream().filter(isHighValue).toList();
    }

    static Function<List<PricedOrder>, String> summaryReport() {
        return orders -> {
            double grandTotal = orders.stream().mapToDouble(PricedOrder::total).sum();
            long   count      = orders.size();
            double avgOrder   = count == 0 ? 0 : grandTotal / count;
            return String.format("Orders: %d | Grand total: ₹%.0f | Avg: ₹%.0f",
                count, grandTotal, avgOrder);
        };
    }

    // -------------------------------------------------------------------------
    // Main demonstration
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        // Test data
        Map<String, Customer> customers = Map.of(
            "C001", new Customer("C001", "Alice",   "PLATINUM"),
            "C002", new Customer("C002", "Bob",     "GOLD"),
            "C003", new Customer("C003", "Carol",   "STANDARD"),
            "C004", new Customer("C004", "Dave",    "GOLD")
        );

        Map<String, Product> products = Map.of(
            "LAPTOP",   new Product("LAPTOP",   "Pro Laptop",     75_000, "ELECTRONICS"),
            "SHIRT",    new Product("SHIRT",    "Cotton Shirt",    1_500, "CLOTHING"),
            "PHONE",    new Product("PHONE",    "Smartphone",     45_000, "ELECTRONICS"),
            "COFFEE",   new Product("COFFEE",   "Coffee Beans",      800, "FOOD")
        );

        List<String> completedIds = new ArrayList<>();
        Consumer<PricedOrder> onComplete = o -> completedIds.add(o.orderId());

        OrderProcessingPipeline pipeline = new OrderProcessingPipeline(
            customers, products,
            new GstTaxStrategy(),
            new TieredDiscountStrategy(),
            onComplete
        );

        List<RawOrder> rawOrders = List.of(
            new RawOrder("ORD-001", "C001", "LAPTOP", 1, null),      // PLATINUM, no coupon
            new RawOrder("ORD-002", "C002", "SHIRT",  3, "SAVE10"),  // GOLD, coupon
            new RawOrder("ORD-003", "C003", "PHONE",  1, "SAVE20"),  // STANDARD, coupon beats tier
            new RawOrder("ORD-004", "C004", "COFFEE", 5, "WELCOME"), // GOLD, coupon
            new RawOrder("ORD-005", "C001", "SHIRT",  2, "SAVE10")   // PLATINUM, coupon
        );

        System.out.println("=== Order Processing Pipeline ===");
        List<PricedOrder> processed = new ArrayList<>();

        for (RawOrder raw : rawOrders) {
            try {
                PricedOrder order = pipeline.execute(raw);
                processed.add(order);
                System.out.println(order.summary());
            } catch (PipelineException e) {
                System.out.println("[REJECTED] " + e.getMessage());
            }
        }

        // Error case
        System.out.println("\n--- Error handling ---");
        try {
            pipeline.execute(new RawOrder("ORD-999", "UNKNOWN", "LAPTOP", 1, null));
        } catch (PipelineException e) {
            System.out.println("[REJECTED] " + e.getMessage());
        }

        // Functional post-processing
        System.out.println("\n=== Revenue by Category ===");
        revenueByCategory(processed).entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .forEach(e -> System.out.printf("  %-14s ₹%.0f%n", e.getKey(), e.getValue()));

        System.out.println("\n=== High-Value Orders (≥ ₹10,000) ===");
        highValueOrders(processed, 10_000)
            .forEach(o -> System.out.println("  " + o.summary()));

        System.out.println("\n=== Summary ===");
        System.out.println("  " + summaryReport().apply(processed));
        System.out.println("  Completed IDs: " + completedIds);
    }
}
