package com.javatraining.nosql.repository;

import com.javatraining.nosql.document.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link Product}.
 *
 * <p>Derived query methods work the same as with JPA - Spring parses the method
 * name and generates a MongoDB query.  The difference:
 * <ul>
 *   <li>JPA generates JPQL/SQL; this generates a MongoDB JSON query document.</li>
 *   <li>{@code @Query} uses MongoDB's JSON filter syntax instead of JPQL.</li>
 *   <li>{@code findByTagsContaining} queries inside an embedded array - no join needed.</li>
 * </ul>
 */
public interface ProductRepository extends MongoRepository<Product, String> {

    // ── Derived query methods ─────────────────────────────────────────────────

    /** { "name": ?0 } */
    Optional<Product> findByName(String name);

    /** { "category": ?0 } */
    List<Product> findByCategory(String category);

    /** { "price": { "$lt": ?0 } } */
    List<Product> findByPriceLessThan(BigDecimal price);

    /** { "price": { "$gt": ?0 } } */
    List<Product> findByPriceGreaterThan(BigDecimal price);

    /** { "in_stock": ?0 } - uses the @Field name */
    List<Product> findByInStock(boolean inStock);

    /** { "tags": ?0 } - MongoDB's array contains operator */
    List<Product> findByTagsContaining(String tag);

    /** { } sorted by price ASC */
    List<Product> findAllByOrderByPriceAsc();

    /** { "category": ?0 } sorted by price ASC */
    List<Product> findByCategoryOrderByPriceAsc(String category);

    // ── @Query - MongoDB JSON filter ──────────────────────────────────────────

    /**
     * Range query using MongoDB's {@code $gte} and {@code $lte} operators.
     *
     * <p><b>Why {@link Decimal128} instead of {@link BigDecimal}?</b>
     * Spring Data MongoDB's {@code @Query} parameter substitution serializes
     * {@code BigDecimal} as a plain JSON string ({@code "500.00"}).
     * MongoDB cannot compare a Decimal128 stored field with a string.
     * Passing {@link Decimal128} (a native BSON type) ensures the parameter is
     * encoded as {@code $numberDecimal} in the wire protocol, enabling the comparison.
     *
     * <p>Derived query methods ({@code findByPriceLessThan}) avoid this issue
     * because they go through the entity mapping layer which applies the
     * {@code @Field(targetType = DECIMAL128)} conversion.
     */
    @Query("{ 'price': { '$gte': ?0, '$lte': ?1 } }")
    List<Product> findByPriceRange(Decimal128 min, Decimal128 max);

    /**
     * Array query using {@code $all} - product must have ALL listed tags.
     */
    @Query("{ 'tags': { '$all': ?0 } }")
    List<Product> findByAllTags(List<String> tags);

    /**
     * Compound filter: category match AND only available products.
     * Note: uses the stored field name {@code in_stock}.
     */
    @Query("{ 'category': ?0, 'in_stock': true }")
    List<Product> findAvailableByCategory(String category);
}
