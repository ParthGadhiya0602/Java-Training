package com.javatraining.apidesign.api.v2;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Full product information including inventory status")
public record ProductDetail(
        @Schema(description = "Unique product identifier") Long id,
        @Schema(description = "Product name")              String name,
        @Schema(description = "Unit price")                BigDecimal price,
        @Schema(description = "Product category")          String category,
        @Schema(description = "Whether stock is available") boolean inStock
) {
}
