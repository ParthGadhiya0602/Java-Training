package com.javatraining.webflux.controller;

import com.javatraining.webflux.dto.ProductRequest;
import com.javatraining.webflux.dto.ProductResponse;
import com.javatraining.webflux.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * WebFlux annotated controller.
 *
 * Handler methods return Mono<T> or Flux<T> instead of T or List<T>.
 * WebFlux subscribes to the returned publisher automatically - no blocking occurs.
 *
 * SSE (Server-Sent Events):
 *   produces = TEXT_EVENT_STREAM_VALUE - WebFlux serializes each Flux element as
 *   "data: <json>\n\n" on the wire. The connection stays open until the Flux completes
 *   or the client disconnects. Useful for real-time dashboards, live feeds, etc.
 *
 *   For infinite streams (e.g. tailing a Kafka topic), the Flux never completes -
 *   the connection stays alive until the client closes it.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Regular JSON list - WebFlux buffers the Flux and returns a JSON array
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ProductResponse> getAll() {
        return productService.findAll();
    }

    // SSE stream - each element is pushed as a separate "data:" event
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ProductResponse> streamAll() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ProductResponse> getById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProductResponse> create(@RequestBody @Valid ProductRequest request) {
        return productService.create(request);
    }
}
