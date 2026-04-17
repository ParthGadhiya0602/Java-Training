package com.javatraining.integration;

import java.util.List;
import java.util.Optional;

/**
 * Business-logic layer — validates input and delegates to the repository.
 * All validation throws {@link IllegalArgumentException} so the HTTP layer
 * can return 400 Bad Request.
 */
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    public Optional<Product> findById(long id) {
        return repository.findById(id);
    }

    public List<Product> findByCategory(String category) {
        return repository.findByCategory(category);
    }

    /**
     * Creates a new product after validating the input.
     * @throws IllegalArgumentException if name is blank or price is negative
     */
    public Product create(String name, double price, String category) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (price < 0)
            throw new IllegalArgumentException("price must be non-negative");
        String cat = (category == null) ? "" : category.strip();
        return repository.save(new Product(0, name.strip(), price, cat));
    }

    /**
     * Deletes a product by id.
     * @return {@code true} if the product existed and was deleted
     */
    public boolean deleteById(long id) {
        return repository.deleteById(id);
    }
}
