package com.javatraining.generics;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TypeErasureTest {

    // ── TypedContainer ────────────────────────────────────────────────────────

    @Nested
    class TypedContainerTests {

        @Test
        void set_and_get_string() {
            TypeErasure.TypedContainer<String> box = new TypeErasure.TypedContainer<>(String.class);
            box.set("hello");
            assertEquals("hello", box.get());
        }

        @Test
        void set_wrong_type_throws() {
            TypeErasure.TypedContainer<String> box = new TypeErasure.TypedContainer<>(String.class);
            assertThrows(ClassCastException.class, () -> box.set(42));
        }

        @Test
        void isInstance_true_for_correct_type() {
            TypeErasure.TypedContainer<Integer> box = new TypeErasure.TypedContainer<>(Integer.class);
            assertTrue(box.isInstance(99));
        }

        @Test
        void isInstance_false_for_wrong_type() {
            TypeErasure.TypedContainer<Integer> box = new TypeErasure.TypedContainer<>(Integer.class);
            assertFalse(box.isInstance("string"));
        }

        @Test
        void type_returns_class_token() {
            TypeErasure.TypedContainer<Double> box = new TypeErasure.TypedContainer<>(Double.class);
            assertEquals(Double.class, box.type());
        }
    }

    // ── erasure / instanceof ──────────────────────────────────────────────────

    @Nested
    class ErasureTests {

        @Test
        void list_of_strings_is_a_list() {
            assertTrue(TypeErasure.isAList(List.of("a", "b")));
        }

        @Test
        void list_of_integers_is_also_a_list() {
            assertTrue(TypeErasure.isAList(List.of(1, 2, 3)));
        }

        @Test
        void non_list_is_not_a_list() {
            assertFalse(TypeErasure.isAList("not a list"));
        }

        @Test
        void erased_class_same_for_different_type_params() {
            // List<String> and List<Integer> have identical class at runtime
            assertEquals(List.of("a").getClass(), List.of(1).getClass());
        }

        @Test
        void describeType_identifies_list() {
            String desc = TypeErasure.describeType(List.of(1, 2, 3));
            assertTrue(desc.startsWith("List of size"));
        }

        @Test
        void describeType_identifies_map() {
            String desc = TypeErasure.describeType(Map.of("k", "v"));
            assertTrue(desc.startsWith("Map of size"));
        }
    }

    // ── SafeCastList ──────────────────────────────────────────────────────────

    @Nested
    class SafeCastListTests {

        @Test
        void add_and_get() {
            TypeErasure.SafeCastList<String> list = new TypeErasure.SafeCastList<>(String.class);
            list.add("hello");
            list.add("world");
            assertEquals("hello", list.get(0));
            assertEquals("world", list.get(1));
        }

        @Test
        void size_tracks_adds() {
            TypeErasure.SafeCastList<Integer> list = new TypeErasure.SafeCastList<>(Integer.class);
            list.add(1); list.add(2); list.add(3);
            assertEquals(3, list.size());
        }

        @Test
        void toList_returns_typed_list() {
            TypeErasure.SafeCastList<Integer> list = new TypeErasure.SafeCastList<>(Integer.class);
            list.add(10); list.add(20);
            List<Integer> typed = list.toList();
            assertEquals(List.of(10, 20), typed);
        }
    }

    // ── TypeRef ───────────────────────────────────────────────────────────────

    @Nested
    class TypeRefTests {

        @Test
        void captures_simple_type() {
            TypeErasure.TypeRef<String> ref = new TypeErasure.TypeRef<>() {};
            assertEquals("java.lang.String", ref.capturedType().getTypeName());
        }

        @Test
        void captures_parameterised_type() {
            TypeErasure.TypeRef<List<Integer>> ref = new TypeErasure.TypeRef<>() {};
            String name = ref.capturedType().getTypeName();
            assertTrue(name.contains("List"));
            assertTrue(name.contains("Integer"));
        }
    }

    // ── reflection ────────────────────────────────────────────────────────────

    @Nested
    class ReflectionTests {

        @Test
        void field_generic_type_preserved() throws Exception {
            String typeName = TypeErasure.getFieldGenericType();
            assertTrue(typeName.contains("List"));
            assertTrue(typeName.contains("String"));
        }
    }
}
