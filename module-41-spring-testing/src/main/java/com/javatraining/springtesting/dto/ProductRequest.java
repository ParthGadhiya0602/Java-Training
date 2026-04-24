package com.javatraining.springtesting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Name is required") String name,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotBlank(message = "Category is required") String category
) {}
