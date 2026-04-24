package com.javatraining.webflux.service;

import com.javatraining.webflux.dto.ProductRequest;
import com.javatraining.webflux.dto.ProductResponse;
import com.javatraining.webflux.exception.ProductNotFoundException;
import com.javatraining.webflux.model.Product;
import com.javatraining.webflux.repository.ProductRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service layer with reactive return types.
 *
 * Rules:
 *   - NEVER call .block() — that would pin a Netty thread and defeats the purpose of WebFlux
 *   - Compose operators (map, flatMap, switchIfEmpty) instead of imperative if/else
 *   - switchIfEmpty(Mono.error(...)) is the reactive pattern for "throw if not found"
 *   - flatMap is needed when the inner function itself returns Mono/Flux
 *     (use map when the function returns a plain value)
 */
@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Flux<ProductResponse> findAll() {
        return repository.findAll().map(this::toResponse);
    }

    public Mono<ProductResponse> findById(Long id) {
        return repository.findById(id)
                // switchIfEmpty: if the upstream completes empty, subscribe to the fallback Mono
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)))
                .map(this::toResponse);
    }

    public Mono<ProductResponse> create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .category(request.category())
                .price(request.price())
                .build();
        // save() returns Mono<Product> — flatMap would be needed if we had inner reactive calls
        return repository.save(product).map(this::toResponse);
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getCategory(), p.isActive());
    }
}
