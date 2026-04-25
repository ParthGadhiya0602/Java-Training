package com.javatraining.nosql;

import com.javatraining.nosql.document.Product;
import com.javatraining.nosql.repository.ProductRepository;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

/**
 * Demonstrates the MongoDB aggregation pipeline via {@link MongoTemplate}.
 *
 * <p>The aggregation pipeline is a sequence of stages, each transforming the
 * document stream:
 *
 * <pre>
 *   input documents
 *       │
 *       ▼  $match  - filter (like WHERE)
 *       │
 *       ▼  $group  - group by field, compute accumulators (COUNT, SUM, AVG, MAX)
 *       │
 *       ▼  $sort   - order results
 *       │
 *       ▼  $limit  - take first N
 *       │
 *       ▼  $project - reshape / add computed fields
 *       │
 *     output documents
 * </pre>
 *
 * <p>Result types are plain Java records or classes - Spring Data maps the
 * MongoDB output fields by name.
 */
@DataMongoTest
class MongoAggregationTest {

    @Autowired MongoTemplate    mongoTemplate;
    @Autowired ProductRepository products;

    /** Aggregation result: category name + document count in that category. */
    record CategoryCount(String id, long count) {}

    /**
     * Aggregation result: category name + total and average price.
     *
     * <p>{@code BigDecimal} (not {@code double}) because MongoDB returns Decimal128
     * for {@code $sum} / {@code $avg} of Decimal128 fields, and Spring Data MongoDB
     * maps Decimal128 → BigDecimal, not Decimal128 → double.
     */
    record CategoryPriceSummary(String id, BigDecimal totalPrice, BigDecimal avgPrice) {}

    /** Projection result: product name + price-with-tax. */
    record ProductWithTax(String name, double priceWithTax) {}

    @BeforeEach
    void seed() {
        products.deleteAll();
        products.saveAll(List.of(
            new Product("MacBook Pro",  "Electronics", new BigDecimal("2499.00"), true),
            new Product("iPhone 15",    "Electronics", new BigDecimal("999.00"),  true),
            new Product("Samsung TV",   "Electronics", new BigDecimal("799.00"),  false),
            new Product("Java Book",    "Books",       new BigDecimal("49.99"),   true),
            new Product("Spring Book",  "Books",       new BigDecimal("39.99"),   true),
            new Product("Standing Desk","Furniture",   new BigDecimal("599.00"),  true)
        ));
    }

    // ── $group - count ────────────────────────────────────────────────────────

    @Test
    void group_by_category_counts_documents_per_category() {
        AggregationResults<CategoryCount> results = mongoTemplate.aggregate(
            newAggregation(
                group("category").count().as("count"),
                sort(Sort.Direction.ASC, "count")
            ),
            Product.class,
            CategoryCount.class);

        List<CategoryCount> counts = results.getMappedResults();
        assertThat(counts).hasSize(3);
        // Furniture and Books have 1 and 2; Electronics has 3
        CategoryCount electronics = counts.stream()
            .filter(c -> "Electronics".equals(c.id())).findFirst().orElseThrow();
        assertThat(electronics.count()).isEqualTo(3L);
    }

    // ── $group - sum + avg ────────────────────────────────────────────────────

    @Test
    void group_computes_total_and_average_price_per_category() {
        AggregationResults<CategoryPriceSummary> results = mongoTemplate.aggregate(
            newAggregation(
                group("category")
                    .sum("price").as("totalPrice")
                    .avg("price").as("avgPrice"),
                sort(Sort.Direction.DESC, "totalPrice")
            ),
            Product.class,
            CategoryPriceSummary.class);

        List<CategoryPriceSummary> summaries = results.getMappedResults();
        // Electronics: 2499 + 999 + 799 = 4297 → highest total
        assertThat(summaries.get(0).id()).isEqualTo("Electronics");
        assertThat(summaries.get(0).totalPrice()).isEqualByComparingTo(new BigDecimal("4297.00"));
    }

    // ── $match + $group ───────────────────────────────────────────────────────

    @Test
    void match_then_group_counts_only_in_stock_products_per_category() {
        AggregationResults<CategoryCount> results = mongoTemplate.aggregate(
            newAggregation(
                match(Criteria.where("in_stock").is(true)),   // note: @Field name
                group("category").count().as("count"),
                sort(Sort.Direction.DESC, "count")
            ),
            Product.class,
            CategoryCount.class);

        List<CategoryCount> counts = results.getMappedResults();
        // Samsung TV is out of stock → Electronics drops to 2
        CategoryCount electronics = counts.stream()
            .filter(c -> "Electronics".equals(c.id())).findFirst().orElseThrow();
        assertThat(electronics.count()).isEqualTo(2L);
    }

    // ── $project - computed fields ────────────────────────────────────────────

    @Test
    void project_adds_computed_price_with_tax_field() {
        AggregationResults<ProductWithTax> results = mongoTemplate.aggregate(
            newAggregation(
                match(Criteria.where("category").is("Books")),
                project("name")
                    .andExpression("price * 1.2").as("priceWithTax"),
                sort(Sort.Direction.ASC, "priceWithTax")
            ),
            Product.class,
            ProductWithTax.class);

        List<ProductWithTax> books = results.getMappedResults();
        assertThat(books).hasSize(2);
        // Spring Book 39.99 * 1.2 = 47.988; Java Book 49.99 * 1.2 = 59.988
        assertThat(books.get(0).name()).isEqualTo("Spring Book");
        assertThat(books.get(0).priceWithTax()).isCloseTo(47.988, org.assertj.core.data.Offset.offset(0.01));
    }

    // ── $sort + $limit - top N ────────────────────────────────────────────────

    @Test
    void sort_and_limit_returns_top_two_most_expensive_products() {
        // Use raw Document output to avoid needing another result class
        AggregationResults<Document> results = mongoTemplate.aggregate(
            newAggregation(
                sort(Sort.Direction.DESC, "price"),
                limit(2),
                project("name", "price")
            ),
            Product.class,
            Document.class);

        List<Document> top2 = results.getMappedResults();
        assertThat(top2).hasSize(2);
        assertThat(top2.get(0).getString("name")).isEqualTo("MacBook Pro");
        assertThat(top2.get(1).getString("name")).isEqualTo("iPhone 15");
    }

    // ── MongoTemplate query (non-aggregation) ─────────────────────────────────

    @Test
    void mongo_template_query_with_criteria_uses_regex() {
        Query query = new Query(Criteria.where("name").regex("^S")); // starts with S
        List<Product> result = mongoTemplate.find(query, Product.class);

        assertThat(result).hasSize(3)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Samsung TV", "Spring Book", "Standing Desk");
    }
}
