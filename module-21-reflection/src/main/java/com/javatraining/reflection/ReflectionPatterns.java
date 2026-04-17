package com.javatraining.reflection;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * Module 21 — Reflection: Practical Patterns
 *
 * Real-world reflection patterns used in frameworks:
 *   - Simple dependency injection container
 *   - Object-to-map / map-to-object mapper
 *   - Simple validator (field constraint checking)
 *   - Plugin loader (load and instantiate by class name)
 *   - toString / equals / hashCode generators
 *
 * These patterns appear in Spring (DI), Jackson (JSON), Hibernate (ORM),
 * and many other Java frameworks.
 */
public class ReflectionPatterns {

    // ── Object <-> Map mapper ─────────────────────────────────────────────────

    /**
     * Converts an object's declared fields to a Map<String, Object>.
     * Null values are included as null entries.
     */
    public static Map<String, Object> toMap(Object obj) throws IllegalAccessException {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);
            result.put(f.getName(), f.get(obj));
        }
        return result;
    }

    /**
     * Sets declared fields of obj from the provided map.
     * Fields not present in the map are left unchanged.
     * Type coercion: if the map value is a String and the field is int/Integer,
     * parseInt is applied automatically.
     */
    public static void fromMap(Object obj, Map<String, Object> values)
            throws IllegalAccessException {
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (!values.containsKey(f.getName())) continue;
            f.setAccessible(true);
            Object val = values.get(f.getName());
            // simple String → int coercion
            if (val instanceof String s && (f.getType() == int.class || f.getType() == Integer.class)) {
                val = Integer.parseInt(s);
            }
            f.set(obj, val);
        }
    }

    // ── Reflection-based toString ─────────────────────────────────────────────

    /**
     * Generates a toString representation using reflection:
     *   ClassName{field1=value1, field2=value2}
     */
    public static String reflectiveToString(Object obj) throws IllegalAccessException {
        StringBuilder sb = new StringBuilder(obj.getClass().getSimpleName()).append("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            if (Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);
            if (i > 0) sb.append(", ");
            sb.append(f.getName()).append("=").append(f.get(obj));
        }
        return sb.append("}").toString();
    }

    // ── Reflection-based equals ───────────────────────────────────────────────

    /**
     * Compares two objects field-by-field using declared fields of a's class.
     * Returns false if they are different types.
     */
    public static boolean reflectiveEquals(Object a, Object b) throws IllegalAccessException {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (!a.getClass().equals(b.getClass())) return false;
        for (Field f : a.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);
            Object va = f.get(a);
            Object vb = f.get(b);
            if (!Objects.equals(va, vb)) return false;
        }
        return true;
    }

    // ── Reflection-based hashCode ─────────────────────────────────────────────

    /**
     * Generates a hashCode from all declared non-static fields.
     */
    public static int reflectiveHashCode(Object obj) throws IllegalAccessException {
        List<Object> values = new ArrayList<>();
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);
            values.add(f.get(obj));
        }
        return Objects.hash(values.toArray());
    }

    // ── Method scanner ────────────────────────────────────────────────────────

    /**
     * Returns all declared methods (including superclass methods, excluding Object)
     * that match the given predicate.
     */
    public static List<Method> findMethods(Class<?> clazz, Predicate<Method> predicate) {
        List<Method> result = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Method m : current.getDeclaredMethods()) {
                if (predicate.test(m)) result.add(m);
            }
            current = current.getSuperclass();
        }
        return result;
    }

    /** Returns all methods whose name starts with "get" and have no parameters. */
    public static List<Method> getGetters(Class<?> clazz) {
        return findMethods(clazz,
            m -> m.getName().startsWith("get") && m.getParameterCount() == 0);
    }

    /** Returns all methods whose name starts with "set" and have exactly one parameter. */
    public static List<Method> getSetters(Class<?> clazz) {
        return findMethods(clazz,
            m -> m.getName().startsWith("set") && m.getParameterCount() == 1);
    }

    // ── Plugin / factory pattern ──────────────────────────────────────────────

    /**
     * Loads a class by name and creates an instance by calling its no-arg constructor.
     * The instance must be assignable to the expected type.
     */
    @SuppressWarnings("unchecked")
    public static <T> T loadPlugin(String className, Class<T> expectedType)
            throws ReflectiveOperationException {
        Class<?> clazz = Class.forName(className);
        if (!expectedType.isAssignableFrom(clazz)) {
            throw new ClassCastException(className + " does not implement " + expectedType.getName());
        }
        Constructor<?> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        return (T) ctor.newInstance();
    }

    // ── Shallow copy ──────────────────────────────────────────────────────────

    /**
     * Creates a new instance of obj's class and copies all declared non-static fields.
     * Requires a no-arg constructor.
     */
    @SuppressWarnings("unchecked")
    public static <T> T shallowCopy(T obj) throws ReflectiveOperationException {
        Class<?> clazz = obj.getClass();
        Constructor<?> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        T copy = (T) ctor.newInstance();
        for (Field f : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);
            f.set(copy, f.get(obj));
        }
        return copy;
    }

    // ── Invoke all methods matching a pattern ─────────────────────────────────

    /**
     * Invokes all no-arg methods whose name matches the prefix on the given object.
     * Returns a map of method name → return value.
     */
    public static Map<String, Object> invokeMatching(Object obj, String methodPrefix)
            throws ReflectiveOperationException {
        Map<String, Object> results = new LinkedHashMap<>();
        for (Method m : obj.getClass().getDeclaredMethods()) {
            if (m.getName().startsWith(methodPrefix) && m.getParameterCount() == 0) {
                m.setAccessible(true);
                results.put(m.getName(), m.invoke(obj));
            }
        }
        return results;
    }
}
