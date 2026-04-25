package com.javatraining.nosql;

import com.javatraining.nosql.document.Product;
import com.javatraining.nosql.repository.ProductRepository;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for derived query methods and {@code @Query} JSON filters.
 *
 * <p>{@code @DataMongoTest} loads only MongoDB-related beans (documents, repositories,
 * {@link org.springframework.data.mongodb.core.MongoTemplate}).  When
 * {@code de.flapdoodle.embed.mongo.spring3x} is on the test classpath, Spring Boot
 * auto-configures an in-process embedded MongoDB - no external server needed.
 *
 * <p>Each test rolls back through collection cleanup in {@code @BeforeEach}.
 */
@DataMongoTest
class MongoRepositoryTest {

    @Autowired ProductRepository products;
    @Autowired MongoTemplate      mongoTemplate;

    @BeforeEach
    void clean() {
        products.deleteAll();
    }

    void seed() {
        products.saveAll(List.of(
            new Product("MacBook Pro",  "Electronics", new BigDecimal("2499.00"), true)
                .tag("laptop", "apple", "premium"),
            new Product("iPhone 15",    "Electronics", new BigDecimal("999.00"),  true)
                .tag("phone", "apple"),
            new Product("Samsung TV",   "Electronics", new BigDecimal("799.00"),  false)
                .tag("tv", "samsung"),
            new Product("Java Book",    "Books",       new BigDecimal("49.99"),   true)
                .tag("programming", "java"),
            new Product("Spring Book",  "Books",       new BigDecimal("39.99"),   true)
                .tag("programming", "java", "spring"),
            new Product("Standing Desk","Furniture",   new BigDecimal("599.00"),  true)
                .tag("office", "ergonomic")
        ));
    }

    // ── Derived queries ───────────────────────────────────────────────────────

    @Test
    void save_assigns_mongodb_id() {
        Product p = products.save(
            new Product("Widget", "Misc", new BigDecimal("9.99"), true));
        assertThat(p.getId()).isNotNull().isNotEmpty();
    }

    @Test
    void findByName_returns_matching_product() {
        seed();
        Optional<Product> result = products.findByName("iPhone 15");
        assertThat(result).isPresent();
        assertThat(result.get().getCategory()).isEqualTo("Electronics");
    }

    @Test
    void findByCategory_returns_all_in_category() {
        seed();
        List<Product> electronics = products.findByCategory("Electronics");
        assertThat(electronics).hasSize(3)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("MacBook Pro", "iPhone 15", "Samsung TV");
    }

    @Test
    void findByPriceLessThan_filters_correctly() {
        seed();
        List<Product> cheap = products.findByPriceLessThan(new BigDecimal("100.00"));
        assertThat(cheap).hasSize(2)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Java Book", "Spring Book");
    }

    @Test
    void findByInStock_returns_only_available() {
        seed();
        List<Product> available = products.findByInStock(true);
        assertThat(available).hasSize(5).allMatch(Product::isInStock);
    }

    @Test
    void findByTagsContaining_queries_inside_embedded_array() {
        seed();
        // "java" tag appears in Java Book and Spring Book
        List<Product> javaTagged = products.findByTagsContaining("java");
        assertThat(javaTagged).hasSize(2)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Java Book", "Spring Book");
    }

    @Test
    void findAllByOrderByPriceAsc_returns_sorted_list() {
        seed();
        List<Product> sorted = products.findAllByOrderByPriceAsc();
        List<BigDecimal> prices = sorted.stream().map(Product::getPrice).toList();
        for (int i = 0; i < prices.size() - 1; i++) {
            assertThat(prices.get(i)).isLessThanOrEqualTo(prices.get(i + 1));
        }
    }

    @Test
    void findByCategoryOrderByPriceAsc_filters_and_sorts() {
        seed();
        List<Product> books = products.findByCategoryOrderByPriceAsc("Books");
        assertThat(books).hasSize(2);
        assertThat(books.get(0).getName()).isEqualTo("Spring Book"); // 39.99 < 49.99
        assertThat(books.get(1).getName()).isEqualTo("Java Book");
    }

    // ── @Query JSON filter ────────────────────────────────────────────────────

    @Test
    void query_price_range_with_decimal128_params_returns_correct_products() {
        // @Query with BigDecimal params serializes as strings, breaking Decimal128 comparison.
        // Passing Decimal128 (native BSON type) encodes as $numberDecimal on the wire.
        seed();
        // $gte: 500, $lte: 1000 → Samsung TV (799), iPhone 15 (999), Standing Desk (599)
        List<Product> result = products.findByPriceRange(
                new Decimal128(new BigDecimal("500.00")),
                new Decimal128(new BigDecimal("1000.00")));
        assertThat(result).hasSize(3)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("iPhone 15", "Samsung TV", "Standing Desk");
    }

    @Test
    void query_all_tags_requires_every_listed_tag() {
        seed();
        // $all: ["java", "spring"] → only Spring Book has BOTH tags
        List<Product> result = products.findByAllTags(List.of("java", "spring"));
        assertThat(result).hasSize(1)
                .extracting(Product::getName)
                .containsExactly("Spring Book");
    }

    @Test
    void query_available_by_category_combines_two_filters() {
        seed();
        // Electronics AND in_stock=true → MacBook Pro + iPhone 15 (Samsung TV is out of stock)
        List<Product> result = products.findAvailableByCategory("Electronics");
        assertThat(result).hasSize(2)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("MacBook Pro", "iPhone 15");
    }
}
