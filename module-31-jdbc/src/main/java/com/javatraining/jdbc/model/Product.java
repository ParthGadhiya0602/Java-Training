package com.javatraining.jdbc.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Immutable product value object.
 *
 * <p>The {@code id} field is 0 for unsaved (transient) instances and positive
 * after a successful INSERT via {@link com.javatraining.jdbc.repository.ProductRepository}.
 *
 * <p>{@link BigDecimal} is used for {@code price} — never {@code double} —
 * to guarantee exact decimal representation in DECIMAL(10,2) columns.
 */
public record Product(int id, String name, BigDecimal price, int stockQty) {

    /** Convenience constructor for new (unsaved) products. */
    public Product(String name, BigDecimal price, int stockQty) {
        this(0, name, price, stockQty);
    }

    /**
     * Factory that accepts a {@code double} for ergonomic test data creation.
     * Converts via {@link BigDecimal#valueOf(double)} (uses exact string representation)
     * and sets scale to 2 to match the DECIMAL(10,2) column.
     */
    public static Product of(String name, double price, int stockQty) {
        return new Product(name,
                BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP),
                stockQty);
    }
}
