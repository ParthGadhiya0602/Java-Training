package com.javatraining.webflux.dto;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, BigDecimal price, String category, boolean active) {}
