package com.javatraining.integration;

/**
 * Immutable product value object.
 * {@code id == 0} signals an unsaved product (no identity assigned yet).
 */
public record Product(long id, String name, double price, String category) {}
