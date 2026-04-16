package com.javatraining.streams;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CollectorsDeepTest {

    List<CollectorsDeep.Order> orders;

    @BeforeEach
    void setUp() {
        orders = List.of(
            new CollectorsDeep.Order("O1", "Alice", "Electronics", 25000, true),
            new CollectorsDeep.Order("O2", "Bob",   "Clothing",      800, false),
            new CollectorsDeep.Order("O3", "Alice", "Food",          200, true),
            new CollectorsDeep.Order("O4", "Carol", "Electronics", 75000, true),
            new CollectorsDeep.Order("O5", "Bob",   "Electronics",  3500, false),
            new CollectorsDeep.Order("O6", "Carol", "Clothing",     1500, true),
            new CollectorsDeep.Order("O7", "Alice", "Food",          150, false)
        );
    }

    // ── groupingBy ────────────────────────────────────────────────────────────

    @Nested
    class GroupingByTests {

        @Test
        void byCategory_groups_all_orders() {
            Map<String, List<CollectorsDeep.Order>> groups =
                CollectorsDeep.byCategory(orders);
            assertEquals(3, groups.get("Electronics").size());
            assertEquals(2, groups.get("Clothing").size());
            assertEquals(2, groups.get("Food").size());
        }

        @Test
        void countByCategory_counts_per_group() {
            Map<String, Long> counts = CollectorsDeep.countByCategory(orders);
            assertEquals(3L, counts.get("Electronics"));
            assertEquals(2L, counts.get("Clothing"));
        }

        @Test
        void revenueByCategory_sums_amounts() {
            Map<String, Double> revenue = CollectorsDeep.revenueByCategory(orders);
            assertEquals(25000 + 75000 + 3500, revenue.get("Electronics"), 0.001);
            assertEquals(800 + 1500,           revenue.get("Clothing"),     0.001);
        }

        @Test
        void idsByCategory_collects_ids() {
            Map<String, List<String>> ids = CollectorsDeep.idsByCategory(orders);
            assertTrue(ids.get("Electronics").contains("O1"));
            assertTrue(ids.get("Electronics").contains("O4"));
        }

        @Test
        void maxAmountByCategory_finds_largest_order() {
            Map<String, Optional<CollectorsDeep.Order>> max =
                CollectorsDeep.maxAmountByCategory(orders);
            assertEquals("O4",
                max.get("Electronics").map(CollectorsDeep.Order::id).orElse("?"));
        }
    }

    // ── partitioningBy ────────────────────────────────────────────────────────

    @Nested
    class PartitioningByTests {

        @Test
        void paidVsUnpaid_splits_correctly() {
            Map<Boolean, List<CollectorsDeep.Order>> split =
                CollectorsDeep.paidVsUnpaid(orders);
            assertEquals(4, split.get(true).size());
            assertEquals(3, split.get(false).size());
        }

        @Test
        void highLowValueCount_counts_by_threshold() {
            Map<Boolean, Long> counts = CollectorsDeep.highLowValueCount(orders, 1000);
            // ≥ 1000: O1(25000),O4(75000),O5(3500),O6(1500) = 4
            assertEquals(4L, counts.get(true));
            assertEquals(3L, counts.get(false));
        }
    }

    // ── toMap ─────────────────────────────────────────────────────────────────

    @Nested
    class ToMapTests {

        @Test
        void byId_builds_lookup_map() {
            Map<String, CollectorsDeep.Order> map = CollectorsDeep.byId(orders);
            assertEquals(7, map.size());
            assertEquals("Electronics", map.get("O1").category());
        }

        @Test
        void spendByCustomer_merges_duplicate_customers() {
            Map<String, Double> spend = CollectorsDeep.spendByCustomer(orders);
            // Alice: O1(25000) + O3(200) + O7(150) = 25350
            assertEquals(25350.0, spend.get("Alice"), 0.001);
            // Bob: O2(800) + O5(3500) = 4300
            assertEquals(4300.0, spend.get("Bob"), 0.001);
        }
    }

    // ── joining ───────────────────────────────────────────────────────────────

    @Nested
    class JoiningTests {

        @Test
        void orderSummary_wrapped_and_delimited() {
            List<CollectorsDeep.Order> two = List.of(
                new CollectorsDeep.Order("O1","A","Cat",500,true),
                new CollectorsDeep.Order("O2","B","Cat",300,false)
            );
            String result = CollectorsDeep.orderSummary(two);
            assertTrue(result.startsWith("["));
            assertTrue(result.endsWith("]"));
            assertTrue(result.contains("O1=500"));
            assertTrue(result.contains("O2=300"));
        }
    }

    // ── summarizing ───────────────────────────────────────────────────────────

    @Nested
    class SummarizingTests {

        @Test
        void amountStats_correct_count_and_sum() {
            DoubleSummaryStatistics stats = CollectorsDeep.amountStats(orders);
            assertEquals(7, stats.getCount());
            assertEquals(25000+800+200+75000+3500+1500+150, stats.getSum(), 0.001);
            assertEquals(150.0, stats.getMin(), 0.001);
            assertEquals(75000.0, stats.getMax(), 0.001);
        }
    }

    // ── teeing ────────────────────────────────────────────────────────────────

    @Nested
    class TeeingTests {

        @Test
        void sumAndCount_computes_both_in_one_pass() {
            CollectorsDeep.SumCount sc = CollectorsDeep.sumAndCount(orders);
            assertEquals(7, sc.count());
            assertEquals(25000+800+200+75000+3500+1500+150, sc.sum(), 0.001);
            assertTrue(sc.average() > 0);
        }

        @Test
        void splitPaidUnpaid_partitions_correctly() {
            Map.Entry<List<CollectorsDeep.Order>, List<CollectorsDeep.Order>> split =
                CollectorsDeep.splitPaidUnpaid(orders);
            assertEquals(4, split.getKey().size());    // paid
            assertEquals(3, split.getValue().size());  // unpaid
        }
    }
}
