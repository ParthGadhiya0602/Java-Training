package com.javatraining.apidesign.product;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ProductRepository {

    private static final Map<Long, Product> PRODUCTS = Map.of(
            1L, new Product(1L, "Widget",       new BigDecimal("9.99"),  "Tools",       true),
            2L, new Product(2L, "Gadget",        new BigDecimal("29.99"), "Electronics", true),
            3L, new Product(3L, "Thingamajig",   new BigDecimal("4.99"),  "Misc",        false)
    );

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(PRODUCTS.get(id));
    }

    public List<Product> findAll() {
        return List.copyOf(PRODUCTS.values());
    }
}
