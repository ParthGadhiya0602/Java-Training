package com.javatraining.cleancode;

import com.javatraining.cleancode.smells.EmailAddress;
import com.javatraining.cleancode.smells.Money;
import com.javatraining.cleancode.smells.ReportBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests demonstrating refactored code (code smells fixed).
 * Each nested class maps to one smell and its fix.
 */
class CodeSmellsTest {

    // ═══════════════════════════════════════════════════════════════
    // Smell: Primitive Obsession → Fix: EmailAddress value object
    // ═══════════════════════════════════════════════════════════════
    @Nested
    class PrimitiveObsession_EmailAddress {

        @Test
        void valid_email_is_created_successfully() {
            var email = new EmailAddress("alice@example.com");
            assertEquals("alice@example.com", email.value());
        }

        @Test
        void domain_and_local_part_are_parsed_correctly() {
            var email = new EmailAddress("alice@example.com");
            assertEquals("alice",       email.localPart());
            assertEquals("example.com", email.domain());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  ", "no-at-sign", "@nodomain", "nolocal@"})
        void invalid_formats_are_rejected(String bad) {
            assertThrows(IllegalArgumentException.class, () -> new EmailAddress(bad));
        }

        @Test
        void null_email_is_rejected() {
            assertThrows(IllegalArgumentException.class, () -> new EmailAddress(null));
        }

        @Test
        void two_email_addresses_with_same_value_are_equal() {
            // Records give value-based equals automatically
            assertEquals(new EmailAddress("x@y.com"), new EmailAddress("x@y.com"));
        }

        @Test
        void toString_returns_the_email_string() {
            assertEquals("dev@test.io", new EmailAddress("dev@test.io").toString());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Smell: Data Clump + floating-point → Fix: Money value object
    // ═══════════════════════════════════════════════════════════════
    @Nested
    class DataClump_Money {

        @Test
        void money_of_factory_creates_correct_value() {
            var price = Money.of(9.99, "USD");
            assertEquals(new BigDecimal("9.99"), price.amount());
            assertEquals("USD", price.currency());
        }

        @Test
        void add_sums_amounts_same_currency() {
            var a = Money.of(10.00, "USD");
            var b = Money.of(5.50,  "USD");
            assertEquals(Money.of(15.50, "USD"), a.add(b));
        }

        @Test
        void subtract_returns_difference() {
            var a = Money.of(20.00, "USD");
            var b = Money.of(7.50,  "USD");
            assertEquals(Money.of(12.50, "USD"), a.subtract(b));
        }

        @Test
        void subtract_throws_when_result_would_be_negative() {
            var a = Money.of(5.00, "USD");
            var b = Money.of(10.00, "USD");
            assertThrows(IllegalArgumentException.class, () -> a.subtract(b));
        }

        @Test
        void add_throws_on_currency_mismatch() {
            var usd = Money.of(10.00, "USD");
            var eur = Money.of(10.00, "EUR");
            assertThrows(IllegalArgumentException.class, () -> usd.add(eur));
        }

        @Test
        void multiply_scales_amount() {
            assertEquals(Money.of(30.00, "USD"), Money.of(10.00, "USD").multiply(3));
        }

        @Test
        void is_greater_than_compares_amounts() {
            assertTrue(Money.of(20.00, "USD").isGreaterThan(Money.of(10.00, "USD")));
            assertFalse(Money.of(5.00, "USD").isGreaterThan(Money.of(10.00, "USD")));
        }

        @Test
        void zero_factory_creates_zero_amount() {
            var zero = Money.zero("GBP");
            assertEquals(BigDecimal.ZERO.setScale(2), zero.amount());
        }

        @Test
        void negative_amount_is_rejected() {
            assertThrows(IllegalArgumentException.class, () -> Money.of(-1.00, "USD"));
        }

        @Test
        void toString_includes_currency_and_amount() {
            assertTrue(Money.of(42.50, "USD").toString().contains("USD"));
            assertTrue(Money.of(42.50, "USD").toString().contains("42.50"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Smell: Duplicate Code + Magic Numbers → Fix: Extract Method + constants
    // ═══════════════════════════════════════════════════════════════
    @Nested
    class DuplicateCode_ReportBuilder {

        private final ReportBuilder builder = new ReportBuilder("Sales");
        private final List<String> items = List.of("Widget A", "Widget B", "Widget C");

        @Test
        void text_report_contains_all_items() {
            String report = builder.generateTextReport(items);
            assertTrue(report.contains("Widget A"));
            assertTrue(report.contains("Widget B"));
            assertTrue(report.contains("Widget C"));
        }

        @Test
        void csv_report_contains_all_items() {
            String csv = builder.generateCsvReport(items);
            assertTrue(csv.contains("Widget A"));
            assertTrue(csv.contains("Widget C"));
        }

        @Test
        void summary_contains_item_count() {
            String summary = builder.generateSummary(items);
            assertTrue(summary.contains("3"));
        }

        @Test
        void all_formats_share_same_header_title() {
            // Extracted buildHeader() is called by all generators - one source of truth
            String text    = builder.generateTextReport(items);
            String csv     = builder.generateCsvReport(items);
            String summary = builder.generateSummary(items);

            // All contain the title "Sales"
            assertTrue(text.contains("Sales"));
            assertTrue(csv.contains("Sales"));
            assertTrue(summary.contains("Sales"));
        }

        @Test
        void all_formats_share_same_footer_structure() {
            String text    = builder.generateTextReport(items);
            String csv     = builder.generateCsvReport(items);
            String summary = builder.generateSummary(items);

            // Extracted buildFooter() ensures consistent END marker
            assertTrue(text.contains("END"));
            assertTrue(csv.contains("END"));
            assertTrue(summary.contains("END"));
        }

        @Test
        void text_report_shows_overflow_notice_beyond_max_items() {
            // MAX_ITEMS_PER_PAGE = 100 - named constant, not a magic number
            List<String> bigList = java.util.stream.IntStream.rangeClosed(1, 110)
                    .mapToObj(i -> "Item " + i).toList();
            String report = builder.generateTextReport(bigList);
            assertTrue(report.contains("10 more"), "Should mention overflow count");
        }
    }
}
