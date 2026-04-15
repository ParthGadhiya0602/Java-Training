package com.javatraining.interfaces;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderPipelineTest {

    private Map<String, OrderPipeline.Customer> customers;
    private Map<String, OrderPipeline.Product>  products;
    private OrderPipeline.OrderProcessingPipeline pipeline;
    private List<OrderPipeline.PricedOrder> completed;

    @BeforeEach
    void setup() {
        customers = Map.of(
            "C001", new OrderPipeline.Customer("C001", "Alice",  "PLATINUM"),
            "C002", new OrderPipeline.Customer("C002", "Bob",    "GOLD"),
            "C003", new OrderPipeline.Customer("C003", "Carol",  "STANDARD")
        );
        products = Map.of(
            "LAPTOP",  new OrderPipeline.Product("LAPTOP",  "Laptop",  75_000, "ELECTRONICS"),
            "SHIRT",   new OrderPipeline.Product("SHIRT",   "Shirt",    1_500, "CLOTHING"),
            "COFFEE",  new OrderPipeline.Product("COFFEE",  "Coffee",     800, "FOOD")
        );
        completed = new ArrayList<>();
        pipeline = new OrderPipeline.OrderProcessingPipeline(
            customers, products,
            new OrderPipeline.GstTaxStrategy(),
            new OrderPipeline.TieredDiscountStrategy(),
            completed::add
        );
    }

    // -----------------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------------
    @Test
    void valid_order_produces_priced_order() {
        OrderPipeline.RawOrder raw = new OrderPipeline.RawOrder(
            "ORD-001", "C001", "LAPTOP", 1, null);
        OrderPipeline.PricedOrder order = pipeline.execute(raw);
        assertNotNull(order);
        assertEquals("ORD-001", order.orderId());
    }

    @Test
    void subtotal_is_price_times_quantity() {
        OrderPipeline.RawOrder raw = new OrderPipeline.RawOrder(
            "ORD-001", "C001", "SHIRT", 3, null);
        OrderPipeline.PricedOrder order = pipeline.execute(raw);
        assertEquals(4_500.0, order.subtotal(), 1e-9);  // 1500 * 3
    }

    @Test
    void electronics_tax_is_18_percent() {
        OrderPipeline.RawOrder raw = new OrderPipeline.RawOrder(
            "ORD-001", "C003", "LAPTOP", 1, null);
        OrderPipeline.PricedOrder order = pipeline.execute(raw);
        assertEquals(75_000 * 0.18, order.taxAmount(), 1e-9);
    }

    @Test
    void clothing_tax_is_5_percent() {
        OrderPipeline.RawOrder raw = new OrderPipeline.RawOrder(
            "ORD-001", "C003", "SHIRT", 1, null);
        OrderPipeline.PricedOrder order = pipeline.execute(raw);
        assertEquals(1_500 * 0.05, order.taxAmount(), 1e-9);
    }

    @Test
    void food_has_zero_tax() {
        OrderPipeline.RawOrder raw = new OrderPipeline.RawOrder(
            "ORD-001", "C003", "COFFEE", 1, null);
        OrderPipeline.PricedOrder order = pipeline.execute(raw);
        assertEquals(0.0, order.taxAmount(), 1e-9);
    }

    @Test
    void platinum_customer_gets_10_percent_discount() {
        // PLATINUM tier = 10%; no coupon
        OrderPipeline.RawOrder raw = new OrderPipeline.RawOrder(
            "ORD-001", "C001", "SHIRT", 1, null);
        OrderPipeline.PricedOrder order = pipeline.execute(raw);
        assertEquals(1_500 * 0.10, order.discountAmount(), 1e-9);
    }

    @Test
    void coupon_save20_beats_gold_tier() {
        // GOLD=5%, SAVE20=20% → discount should be 20%
        OrderPipeline.RawOrder raw = new OrderPipeline.RawOrder(
            "ORD-001", "C002", "SHIRT", 1, "SAVE20");
        OrderPipeline.PricedOrder order = pipeline.execute(raw);
        assertEquals(1_500 * 0.20, order.discountAmount(), 1e-9);
    }

    @Test
    void total_is_subtotal_plus_tax_minus_discount() {
        OrderPipeline.RawOrder raw = new OrderPipeline.RawOrder(
            "ORD-001", "C003", "SHIRT", 2, null);
        OrderPipeline.PricedOrder order = pipeline.execute(raw);
        double expected = order.subtotal() + order.taxAmount() - order.discountAmount();
        assertEquals(expected, order.total(), 1e-9);
    }

    @Test
    void consumer_is_called_on_completion() {
        pipeline.execute(new OrderPipeline.RawOrder("ORD-X", "C001", "SHIRT", 1, null));
        assertEquals(1, completed.size());
        assertEquals("ORD-X", completed.get(0).orderId());
    }

    // -----------------------------------------------------------------------
    // Validation errors
    // -----------------------------------------------------------------------
    @Test
    void blank_order_id_throws_pipeline_exception() {
        assertThrows(OrderPipeline.PipelineException.class, () ->
            pipeline.execute(new OrderPipeline.RawOrder("", "C001", "SHIRT", 1, null)));
    }

    @Test
    void zero_quantity_throws() {
        assertThrows(OrderPipeline.PipelineException.class, () ->
            pipeline.execute(new OrderPipeline.RawOrder("ORD-1", "C001", "SHIRT", 0, null)));
    }

    @Test
    void negative_quantity_throws() {
        assertThrows(OrderPipeline.PipelineException.class, () ->
            pipeline.execute(new OrderPipeline.RawOrder("ORD-1", "C001", "SHIRT", -1, null)));
    }

    @Test
    void unknown_customer_throws() {
        assertThrows(OrderPipeline.PipelineException.class, () ->
            pipeline.execute(new OrderPipeline.RawOrder("ORD-1", "UNKNOWN", "SHIRT", 1, null)));
    }

    @Test
    void unknown_product_throws() {
        assertThrows(OrderPipeline.PipelineException.class, () ->
            pipeline.execute(new OrderPipeline.RawOrder("ORD-1", "C001", "UNKNOWN", 1, null)));
    }

    // -----------------------------------------------------------------------
    // Post-processing functions
    // -----------------------------------------------------------------------
    @Test
    void revenue_by_category_sums_totals() {
        pipeline.execute(new OrderPipeline.RawOrder("A", "C003", "SHIRT",  1, null));
        pipeline.execute(new OrderPipeline.RawOrder("B", "C003", "COFFEE", 1, null));
        pipeline.execute(new OrderPipeline.RawOrder("C", "C003", "SHIRT",  2, null));

        Map<String, Double> rev = OrderPipeline.revenueByCategory(completed);
        assertTrue(rev.containsKey("CLOTHING"));
        assertTrue(rev.containsKey("FOOD"));
        // Three SHIRT orders: totals include tax
        assertTrue(rev.get("CLOTHING") > 0);
    }

    @Test
    void high_value_orders_filters_correctly() {
        pipeline.execute(new OrderPipeline.RawOrder("A", "C003", "LAPTOP", 1, null)); // high
        pipeline.execute(new OrderPipeline.RawOrder("B", "C003", "SHIRT",  1, null)); // low

        List<OrderPipeline.PricedOrder> high =
            OrderPipeline.highValueOrders(completed, 10_000);
        assertEquals(1, high.size());
        assertEquals("A", high.get(0).orderId());
    }

    @Test
    void summary_report_shows_count_and_total() {
        pipeline.execute(new OrderPipeline.RawOrder("A", "C003", "SHIRT", 1, null));
        pipeline.execute(new OrderPipeline.RawOrder("B", "C003", "SHIRT", 2, null));

        String report = OrderPipeline.summaryReport().apply(completed);
        assertTrue(report.contains("2"));      // count
        assertTrue(report.contains("Orders")); // label
    }

    // -----------------------------------------------------------------------
    // GstTaxStrategy — standalone
    // -----------------------------------------------------------------------
    @Test
    void gst_electronics_18() {
        OrderPipeline.GstTaxStrategy gst = new OrderPipeline.GstTaxStrategy();
        OrderPipeline.Product p =
            new OrderPipeline.Product("X", "X", 1000, "ELECTRONICS");
        assertEquals(0.18, gst.taxRate(p), 1e-9);
        assertEquals(180.0, gst.taxAmount(p, 1000), 1e-9);
    }

    @Test
    void gst_food_zero() {
        OrderPipeline.GstTaxStrategy gst = new OrderPipeline.GstTaxStrategy();
        OrderPipeline.Product p =
            new OrderPipeline.Product("X", "X", 1000, "FOOD");
        assertEquals(0.0, gst.taxRate(p), 1e-9);
    }

    // -----------------------------------------------------------------------
    // TieredDiscountStrategy — standalone
    // -----------------------------------------------------------------------
    @Test
    void platinum_tier_discount_is_10() {
        OrderPipeline.TieredDiscountStrategy ds = new OrderPipeline.TieredDiscountStrategy();
        OrderPipeline.Customer c = new OrderPipeline.Customer("X", "X", "PLATINUM");
        assertEquals(0.10, ds.discountRate(c, null), 1e-9);
    }

    @Test
    void coupon_save20_beats_gold() {
        OrderPipeline.TieredDiscountStrategy ds = new OrderPipeline.TieredDiscountStrategy();
        OrderPipeline.Customer gold = new OrderPipeline.Customer("X", "X", "GOLD");
        assertEquals(0.20, ds.discountRate(gold, "SAVE20"), 1e-9);
    }

    @Test
    void unknown_coupon_falls_back_to_tier() {
        OrderPipeline.TieredDiscountStrategy ds = new OrderPipeline.TieredDiscountStrategy();
        OrderPipeline.Customer gold = new OrderPipeline.Customer("X", "X", "GOLD");
        assertEquals(0.05, ds.discountRate(gold, "INVALID_COUPON"), 1e-9);
    }
}
