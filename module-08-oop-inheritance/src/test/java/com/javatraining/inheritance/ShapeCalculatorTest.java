package com.javatraining.inheritance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShapeCalculatorTest {

    // -----------------------------------------------------------------------
    // Circle
    // -----------------------------------------------------------------------
    @Test
    void circle_area() {
        ShapeCalculator.Circle c = new ShapeCalculator.Circle(5, "red");
        assertEquals(Math.PI * 25, c.area(), 1e-9);
    }

    @Test
    void circle_perimeter() {
        ShapeCalculator.Circle c = new ShapeCalculator.Circle(5, "red");
        assertEquals(Math.PI * 10, c.perimeter(), 1e-9);
    }

    @Test
    void circle_zero_radius_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> new ShapeCalculator.Circle(0, "red"));
    }

    @Test
    void circle_scale_doubles_radius_quadruples_area() {
        ShapeCalculator.Circle c = new ShapeCalculator.Circle(3, "red");
        ShapeCalculator.AbstractShape scaled = c.scaled(2);
        assertEquals(c.area() * 4, scaled.area(), 1e-6);
    }

    // -----------------------------------------------------------------------
    // Rectangle
    // -----------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "4, 6, 24.0, 20.0",
        "5, 5, 25.0, 20.0",
    })
    void rectangle_area_and_perimeter(double w, double h, double area, double perim) {
        ShapeCalculator.Rectangle r = new ShapeCalculator.Rectangle(w, h, "blue");
        assertEquals(area,  r.area(),      1e-9);
        assertEquals(perim, r.perimeter(), 1e-9);
    }

    @Test
    void rectangle_is_square_when_sides_equal() {
        assertTrue(new ShapeCalculator.Rectangle(4, 4, "blue").isSquare());
        assertFalse(new ShapeCalculator.Rectangle(4, 5, "blue").isSquare());
    }

    // -----------------------------------------------------------------------
    // Triangle
    // -----------------------------------------------------------------------
    @Test
    void triangle_345_area_is_6() {
        ShapeCalculator.Triangle t = new ShapeCalculator.Triangle(3, 4, 5, "green");
        assertEquals(6.0, t.area(), 1e-9);
    }

    @Test
    void triangle_perimeter() {
        ShapeCalculator.Triangle t = new ShapeCalculator.Triangle(3, 4, 5, "green");
        assertEquals(12.0, t.perimeter(), 1e-9);
    }

    @Test
    void invalid_triangle_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> new ShapeCalculator.Triangle(1, 1, 10, "x")); // violates triangle inequality
    }

    // -----------------------------------------------------------------------
    // Scale — template method
    // -----------------------------------------------------------------------
    @Test
    void scale_zero_throws() {
        ShapeCalculator.Circle c = new ShapeCalculator.Circle(5, "red");
        assertThrows(IllegalArgumentException.class, () -> c.scaled(0));
    }

    @Test
    void scale_negative_throws() {
        ShapeCalculator.Circle c = new ShapeCalculator.Circle(5, "red");
        assertThrows(IllegalArgumentException.class, () -> c.scaled(-1));
    }

    @Test
    void rectangle_scale_preserves_shape_type() {
        ShapeCalculator.Rectangle r = new ShapeCalculator.Rectangle(3, 4, "blue");
        ShapeCalculator.AbstractShape s = r.scaled(3);
        assertInstanceOf(ShapeCalculator.Rectangle.class, s);
    }

    // -----------------------------------------------------------------------
    // Calculator
    // -----------------------------------------------------------------------
    @Test
    void calculator_total_area_sums_all() {
        List<ShapeCalculator.AbstractShape> shapes = List.of(
            new ShapeCalculator.Circle(1, "r"),       // π
            new ShapeCalculator.Rectangle(2, 3, "b")  // 6
        );
        ShapeCalculator.Calculator calc = new ShapeCalculator.Calculator(shapes);
        assertEquals(Math.PI + 6, calc.totalArea(), 1e-9);
    }

    @Test
    void calculator_largest_by_area() {
        ShapeCalculator.AbstractShape big   = new ShapeCalculator.Circle(10, "r");
        ShapeCalculator.AbstractShape small = new ShapeCalculator.Circle(1,  "b");
        ShapeCalculator.Calculator calc =
            new ShapeCalculator.Calculator(List.of(small, big));
        assertEquals(big.area(), calc.largest().orElseThrow().area(), 1e-9);
    }

    @Test
    void calculator_filter_by_min_area() {
        List<ShapeCalculator.AbstractShape> shapes = List.of(
            new ShapeCalculator.Circle(1,  "r"),   // ~3.14
            new ShapeCalculator.Circle(5,  "b"),   // ~78.5
            new ShapeCalculator.Circle(10, "g")    // ~314
        );
        ShapeCalculator.Calculator calc = new ShapeCalculator.Calculator(shapes);
        List<ShapeCalculator.AbstractShape> filtered = calc.filterByMinArea(50);
        assertEquals(2, filtered.size());
    }

    @Test
    void calculator_sorted_by_area_descending() {
        ShapeCalculator.AbstractShape small  = new ShapeCalculator.Circle(1,  "r");
        ShapeCalculator.AbstractShape medium = new ShapeCalculator.Circle(5,  "b");
        ShapeCalculator.AbstractShape large  = new ShapeCalculator.Circle(10, "g");
        ShapeCalculator.Calculator calc =
            new ShapeCalculator.Calculator(List.of(small, large, medium));
        List<ShapeCalculator.AbstractShape> sorted = calc.sortedByAreaDesc();
        assertEquals(large.area(),  sorted.get(0).area(), 1e-9);
        assertEquals(medium.area(), sorted.get(1).area(), 1e-9);
        assertEquals(small.area(),  sorted.get(2).area(), 1e-9);
    }

    @Test
    void calculator_count_by_type() {
        List<ShapeCalculator.AbstractShape> shapes = List.of(
            new ShapeCalculator.Circle(1,    "r"),
            new ShapeCalculator.Circle(2,    "b"),
            new ShapeCalculator.Rectangle(3, 4, "g")
        );
        ShapeCalculator.Calculator calc = new ShapeCalculator.Calculator(shapes);
        var counts = calc.countByType();
        assertEquals(2L, counts.get("Circle"));
        assertEquals(1L, counts.get("Rectangle"));
    }

    @Test
    void calculator_scaled_all_does_not_modify_originals() {
        ShapeCalculator.Circle original = new ShapeCalculator.Circle(5, "r");
        ShapeCalculator.Calculator calc =
            new ShapeCalculator.Calculator(List.of(original));
        calc.scaledAll(3);
        assertEquals(Math.PI * 25, original.area(), 1e-9); // unchanged
    }
}
