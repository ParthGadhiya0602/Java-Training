package com.javatraining.springrest.dto;

import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;

// @Relation drives the HAL "_embedded" key:
//   CollectionModel<EntityModel<ProductResponse>> → {"_embedded": {"products": [...]}}
// Without it, HATEOAS uses the class name → "productResponseList" (ugly).
@Relation(collectionRelation = "products", itemRelation = "product")
public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        String category
) {}
