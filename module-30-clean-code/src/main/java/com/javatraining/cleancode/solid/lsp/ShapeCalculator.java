package com.javatraining.cleancode.solid.lsp;

import java.util.List;

/**
 * Works with any {@link Shape} - LSP guarantee: Rectangle and Square
 * are substitutable here without any special-casing.
 */
public class ShapeCalculator {

    public double totalArea(List<Shape> shapes) {
        return shapes.stream().mapToDouble(Shape::area).sum();
    }

    public double totalPerimeter(List<Shape> shapes) {
        return shapes.stream().mapToDouble(Shape::perimeter).sum();
    }
}
