package com.javatraining.streams;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StreamBasicsTest {

    List<StreamBasics.Product> products;

    @BeforeEach
    void setUp() {
        products = List.of(
            new StreamBasics.Product("Laptop",    "Electronics", 75000.0, 10),
            new StreamBasics.Product("Phone",     "Electronics", 25000.0,  5),
            new StreamBasics.Product("Shirt",     "Clothing",      800.0, 20),
            new StreamBasics.Product("Jeans",     "Clothing",     1500.0,  0),
            new StreamBasics.Product("Rice",      "Food",          120.0, 50),
            new StreamBasics.Product("Headphones","Electronics",  3500.0, 15)
        );
    }

    // ── filter ────────────────────────────────────────────────────────────────

    @Nested
    class FilterTests {

        @Test
        void inStock_excludes_zero_stock() {
            List<String> names = StreamBasics.names(StreamBasics.inStock(products));
            assertFalse(names.contains("Jeans"));
            assertTrue(names.contains("Laptop"));
        }

        @Test
        void byCategory_case_insensitive() {
            List<String> names = StreamBasics.names(
                StreamBasics.byCategory(products, "clothing"));
            assertEquals(2, names.size());
            assertTrue(names.contains("Shirt"));
            assertTrue(names.contains("Jeans"));
        }

        @Test
        void affordable_filters_by_max_price() {
            List<String> names = StreamBasics.names(
                StreamBasics.affordable(products, 2000.0));
            assertTrue(names.contains("Shirt"));
            assertTrue(names.contains("Jeans"));
            assertTrue(names.contains("Rice"));
            assertFalse(names.contains("Laptop"));
        }
    }

    // ── map ───────────────────────────────────────────────────────────────────

    @Nested
    class MapTests {

        @Test
        void names_extracts_all_product_names() {
            List<String> names = StreamBasics.names(products);
            assertEquals(6, names.size());
            assertTrue(names.contains("Laptop"));
        }

        @Test
        void discountedPrices_applies_percentage() {
            List<Double> prices = StreamBasics.discountedPrices(products, 10.0);
            assertEquals(6, prices.size());
            assertEquals(67500.0, prices.get(0), 0.001);  // Laptop: 75000 * 0.9
        }

        @Test
        void upperCaseNames_converts_all() {
            List<String> result = StreamBasics.upperCaseNames(List.of("apple", "banana"));
            assertEquals(List.of("APPLE", "BANANA"), result);
        }
    }

    // ── sorted / distinct / limit / skip ─────────────────────────────────────

    @Nested
    class OrderingTests {

        @Test
        void sortedByPrice_ascending() {
            List<StreamBasics.Product> sorted = StreamBasics.sortedByPrice(products);
            assertEquals("Rice", sorted.get(0).name());
            assertEquals("Laptop", sorted.get(sorted.size() - 1).name());
        }

        @Test
        void top3ByPrice_returns_three_most_expensive() {
            List<String> names = StreamBasics.names(StreamBasics.top3ByPrice(products));
            assertEquals(3, names.size());
            assertEquals("Laptop", names.get(0));
        }

        @Test
        void distinctWords_removes_duplicates_preserves_order() {
            List<String> result = StreamBasics.distinctWords(
                List.of("Apple", "banana", "apple", "Cherry", "BANANA"));
            assertEquals(List.of("Apple", "banana", "Cherry"), result);
        }

        @Test
        void page_returns_correct_slice() {
            List<Integer> items = List.of(1,2,3,4,5,6,7,8,9,10);
            assertEquals(List.of(4,5,6), StreamBasics.page(items, 1, 3)); // skip 3, take 3
        }

        @Test
        void page_last_page_partial() {
            List<Integer> items = List.of(1,2,3,4,5);
            assertEquals(List.of(4,5), StreamBasics.page(items, 1, 3)); // only 2 left
        }
    }

    // ── terminal ops ──────────────────────────────────────────────────────────

    @Nested
    class TerminalTests {

        @Test
        void countInStock_excludes_zero_stock() {
            assertEquals(5, StreamBasics.countInStock(products));
        }

        @Test
        void totalPrice_sums_all_prices() {
            assertEquals(75000 + 25000 + 800 + 1500 + 120 + 3500,
                StreamBasics.totalPrice(products), 0.001);
        }

        @Test
        void cheapest_returns_min_price_product() {
            assertEquals("Rice",
                StreamBasics.cheapest(products).map(StreamBasics.Product::name).orElse("?"));
        }

        @Test
        void mostExpensive_returns_max_price_product() {
            assertEquals("Laptop",
                StreamBasics.mostExpensive(products).map(StreamBasics.Product::name).orElse("?"));
        }

        @Test
        void anyExpensive_true_when_above_threshold() {
            assertTrue(StreamBasics.anyExpensive(products, 50000));
        }

        @Test
        void anyExpensive_false_when_all_below() {
            assertFalse(StreamBasics.anyExpensive(products, 100000));
        }

        @Test
        void allInStock_false_when_jeans_out() {
            assertFalse(StreamBasics.allInStock(products));
        }

        @Test
        void noneOutOfStock_false() {
            assertFalse(StreamBasics.noneOutOfStock(products));
        }

        @Test
        void firstByCategory_returns_first_match() {
            Optional<StreamBasics.Product> first =
                StreamBasics.firstByCategory(products, "Food");
            assertTrue(first.isPresent());
            assertEquals("Food", first.get().category());
        }
    }

    // ── joining ───────────────────────────────────────────────────────────────

    @Nested
    class JoiningTests {

        @Test
        void joinNames_uses_delimiter() {
            List<StreamBasics.Product> two = List.of(
                new StreamBasics.Product("A", "Cat", 1.0, 1),
                new StreamBasics.Product("B", "Cat", 2.0, 1)
            );
            assertEquals("A | B", StreamBasics.joinNames(two, " | "));
        }

        @Test
        void csvLine_wraps_in_quotes_and_joins_with_commas() {
            assertEquals("\"Alice,30,Eng\"", StreamBasics.csvLine(List.of("Alice","30","Eng")));
        }
    }
}
