package com.javatraining.datarest.repository;

import com.javatraining.datarest.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

/**
 * @RepositoryRestResource — customises the REST exposure of this repository.
 *
 *   collectionResourceRel: controls the key in _embedded when returning collections.
 *     Without this, Spring Data REST infers it from the entity class name.
 *     "products" → _embedded.products  (not _embedded.productList or similar)
 *
 *   path: the URL segment used for this resource.
 *     "products" → GET /api/products, GET /api/products/{id}, etc.
 *
 * Spring Data REST auto-generates:
 *   GET    /api/products              — paginated collection (HAL)
 *   POST   /api/products              — create
 *   GET    /api/products/{id}         — single resource
 *   PUT    /api/products/{id}         — full replace
 *   PATCH  /api/products/{id}         — partial update
 *   DELETE /api/products/{id}         — delete
 *   GET    /api/products/search       — lists exported search methods
 *   GET    /api/products/search/findByCategory?category=X  — custom query
 */
@RepositoryRestResource(collectionResourceRel = "products", path = "products")
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Derived query method — auto-exported as a search endpoint.
     * @Param gives the query parameter its name: ?category=Electronics
     *
     * Endpoint: GET /api/products/search/findByCategory?category=Electronics
     */
    List<Product> findByCategory(@Param("category") String category);

    /**
     * @RestResource(exported = false) — hides this method from the REST API.
     * It remains callable from Java code but is not reachable via HTTP.
     * Use this to prevent exposing internal query methods.
     */
    @RestResource(exported = false)
    List<Product> findByActiveTrue();
}
