package com.javatraining.webflux.repository;

import com.javatraining.webflux.model.Product;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * ReactiveCrudRepository — the reactive equivalent of CrudRepository.
 *
 * All methods return Mono<T> or Flux<T> instead of T or List<T>.
 * The caller subscribes (or lets WebFlux subscribe) — no blocking I/O on the calling thread.
 *
 * Derived query methods follow the same naming conventions as Spring Data JPA:
 *   findByCategory(String) → SELECT * FROM products WHERE category = ?
 *   findByActiveTrue()     → SELECT * FROM products WHERE active = true
 */
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {
    Flux<Product> findByCategory(String category);
    Flux<Product> findByActiveTrue();
}
