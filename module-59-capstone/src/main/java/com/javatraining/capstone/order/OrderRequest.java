package com.javatraining.capstone.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record OrderRequest(
        @NotBlank String productId,
        @Positive int quantity
) {}
