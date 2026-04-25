package com.javatraining.datarest.projection;

import com.javatraining.datarest.model.Product;
import org.springframework.data.rest.core.config.Projection;

import java.math.BigDecimal;

/**
 * Projection - a read-only view of an entity that limits the fields returned.
 *
 * @Projection(name, types):
 *   name  - the projection identifier used in the ?projection= query parameter
 *   types - the entity class this projection applies to
 *
 * Usage:
 *   GET /api/products/1?projection=summary
 *     → returns only id, name, price (no category, no active, no _links by default)
 *
 *   GET /api/products?projection=summary
 *     → all items in the collection are projected
 *
 * Projections are interface-based: Spring Data REST creates a proxy that delegates
 * each method to the matching getter on the entity.
 *
 * Excerpt projections (optional):
 *   @RepositoryRestResource(excerptProjection = ProductSummary.class)
 *   Applied automatically to COLLECTION resources - individual items in the list
 *   are shown as summaries. Full detail is still available at GET /api/products/{id}.
 */
@Projection(name = "summary", types = Product.class)
public interface ProductSummary {
    Long getId();
    String getName();
    BigDecimal getPrice();
    // category and active are intentionally omitted - this is a public price-list view
}
