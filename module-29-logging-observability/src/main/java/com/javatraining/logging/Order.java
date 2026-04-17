package com.javatraining.logging;

/**
 * Domain model used throughout the logging examples.
 */
public record Order(String id, String item, int quantity) {}
