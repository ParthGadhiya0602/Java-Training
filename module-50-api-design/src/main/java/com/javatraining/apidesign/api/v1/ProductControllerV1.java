package com.javatraining.apidesign.api.v1;

import com.javatraining.apidesign.product.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products V1", description = "Basic product catalogue — id, name, price")
public class ProductControllerV1 {

    private final ProductRepository productRepository;

    @Operation(summary = "Get product by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProductSummary.class))),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductSummary> getById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(p -> ResponseEntity.ok(new ProductSummary(p.id(), p.name(), p.price())))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List all products")
    @GetMapping
    public List<ProductSummary> getAll() {
        return productRepository.findAll().stream()
                .map(p -> new ProductSummary(p.id(), p.name(), p.price()))
                .toList();
    }
}
