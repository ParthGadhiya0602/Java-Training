package com.javatraining.reflection;

import com.javatraining.reflection.SampleClasses.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClassInspector")
class ClassInspectorTest {

    @Nested
    @DisplayName("Class metadata")
    class Metadata {
        @Test void simple_name() {
            assertEquals("Person", ClassInspector.getSimpleName(Person.class));
        }

        @Test void canonical_name() {
            assertTrue(ClassInspector.getCanonicalName(Person.class)
                .endsWith("SampleClasses.Person"));
        }

        @Test void package_name() {
            assertEquals("com.javatraining.reflection",
                ClassInspector.getPackageName(Person.class));
        }

        @Test void superclass_of_duck_is_animal() {
            assertEquals(Animal.class, ClassInspector.getSuperclass(Duck.class));
        }

        @Test void superclass_of_object_is_null() {
            assertNull(ClassInspector.getSuperclass(Object.class));
        }

        @Test void interfaces_of_duck() {
            List<Class<?>> ifaces = ClassInspector.getInterfaces(Duck.class);
            assertTrue(ifaces.contains(Flyable.class));
            assertTrue(ifaces.contains(Swimmable.class));
        }

        @Test void interface_detection() {
            assertTrue(ClassInspector.isInterface(Flyable.class));
            assertFalse(ClassInspector.isInterface(Person.class));
        }

        @Test void abstract_detection() {
            assertTrue(ClassInspector.isAbstract(Animal.class));
            assertFalse(ClassInspector.isAbstract(Person.class));
        }

        @Test void record_detection() {
            record Point(int x, int y) {}
            assertTrue(ClassInspector.isRecord(Point.class));
            assertFalse(ClassInspector.isRecord(Person.class));
        }

        @Test void enum_detection() {
            assertFalse(ClassInspector.isEnum(Person.class));
        }
    }

    @Nested
    @DisplayName("Field inspection")
    class Fields {
        @Test void declared_field_names_include_private() {
            List<String> names = ClassInspector.getDeclaredFieldNames(Person.class);
            assertTrue(names.contains("name"));
            assertTrue(names.contains("age"));
        }

        @Test void declared_field_types() {
            var types = ClassInspector.getDeclaredFieldTypes(Person.class);
            assertEquals(String.class, types.get("name"));
            assertEquals(int.class,    types.get("age"));
        }

        @Test void static_field_detection() throws Exception {
            var f = Person.class.getDeclaredField("SPECIES");
            assertTrue(ClassInspector.isStatic(f));
        }

        @Test void final_field_detection() throws Exception {
            var f = Person.class.getDeclaredField("SPECIES");
            assertTrue(ClassInspector.isFinal(f));
        }
    }

    @Nested
    @DisplayName("Method inspection")
    class Methods {
        @Test void declared_method_names_include_private() {
            List<String> names = ClassInspector.getDeclaredMethodNames(Person.class);
            assertTrue(names.contains("getName"));
            assertTrue(names.contains("secret"));
        }

        @Test void parameter_types_for_setName() throws Exception {
            List<Class<?>> params = ClassInspector.getParameterTypes(Person.class, "setName");
            assertEquals(List.of(String.class), params);
        }
    }

    @Nested
    @DisplayName("Constructor inspection")
    class Constructors {
        @Test void person_has_two_constructors() {
            assertEquals(2, ClassInspector.getConstructorCount(Person.class));
        }

        @Test void constructor_param_counts() {
            List<Integer> counts = ClassInspector.getConstructorParameterCounts(Person.class);
            assertTrue(counts.contains(0));
            assertTrue(counts.contains(2));
        }
    }

    @Nested
    @DisplayName("Hierarchy")
    class Hierarchy {
        @Test void duck_hierarchy_contains_duck_and_animal() {
            List<Class<?>> h = ClassInspector.getHierarchy(Duck.class);
            assertTrue(h.contains(Duck.class));
            assertTrue(h.contains(Animal.class));
            assertFalse(h.contains(Object.class));
        }

        @Test void all_interfaces_of_duck() {
            Set<Class<?>> ifaces = ClassInspector.getAllInterfaces(Duck.class);
            assertTrue(ifaces.contains(Flyable.class));
            assertTrue(ifaces.contains(Swimmable.class));
        }
    }

    @Nested
    @DisplayName("Dynamic class loading")
    class Loading {
        @Test void load_existing_class() {
            var opt = ClassInspector.loadClass("java.lang.String");
            assertTrue(opt.isPresent());
            assertEquals(String.class, opt.get());
        }

        @Test void load_missing_class_returns_empty() {
            var opt = ClassInspector.loadClass("com.example.NoSuchClass");
            assertTrue(opt.isEmpty());
        }
    }
}
