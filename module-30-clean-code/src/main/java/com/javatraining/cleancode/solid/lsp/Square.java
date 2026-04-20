package com.javatraining.cleancode.solid.lsp;

public record Square(double side) implements Shape {
    public Square {
        if (side <= 0) throw new IllegalArgumentException("Side must be positive");
    }
    @Override public double area()      { return side * side; }
    @Override public double perimeter() { return 4 * side; }
}
