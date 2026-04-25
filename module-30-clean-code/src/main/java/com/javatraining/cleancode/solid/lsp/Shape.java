package com.javatraining.cleancode.solid.lsp;

/**
 * LSP - Liskov Substitution Principle.
 *
 * <p>Any code that works with a {@code Shape} must work correctly with any
 * subtype of Shape, without knowing the concrete type.
 *
 * <p><strong>Classic LSP violation - Square extends Rectangle:</strong>
 * <pre>
 *   class Rectangle { int width, height; }
 *   class Square extends Rectangle {
 *       void setWidth(int w)  { this.width = w; this.height = w; }  // ← breaks Rectangle contract
 *       void setHeight(int h) { this.width = h; this.height = h; }  // ← width changes unexpectedly
 *   }
 *
 *   // This method works for Rectangle but FAILS silently for Square:
 *   void stretchWidth(Rectangle r) {
 *       r.setWidth(r.getWidth() * 2);
 *       // Expected: only width changed. Square: height also changed. LSP broken.
 *   }
 * </pre>
 *
 * <p><strong>Fix:</strong> don't force Square into Rectangle's hierarchy.
 * Both implement Shape independently. No subtype breaks any other's contract.
 */
public interface Shape {
    double area();
    double perimeter();
}
