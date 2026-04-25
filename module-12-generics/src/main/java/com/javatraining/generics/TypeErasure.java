package com.javatraining.generics;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

/**
 * TOPIC: Type erasure and its practical consequences
 *
 * Java generics are implemented via ERASURE: the compiler removes all generic
 * type information and inserts casts at use sites.  At runtime, the JVM sees
 * only the erased type.
 *
 * Erasure rules:
 *   List<String>          → List
 *   Pair<Integer,String>  → Pair
 *   <T>                   → Object    (no bound)
 *   <T extends Number>    → Number    (leftmost bound)
 *   <T extends A & B>     → A         (leftmost bound)
 *
 * Consequences:
 *   ILLEGAL:  new T()           - erased; no type token
 *   ILLEGAL:  new T[n]          - array creation requires reifiable type
 *   ILLEGAL:  x instanceof List<String>  - always true or always false after erasure
 *   LEGAL:    x instanceof List<?>      - raw/unbounded wildcard is reifiable
 *   LEGAL:    (T) value                 - compiles with unchecked warning
 *
 * Solutions:
 *   • Pass Class<T> (a type token) to construct or compare at runtime
 *   • Use @SuppressWarnings("unchecked") with a comment when the cast is safe
 *   • Use the supertype-token trick (anonymous subclass of generic class)
 *     to preserve type info in the class's generic supertype signature
 */
public class TypeErasure {

    // -------------------------------------------------------------------------
    // 1. Type token pattern - Class<T>
    //    Passing Class<T> gives you a runtime reified handle to T.
    //    Used to instantiate T via reflection, and to check instanceof.
    // -------------------------------------------------------------------------
    static final class TypedContainer<T> {
        private final Class<T> type;
        private T value;

        TypedContainer(Class<T> type) {
            this.type = type;
        }

        void set(Object raw) {
            // Runtime type check using the class token - safe cast with clear intent
            value = type.cast(raw);      // throws ClassCastException if wrong type
        }

        T get() { return value; }

        /** True if obj is an instance of T. Replaces illegal instanceof T. */
        boolean isInstance(Object obj) {
            return type.isInstance(obj);
        }

        Class<T> type() { return type; }
    }

    // -------------------------------------------------------------------------
    // 2. Factory with type token - creates T instances via Supplier
    //    (Reflection-based new T() is one approach; Supplier is cleaner)
    // -------------------------------------------------------------------------
    static final class Factory<T> {
        private final Supplier<T> supplier;
        private final Class<T>    type;

        Factory(Class<T> type, Supplier<T> supplier) {
            this.type     = type;
            this.supplier = supplier;
        }

        T create()  { return supplier.get(); }
        Class<T> type() { return type; }
    }

    // -------------------------------------------------------------------------
    // 3. Demonstrating erasure with instanceof
    //    At runtime List<String> and List<Integer> are both just List.
    //    'instanceof List<String>' is illegal; 'instanceof List<?>' is fine.
    // -------------------------------------------------------------------------
    static boolean isAList(Object obj) {
        return obj instanceof List<?>;   // OK - List<?> is reifiable
        // return obj instanceof List<String>; // ILLEGAL - won't compile
    }

    static String describeType(Object obj) {
        if (obj instanceof List<?> list) {
            return "List of size " + list.size();
        } else if (obj instanceof Map<?,?> map) {
            return "Map of size " + map.size();
        } else {
            return "Other: " + obj.getClass().getSimpleName();
        }
    }

    // -------------------------------------------------------------------------
    // 4. Heap pollution example - what unchecked warnings protect against
    //    We show the warning-suppressed version WITH a safety note explaining
    //    why the cast is guaranteed safe.
    // -------------------------------------------------------------------------
    static final class SafeCastList<T> {
        private final List<Object> raw = new ArrayList<>();
        private final Class<T>     type;

        SafeCastList(Class<T> type) { this.type = type; }

        void add(T item)  { raw.add(item); }

        @SuppressWarnings("unchecked")
        // Safe: every element was added via add(T), which type-checks at the call site.
        T get(int index)  { return (T) raw.get(index); }

        int  size()       { return raw.size(); }

        /** Copies validated elements into a new typed list using the class token. */
        List<T> toList() {
            List<T> result = new ArrayList<>(raw.size());
            for (Object o : raw) result.add(type.cast(o));
            return result;
        }
    }

    // -------------------------------------------------------------------------
    // 5. Supertype token - preserves generic info in class hierarchy
    //    ParameterizedTypeRef<T> captures the full generic type in the
    //    anonymous subclass's generic supertype - readable via reflection.
    // -------------------------------------------------------------------------
    abstract static class TypeRef<T> {
        private final Type capturedType;

        protected TypeRef() {
            // getGenericSuperclass() returns ParameterizedType for the anonymous subclass
            Type superclass = getClass().getGenericSuperclass();
            capturedType = ((ParameterizedType) superclass).getActualTypeArguments()[0];
        }

        Type capturedType()  { return capturedType; }

        @Override public String toString() { return capturedType.getTypeName(); }
    }

    // -------------------------------------------------------------------------
    // 6. Reflection: generic type info IS preserved in method / field signatures
    // -------------------------------------------------------------------------
    static List<String> exampleField = new ArrayList<>(); // field type preserved in bytecode

    static String getFieldGenericType() throws Exception {
        Field f = TypeErasure.class.getDeclaredField("exampleField");
        return f.getGenericType().getTypeName();   // "java.util.List<java.lang.String>"
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void typeTokenDemo() {
        System.out.println("=== TypedContainer - Class<T> token ===");
        TypedContainer<String> box = new TypedContainer<>(String.class);
        box.set("hello");
        System.out.println("value: " + box.get());
        System.out.println("isInstance(\"x\"): " + box.isInstance("x"));
        System.out.println("isInstance(42):  " + box.isInstance(42));

        try {
            box.set(42);   // wrong type - runtime ClassCastException
        } catch (ClassCastException e) {
            System.out.println("Caught bad set: " + e.getMessage());
        }
    }

    static void erasureDemo() {
        System.out.println("\n=== Erasure: instanceof at runtime ===");
        List<String>  strings  = List.of("a", "b");
        List<Integer> integers = List.of(1, 2, 3);

        // Both are true - same erased type List at runtime
        System.out.println("strings  instanceof List<?>: " + isAList(strings));
        System.out.println("integers instanceof List<?>: " + isAList(integers));

        // getClass() returns the same class
        System.out.println("same class: " + (strings.getClass() == integers.getClass()));

        System.out.println(describeType(strings));
        System.out.println(describeType(Map.of("k", "v")));
        System.out.println(describeType(42));
    }

    static void safeCastListDemo() {
        System.out.println("\n=== SafeCastList - @SuppressWarnings(\"unchecked\") ===");
        SafeCastList<Integer> nums = new SafeCastList<>(Integer.class);
        nums.add(10); nums.add(20); nums.add(30);
        System.out.println("get(1): " + nums.get(1));
        System.out.println("toList: " + nums.toList());
    }

    static void typeRefDemo() {
        System.out.println("\n=== TypeRef - supertype token preserves generic info ===");
        // Anonymous subclass captures List<Map<String,Integer>> in its supertype
        TypeRef<List<Map<String, Integer>>> ref = new TypeRef<>() {};
        System.out.println("captured type: " + ref.capturedType());
    }

    static void reflectionDemo() {
        System.out.println("\n=== Reflection - generic info in signatures ===");
        try {
            System.out.println("exampleField type: " + getFieldGenericType());
        } catch (Exception e) {
            System.out.println("error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        typeTokenDemo();
        erasureDemo();
        safeCastListDemo();
        typeRefDemo();
        reflectionDemo();
    }
}
