package com.javatraining.integration;

import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for products.
 * Both the in-memory and the JDBC implementations satisfy this interface.
 */
public interface ProductRepository {
    /** Persist a product.  If {@code product.id() == 0} a new id is assigned. */
    Product        save(Product product);
    Optional<Product> findById(long id);
    List<Product>  findAll();
    List<Product>  findByCategory(String category);
    boolean        deleteById(long id);
    int            count();
}
