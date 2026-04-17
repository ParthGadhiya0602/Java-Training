package com.javatraining.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe, in-memory ProductRepository.
 * Used as the backing store for the embedded HTTP server during REST-assured tests.
 */
public class InMemoryProductRepository implements ProductRepository {

    private final ConcurrentHashMap<Long, Product> store    = new ConcurrentHashMap<>();
    private final AtomicLong                       sequence = new AtomicLong(1);

    @Override
    public Product save(Product product) {
        long id = product.id() == 0 ? sequence.getAndIncrement() : product.id();
        Product saved = new Product(id, product.name(), product.price(), product.category());
        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<Product> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Product> findAll() {
        return store.values().stream()
            .sorted(java.util.Comparator.comparingLong(Product::id))
            .toList();
    }

    @Override
    public List<Product> findByCategory(String category) {
        return store.values().stream()
            .filter(p -> category.equalsIgnoreCase(p.category()))
            .sorted(java.util.Comparator.comparingLong(Product::id))
            .toList();
    }

    @Override
    public boolean deleteById(long id) {
        return store.remove(id) != null;
    }

    @Override
    public int count() {
        return store.size();
    }

    /** Resets all data and restarts the id sequence — used between tests. */
    public void clear() {
        store.clear();
        sequence.set(1);
    }
}
