package com.javatraining.springsecurity.service;

import com.javatraining.springsecurity.dto.ProductRequest;
import com.javatraining.springsecurity.exception.ProductNotFoundException;
import com.javatraining.springsecurity.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductService {

    private final Map<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    public List<Product> findAll() {
        return List.copyOf(store.values());
    }

    public Product findById(Long id) {
        Product p = store.get(id);
        if (p == null) throw new ProductNotFoundException(id);
        return p;
    }

    public Product create(ProductRequest request) {
        Long id = idSequence.getAndIncrement();
        Product product = Product.builder()
                .id(id)
                .name(request.name())
                .price(request.price())
                .category(request.category())
                .build();
        store.put(id, product);
        return product;
    }

    public void delete(Long id) {
        if (!store.containsKey(id)) throw new ProductNotFoundException(id);
        store.remove(id);
    }
}
