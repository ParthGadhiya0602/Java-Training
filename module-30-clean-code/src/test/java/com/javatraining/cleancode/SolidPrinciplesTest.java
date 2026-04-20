package com.javatraining.cleancode;

import com.javatraining.cleancode.solid.dip.*;
import com.javatraining.cleancode.solid.isp.*;
import com.javatraining.cleancode.solid.lsp.*;
import com.javatraining.cleancode.solid.ocp.*;
import com.javatraining.cleancode.solid.srp.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for all five SOLID principles.
 * Each nested class isolates one principle.
 */
class SolidPrinciplesTest {

    // ═══════════════════════════════════════════════════════════════
    // S — Single Responsibility Principle
    // ═══════════════════════════════════════════════════════════════
    @Nested
    class SingleResponsibility {

        private final Order validOrder = new Order(
                "ORD-1", "Alice", "alice@example.com",
                List.of("Laptop", "Mouse"), 1500.0);

        @Test
        void validator_accepts_well_formed_order() {
            assertTrue(new OrderValidator().isValid(validOrder));
        }

        @Test
        void validator_rejects_order_with_no_items() {
            var empty = new Order("ORD-2", "Bob", "bob@example.com", List.of(), 0.0);
            assertThrows(IllegalArgumentException.class,
                    () -> new OrderValidator().validate(empty));
        }

        @Test
        void validator_rejects_invalid_email() {
            var bad = new Order("ORD-3", "Carol", "not-an-email", List.of("Book"), 20.0);
            assertThrows(IllegalArgumentException.class,
                    () -> new OrderValidator().validate(bad));
        }

        @Test
        void repository_saves_and_retrieves_by_id() {
            var repo = new OrderRepository();
            repo.save(validOrder);
            assertTrue(repo.findById("ORD-1").isPresent());
        }

        @Test
        void repository_returns_empty_for_unknown_id() {
            assertTrue(new OrderRepository().findById("UNKNOWN").isEmpty());
        }

        @Test
        void notification_message_contains_order_id_and_name() {
            String msg = new NotificationService().buildConfirmationMessage(validOrder);
            assertTrue(msg.contains("ORD-1"));
            assertTrue(msg.contains("Alice"));
        }

        @Test
        void invoice_contains_customer_and_total() {
            String invoice = new InvoiceGenerator().generate(validOrder);
            assertTrue(invoice.contains("ORD-1"));
            assertTrue(invoice.contains("1500.0"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // O — Open/Closed Principle
    // ═══════════════════════════════════════════════════════════════
    @Nested
    class OpenClosed {

        @Test
        void no_discount_policy_returns_full_price() {
            var engine = new PricingEngine(DiscountPolicies.NONE);
            assertEquals(100.0, engine.finalPrice(100.0));
        }

        @Test
        void regular_discount_deducts_ten_percent() {
            var engine = new PricingEngine(DiscountPolicies.regular());
            assertEquals(90.0, engine.finalPrice(100.0), 0.001);
        }

        @Test
        void vip_discount_deducts_twenty_percent() {
            var engine = new PricingEngine(DiscountPolicies.vip());
            assertEquals(80.0, engine.finalPrice(100.0), 0.001);
        }

        @Test
        void seasonal_flat_discount_deducts_fixed_amount() {
            var engine = new PricingEngine(DiscountPolicies.seasonal(15.0));
            assertEquals(85.0, engine.finalPrice(100.0), 0.001);
        }

        @Test
        void seasonal_discount_cannot_exceed_total() {
            // seasonal $200 on a $50 order → final price is 0, not negative
            var engine = new PricingEngine(DiscountPolicies.seasonal(200.0));
            assertEquals(0.0, engine.finalPrice(50.0), 0.001);
        }

        @Test
        void pricing_engine_reports_applied_policy_name() {
            var engine = new PricingEngine(DiscountPolicies.vip());
            assertTrue(engine.appliedPolicy().contains("VIP"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // L — Liskov Substitution Principle
    // ═══════════════════════════════════════════════════════════════
    @Nested
    class LiskovSubstitution {

        @Test
        void rectangle_area_is_width_times_height() {
            assertEquals(24.0, new Rectangle(4, 6).area());
        }

        @Test
        void square_area_is_side_squared() {
            assertEquals(25.0, new Square(5).area());
        }

        @Test
        void calculator_sums_mixed_shapes_transparently() {
            // ShapeCalculator works with any Shape — no instanceof checks needed
            List<Shape> shapes = List.of(
                    new Rectangle(3, 4),   // area = 12
                    new Square(5),         // area = 25
                    new Rectangle(2, 7)    // area = 14
            );
            assertEquals(51.0, new ShapeCalculator().totalArea(shapes));
        }

        @Test
        void calculator_sums_perimeters_correctly() {
            List<Shape> shapes = List.of(
                    new Rectangle(3, 4),   // perimeter = 14
                    new Square(5)          // perimeter = 20
            );
            assertEquals(34.0, new ShapeCalculator().totalPerimeter(shapes));
        }

        @Test
        void rectangle_rejects_zero_dimensions() {
            assertThrows(IllegalArgumentException.class, () -> new Rectangle(0, 5));
        }

        @Test
        void square_rejects_negative_side() {
            assertThrows(IllegalArgumentException.class, () -> new Square(-1));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // I — Interface Segregation Principle
    // ═══════════════════════════════════════════════════════════════
    @Nested
    class InterfaceSegregation {

        @Test
        void human_can_work_and_eat() {
            HumanWorker human = new HumanWorker("Alice");
            assertTrue(human.work().contains("Alice"));
            assertTrue(human.eat("pizza").contains("pizza"));
        }

        @Test
        void robot_can_work_and_charge() {
            RobotWorker robot = new RobotWorker("R2D2", 20);
            assertTrue(robot.work().contains("R2D2"));
            assertTrue(robot.charge(50).contains("70%"));
        }

        @Test
        void robot_battery_caps_at_100() {
            RobotWorker robot = new RobotWorker("C3PO", 80);
            robot.charge(50);
            assertEquals(100, robot.batteryLevel());
        }

        @Test
        void human_implements_workable_interface() {
            Workable w = new HumanWorker("Bob");
            assertNotNull(w.work());
        }

        @Test
        void robot_implements_workable_interface() {
            // Both human and robot satisfy Workable — ISP payoff:
            // code that only needs Workable can accept both
            Workable w = new RobotWorker("Bot", 100);
            assertNotNull(w.work());
        }

        @Test
        void robot_does_not_implement_feedable() {
            // Compile-time guarantee: RobotWorker is not a Feedable
            assertFalse(new RobotWorker("X", 50) instanceof Feedable);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // D — Dependency Inversion Principle
    // ═══════════════════════════════════════════════════════════════
    @Nested
    class DependencyInversion {

        @Test
        void alert_service_sends_via_email_when_injected_email_sender() {
            AlertService service = new AlertService(new EmailMessageSender());
            String result = service.sendAlert("admin@example.com", "Server down");
            assertTrue(result.startsWith("EMAIL"));
            assertTrue(result.contains("Server down"));
        }

        @Test
        void alert_service_sends_via_sms_when_injected_sms_sender() {
            AlertService service = new AlertService(new SmsMessageSender());
            String result = service.sendAlert("+1-555-0100", "Server down");
            assertTrue(result.startsWith("SMS"));
        }

        @Test
        void swapping_sender_requires_zero_changes_to_alert_service() {
            // Same AlertService class; different behaviour via injection — DIP payoff
            MessageSender spy = (recipient, message) -> "MOCK → " + recipient + ": " + message;
            AlertService service = new AlertService(spy);
            assertTrue(service.sendAlert("x", "y").startsWith("MOCK"));
        }

        @Test
        void broadcast_sends_to_all_recipients() {
            AlertService service = new AlertService(new EmailMessageSender());
            var results = service.broadcastAlert(
                    List.of("a@x.com", "b@x.com", "c@x.com"), "Deploy started");
            assertEquals(3, results.size());
            assertTrue(results.stream().allMatch(r -> r.contains("Deploy started")));
        }
    }
}
