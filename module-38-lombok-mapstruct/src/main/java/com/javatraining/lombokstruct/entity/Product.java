package com.javatraining.lombokstruct.entity;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * @Value = immutable value object.
 *
 * <p>Equivalent to: all fields {@code private final}, no setters,
 * {@code @Getter}, {@code @ToString}, {@code @EqualsAndHashCode},
 * and a public all-args constructor.
 *
 * <p>Use @Value for:
 * <ul>
 *   <li>DTOs that should never be mutated after creation</li>
 *   <li>Value types where identity IS the data (price, coordinates, money)</li>
 *   <li>Immutable configuration objects</li>
 * </ul>
 *
 * <p>@Builder works with @Value because @Value generates an all-args constructor
 * which @Builder uses to construct instances.
 */
@Value
@Builder
public class Product {
    Long id;
    String name;
    BigDecimal price;
    String category;
}
