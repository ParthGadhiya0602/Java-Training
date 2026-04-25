package com.javatraining.springrest.service;

import com.javatraining.springrest.dto.ProductRequest;
import com.javatraining.springrest.dto.ProductResponse;
import com.javatraining.springrest.exception.ProductNotFoundException;
import com.javatraining.springrest.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// In-memory store - ConcurrentHashMap is thread-safe for concurrent reads/writes.
// AtomicLong gives lock-free, monotonically increasing IDs.
@Service
public class ProductService {

    private final Map<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    public List<ProductResponse> findAll() {
        return store.values().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductResponse> findByCategory(String category) {
        return store.values().stream()
                .filter(p -> p.getCategory().equalsIgnoreCase(category))
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse findById(Long id) {
        Product product = store.get(id);
        if (product == null) {
            throw new ProductNotFoundException(id);
        }
        return toResponse(product);
    }

    public ProductResponse create(ProductRequest request) {
        Long id = idSequence.getAndIncrement();
        Product product = Product.builder()
                .id(id)
                .name(request.name())
                .price(request.price())
                .category(request.category())
                .build();
        store.put(id, product);
        return toResponse(product);
    }

    public ProductResponse update(Long id, ProductRequest request) {
        if (!store.containsKey(id)) {
            throw new ProductNotFoundException(id);
        }
        Product product = Product.builder()
                .id(id)
                .name(request.name())
                .price(request.price())
                .category(request.category())
                .build();
        store.put(id, product);
        return toResponse(product);
    }

    public void delete(Long id) {
        if (!store.containsKey(id)) {
            throw new ProductNotFoundException(id);
        }
        store.remove(id);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getCategory());
    }
}
