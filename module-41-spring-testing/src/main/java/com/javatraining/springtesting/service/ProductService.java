package com.javatraining.springtesting.service;

import com.javatraining.springtesting.client.PricingClient;
import com.javatraining.springtesting.dto.ProductRequest;
import com.javatraining.springtesting.dto.ProductResponse;
import com.javatraining.springtesting.exception.ProductNotFoundException;
import com.javatraining.springtesting.model.Product;
import com.javatraining.springtesting.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;
    private final PricingClient pricingClient;

    public ProductService(ProductRepository repository, PricingClient pricingClient) {
        this.repository = repository;
        this.pricingClient = pricingClient;
    }

    public List<ProductResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public ProductResponse findById(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .price(request.price())
                .category(request.category())
                .active(true)
                .build();
        return toResponse(repository.save(product));
    }

    // Calls the external pricing service - can be stubbed with WireMock in tests
    public BigDecimal getLivePrice(Long id) {
        findById(id);  // ensure product exists
        return pricingClient.getPrice(id);
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getCategory(), p.isActive());
    }
}
