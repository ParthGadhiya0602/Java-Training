package com.javatraining.apidesign.api.v2;

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
@RequestMapping("/v2/products")
@RequiredArgsConstructor
@Tag(name = "Products V2", description = "Extended product catalogue — adds category and stock status")
public class ProductControllerV2 {

    private final ProductRepository productRepository;

    @Operation(summary = "Get product detail by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProductDetail.class))),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetail> getById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(p -> ResponseEntity.ok(new ProductDetail(p.id(), p.name(), p.price(), p.category(), p.inStock())))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List all products with full detail")
    @GetMapping
    public List<ProductDetail> getAll() {
        return productRepository.findAll().stream()
                .map(p -> new ProductDetail(p.id(), p.name(), p.price(), p.category(), p.inStock()))
                .toList();
    }
}
