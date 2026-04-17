package com.javatraining.reflection;

import com.javatraining.reflection.SampleClasses.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReflectionPatterns")
class ReflectionPatternsTest {

    @Nested
    @DisplayName("toMap / fromMap")
    class ToFromMap {
        @Test void toMap_captures_all_fields() throws Exception {
            Product p = new Product("SKU-1", 10, 9.99);
            Map<String, Object> map = ReflectionPatterns.toMap(p);
            assertEquals("SKU-1", map.get("sku"));
            assertEquals(10,      map.get("quantity"));
            assertEquals(9.99,    map.get("price"));
        }

        @Test void fromMap_sets_fields() throws Exception {
            Product p = new Product();
            ReflectionPatterns.fromMap(p, Map.of("sku", "SKU-2", "quantity", 5, "price", 3.5));
            assertEquals("SKU-2", p.getSku());
            assertEquals(5,       p.getQuantity());
            assertEquals(3.5,     p.getPrice());
        }

        @Test void fromMap_string_to_int_coercion() throws Exception {
            Product p = new Product();
            ReflectionPatterns.fromMap(p, Map.of("sku", "X", "quantity", "42", "price", 1.0));
            assertEquals(42, p.getQuantity());
        }

        @Test void fromMap_ignores_missing_keys() throws Exception {
            Product p = new Product("original", 1, 1.0);
            ReflectionPatterns.fromMap(p, Map.of("sku", "updated"));
            assertEquals("updated", p.getSku());
            assertEquals(1, p.getQuantity());  // unchanged
        }
    }

    @Nested
    @DisplayName("reflectiveToString")
    class ToString {
        @Test void contains_class_name_and_fields() throws Exception {
            Product p = new Product("ABC", 3, 2.5);
            String s = ReflectionPatterns.reflectiveToString(p);
            assertTrue(s.startsWith("Product{"));
            assertTrue(s.contains("sku=ABC"));
            assertTrue(s.contains("quantity=3"));
        }
    }

    @Nested
    @DisplayName("reflectiveEquals")
    class Equals {
        @Test void equal_objects() throws Exception {
            Product a = new Product("X", 1, 5.0);
            Product b = new Product("X", 1, 5.0);
            assertTrue(ReflectionPatterns.reflectiveEquals(a, b));
        }

        @Test void unequal_objects() throws Exception {
            Product a = new Product("X", 1, 5.0);
            Product b = new Product("Y", 1, 5.0);
            assertFalse(ReflectionPatterns.reflectiveEquals(a, b));
        }

        @Test void different_types() throws Exception {
            assertFalse(ReflectionPatterns.reflectiveEquals(new Product(), new Person()));
        }

        @Test void same_reference() throws Exception {
            Product p = new Product("X", 1, 1.0);
            assertTrue(ReflectionPatterns.reflectiveEquals(p, p));
        }
    }

    @Nested
    @DisplayName("reflectiveHashCode")
    class HashCode {
        @Test void equal_objects_same_hash() throws Exception {
            Product a = new Product("H", 7, 3.0);
            Product b = new Product("H", 7, 3.0);
            assertEquals(
                ReflectionPatterns.reflectiveHashCode(a),
                ReflectionPatterns.reflectiveHashCode(b));
        }
    }

    @Nested
    @DisplayName("Method scanners")
    class MethodScanners {
        @Test void getGetters_finds_all_getters() {
            List<java.lang.reflect.Method> getters = ReflectionPatterns.getGetters(Product.class);
            assertTrue(getters.stream().anyMatch(m -> m.getName().equals("getSku")));
            assertTrue(getters.stream().anyMatch(m -> m.getName().equals("getQuantity")));
            assertTrue(getters.stream().anyMatch(m -> m.getName().equals("getPrice")));
        }

        @Test void getSetters_finds_all_setters() {
            List<java.lang.reflect.Method> setters = ReflectionPatterns.getSetters(Product.class);
            assertTrue(setters.stream().anyMatch(m -> m.getName().equals("setSku")));
            assertTrue(setters.stream().anyMatch(m -> m.getName().equals("setQuantity")));
        }

        @Test void findMethods_with_predicate() {
            var methods = ReflectionPatterns.findMethods(Product.class,
                m -> m.getReturnType() == String.class);
            assertTrue(methods.stream().anyMatch(m -> m.getName().equals("getSku")));
        }
    }

    @Nested
    @DisplayName("Plugin loader")
    class PluginLoader {
        @Test void loads_and_instantiates_by_class_name() throws Exception {
            Greeter g = ReflectionPatterns.loadPlugin(
                EnglishGreeter.class.getName(), Greeter.class);
            assertEquals("Hello, World!", g.greet("World"));
        }

        @Test void throws_when_class_not_found() {
            assertThrows(ReflectiveOperationException.class, () ->
                ReflectionPatterns.loadPlugin("com.example.NoSuch", Greeter.class));
        }
    }

    @Nested
    @DisplayName("shallowCopy")
    class ShallowCopy {
        @Test void copy_has_same_field_values() throws Exception {
            Product orig = new Product("P1", 5, 19.99);
            Product copy = ReflectionPatterns.shallowCopy(orig);
            assertNotSame(orig, copy);
            assertEquals(orig.getSku(),      copy.getSku());
            assertEquals(orig.getQuantity(), copy.getQuantity());
            assertEquals(orig.getPrice(),    copy.getPrice());
        }
    }

    @Nested
    @DisplayName("invokeMatching")
    class InvokeMatching {
        @Test void invokes_all_matching_getters() throws Exception {
            Product p = new Product("Z", 3, 7.0);
            Map<String, Object> results = ReflectionPatterns.invokeMatching(p, "get");
            assertTrue(results.containsKey("getSku"));
            assertEquals("Z", results.get("getSku"));
        }
    }
}
