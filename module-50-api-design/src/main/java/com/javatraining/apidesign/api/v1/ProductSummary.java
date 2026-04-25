package com.javatraining.apidesign.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Basic product information")
public record ProductSummary(
        @Schema(description = "Unique product identifier") Long id,
        @Schema(description = "Product name")              String name,
        @Schema(description = "Unit price")                BigDecimal price
) {
}
