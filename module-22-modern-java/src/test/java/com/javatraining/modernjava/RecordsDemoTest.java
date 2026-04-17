package com.javatraining.modernjava;

import com.javatraining.modernjava.RecordsDemo.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecordsDemo")
class RecordsDemoTest {

    @Nested
    @DisplayName("Point record")
    class PointTests {
        @Test void accessors_return_components() {
            Point p = new Point(3.0, 4.0);
            assertEquals(3.0, p.x());
            assertEquals(4.0, p.y());
        }

        @Test void equals_by_value() {
            assertEquals(new Point(1.0, 2.0), new Point(1.0, 2.0));
        }

        @Test void not_equal_for_different_values() {
            assertNotEquals(new Point(1.0, 2.0), new Point(2.0, 1.0));
        }

        @Test void toString_contains_components() {
            String s = new Point(3.0, 4.0).toString();
            assertTrue(s.contains("3.0"));
            assertTrue(s.contains("4.0"));
        }

        @Test void origin_factory() {
            assertEquals(new Point(0, 0), Point.origin());
        }

        @Test void distance_3_4_5_triangle() {
            Point a = new Point(0, 0);
            Point b = new Point(3, 4);
            assertEquals(5.0, a.distanceTo(b), 1e-9);
        }

        @Test void translate_shifts_point() {
            Point p = new Point(1, 2);
            assertEquals(new Point(4, 6), p.translate(3, 4));
        }
    }

    @Nested
    @DisplayName("Range record (compact constructor)")
    class RangeTests {
        @Test void valid_range() {
            Range r = new Range(1, 10);
            assertEquals(1, r.min());
            assertEquals(10, r.max());
        }

        @Test void compact_constructor_rejects_inverted_range() {
            assertThrows(IllegalArgumentException.class, () -> new Range(10, 1));
        }

        @Test void contains() {
            Range r = new Range(0, 5);
            assertTrue(r.contains(3));
            assertFalse(r.contains(6));
        }

        @Test void size() {
            assertEquals(10, new Range(5, 15).size());
        }
    }

    @Nested
    @DisplayName("Person record (canonical constructor)")
    class PersonTests {
        @Test void strips_whitespace_from_name() {
            Person p = new Person("  Alice  ", 30);
            assertEquals("Alice", p.name());
        }

        @Test void negative_age_rejected() {
            assertThrows(IllegalArgumentException.class, () -> new Person("Bob", -1));
        }

        @Test void greeting() {
            assertEquals("Hi, I'm Alice (25)", new Person("Alice", 25).greeting());
        }
    }

    @Nested
    @DisplayName("Pair record (generic)")
    class PairTests {
        @Test void accessors() {
            Pair<String, Integer> p = new Pair<>("hello", 42);
            assertEquals("hello", p.first());
            assertEquals(42, p.second());
        }

        @Test void swap() {
            Pair<String, Integer> p = new Pair<>("x", 1);
            Pair<Integer, String> swapped = p.swap();
            assertEquals(1, swapped.first());
            assertEquals("x", swapped.second());
        }

        @Test void static_factory_of() {
            Pair<String, String> dup = Pair.of("hi");
            assertEquals("hi", dup.first());
            assertEquals("hi", dup.second());
        }
    }

    @Nested
    @DisplayName("Shape records (interface)")
    class ShapeTests {
        @Test void circle_area() {
            assertEquals(Math.PI * 4, new Circle(2.0).area(), 1e-9);
        }

        @Test void circle_perimeter() {
            assertEquals(2 * Math.PI * 3, new Circle(3.0).perimeter(), 1e-9);
        }

        @Test void circle_rejects_non_positive_radius() {
            assertThrows(IllegalArgumentException.class, () -> new Circle(0));
        }

        @Test void rectangle_area() {
            assertEquals(12.0, new Rectangle(3.0, 4.0).area(), 1e-9);
        }

        @Test void rectangle_perimeter() {
            assertEquals(14.0, new Rectangle(3.0, 4.0).perimeter(), 1e-9);
        }
    }

    @Nested
    @DisplayName("Temperature record")
    class TemperatureTests {
        @Test void celsius_to_fahrenheit() {
            assertEquals(212.0, new Temperature(100).toFahrenheit(), 1e-9);
        }

        @Test void from_fahrenheit() {
            assertEquals(0.0, Temperature.fromFahrenheit(32).celsius(), 1e-9);
        }

        @Test void is_freezing() {
            assertTrue(new Temperature(0).isFreezing());
            assertFalse(new Temperature(1).isFreezing());
        }
    }

    @Nested
    @DisplayName("Utility methods")
    class Utilities {
        @Test void centroid_of_three_points() {
            List<Point> pts = List.of(new Point(0, 0), new Point(3, 0), new Point(0, 3));
            Point c = RecordsDemo.centroid(pts);
            assertEquals(1.0, c.x(), 1e-9);
            assertEquals(1.0, c.y(), 1e-9);
        }

        @Test void centroid_empty_list_throws() {
            assertThrows(IllegalArgumentException.class, () -> RecordsDemo.centroid(List.of()));
        }

        @Test void group_by_decade() {
            List<Person> people = List.of(
                new Person("A", 15), new Person("B", 25), new Person("C", 22));
            Map<Integer, List<Person>> groups = RecordsDemo.groupByDecade(people);
            assertEquals(1, groups.get(10).size());
            assertEquals(2, groups.get(20).size());
        }
    }
}
