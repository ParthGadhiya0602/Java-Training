package com.javatraining.apidesign.product;

import java.math.BigDecimal;

public record Product(Long id, String name, BigDecimal price, String category, boolean inStock) {
}
