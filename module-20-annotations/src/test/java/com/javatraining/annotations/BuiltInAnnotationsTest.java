package com.javatraining.annotations;

import org.junit.jupiter.api.*;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BuiltInAnnotations")
class BuiltInAnnotationsTest {

    @Nested
    @DisplayName("@Override behaviour")
    class OverrideTests {
        @Test void override_method_returns_dog_sound() {
            BuiltInAnnotations.Dog dog = new BuiltInAnnotations.Dog();
            assertEquals("woof", dog.sound());
        }

        @Test void override_toString_returns_dog() {
            assertEquals("Dog", new BuiltInAnnotations.Dog().toString());
        }

        @Test void animal_default_sound() {
            assertEquals("...", new BuiltInAnnotations.Animal().sound());
        }
    }

    @Nested
    @DisplayName("@Deprecated")
    class DeprecatedTests {
        @Test void legacy_method_still_works() {
            assertEquals("value=42", BuiltInAnnotations.legacyFormat(42));
        }

        @Test void modern_method_produces_same_output() {
            assertEquals("value=42", BuiltInAnnotations.modernFormat(42));
        }

        @Test void legacy_method_has_deprecated_annotation() throws Exception {
            Method m = BuiltInAnnotations.class.getDeclaredMethod("legacyFormat", int.class);
            assertTrue(m.isAnnotationPresent(Deprecated.class));
        }

        @Test void deprecated_for_removal_is_true() throws Exception {
            Method m = BuiltInAnnotations.class.getDeclaredMethod("legacyFormat", int.class);
            Deprecated d = m.getAnnotation(Deprecated.class);
            assertTrue(d.forRemoval());
            assertEquals("2.0", d.since());
        }
    }

    @Nested
    @DisplayName("@SuppressWarnings")
    class SuppressWarningsTests {
        @Test void callLegacy_returns_formatted_string() {
            assertEquals("value=7", BuiltInAnnotations.callLegacy(7));
        }

        @Test void castToList_returns_casted_object() {
            List<String> original = List.of("a", "b");
            List<String> result = BuiltInAnnotations.castToList(original);
            assertEquals(original, result);
        }
    }

    @Nested
    @DisplayName("@FunctionalInterface")
    class FunctionalInterfaceTests {
        @Test void transformer_lambda_works() {
            BuiltInAnnotations.Transformer<String, Integer> len = String::length;
            assertEquals(5, len.transform("hello"));
        }

        @Test void transformer_identity_static_method() {
            BuiltInAnnotations.Transformer<String, String> id =
                BuiltInAnnotations.Transformer.identity();
            assertEquals("abc", id.transform("abc"));
        }

        @Test void transformer_is_functional_interface() {
            Class<?> iface = BuiltInAnnotations.Transformer.class;
            assertTrue(iface.isAnnotationPresent(FunctionalInterface.class));
        }
    }

    @Nested
    @DisplayName("@SafeVarargs")
    class SafeVarargsTests {
        @Test void listOf_single_element() {
            List<String> list = BuiltInAnnotations.listOf("hello");
            assertEquals(List.of("hello"), list);
        }

        @Test void listOf_multiple_elements() {
            List<Integer> list = BuiltInAnnotations.listOf(1, 2, 3);
            assertEquals(List.of(1, 2, 3), list);
        }

        @Test void listOf_empty() {
            List<String> list = BuiltInAnnotations.listOf();
            assertTrue(list.isEmpty());
        }
    }

    @Nested
    @DisplayName("LegacyWidget")
    class LegacyWidgetTests {
        @Test void use_legacy_widget_returns_label() {
            assertEquals("test", BuiltInAnnotations.useLegacyWidget("test"));
        }

        @Test void legacy_widget_toString() {
            var w = new BuiltInAnnotations.LegacyWidget("btn");
            assertEquals("LegacyWidget[btn]", w.toString());
        }
    }
}
