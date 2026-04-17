package com.javatraining.reflection;

import java.lang.reflect.*;
import java.util.*;

/**
 * Module 21 — Reflection: Inspecting Classes
 *
 * java.lang.Class is the entry point to reflection.
 * Every loaded type has exactly one Class object.
 *
 * Ways to obtain a Class object:
 *   ClassName.class           — compile-time literal
 *   object.getClass()         — runtime type of an instance
 *   Class.forName("pkg.Name") — dynamic lookup by fully-qualified name
 *
 * Key reflection types:
 *   Class<T>       — type metadata
 *   Field          — instance/static field
 *   Method         — instance/static method
 *   Constructor<T> — constructor
 *   Parameter      — method/constructor parameter
 *   Modifier       — int bitmask of access flags
 */
public class ClassInspector {

    // ── Class metadata ────────────────────────────────────────────────────────

    public static String getSimpleName(Class<?> clazz) {
        return clazz.getSimpleName();
    }

    public static String getCanonicalName(Class<?> clazz) {
        return clazz.getCanonicalName();
    }

    public static String getPackageName(Class<?> clazz) {
        return clazz.getPackageName();
    }

    public static Class<?> getSuperclass(Class<?> clazz) {
        return clazz.getSuperclass();
    }

    public static List<Class<?>> getInterfaces(Class<?> clazz) {
        return List.of(clazz.getInterfaces());
    }

    /** Returns true if clazz is an interface. */
    public static boolean isInterface(Class<?> clazz) {
        return clazz.isInterface();
    }

    /** Returns true if clazz is an enum. */
    public static boolean isEnum(Class<?> clazz) {
        return clazz.isEnum();
    }

    /** Returns true if clazz is a record. */
    public static boolean isRecord(Class<?> clazz) {
        return clazz.isRecord();
    }

    /** Returns true if clazz is an abstract class or interface. */
    public static boolean isAbstract(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    // ── Field inspection ──────────────────────────────────────────────────────

    /**
     * Returns declared field names (own fields, including private; excludes inherited).
     */
    public static List<String> getDeclaredFieldNames(Class<?> clazz) {
        List<String> names = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) names.add(f.getName());
        return names;
    }

    /**
     * Returns public field names from the class and all superclasses.
     */
    public static List<String> getPublicFieldNames(Class<?> clazz) {
        List<String> names = new ArrayList<>();
        for (Field f : clazz.getFields()) names.add(f.getName());
        return names;
    }

    public static Map<String, Class<?>> getDeclaredFieldTypes(Class<?> clazz) {
        Map<String, Class<?>> result = new LinkedHashMap<>();
        for (Field f : clazz.getDeclaredFields()) result.put(f.getName(), f.getType());
        return result;
    }

    /** Checks if a field is static. */
    public static boolean isStatic(Field field) {
        return Modifier.isStatic(field.getModifiers());
    }

    /** Checks if a field is final. */
    public static boolean isFinal(Field field) {
        return Modifier.isFinal(field.getModifiers());
    }

    // ── Method inspection ─────────────────────────────────────────────────────

    /**
     * Returns declared method names (own methods, including private; excludes inherited).
     */
    public static List<String> getDeclaredMethodNames(Class<?> clazz) {
        List<String> names = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) names.add(m.getName());
        return names;
    }

    public static Map<String, Class<?>> getDeclaredMethodReturnTypes(Class<?> clazz) {
        Map<String, Class<?>> result = new LinkedHashMap<>();
        for (Method m : clazz.getDeclaredMethods()) result.put(m.getName(), m.getReturnType());
        return result;
    }

    /** Returns parameter types for a named method (first match). */
    public static List<Class<?>> getParameterTypes(Class<?> clazz, String methodName)
            throws NoSuchMethodException {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                return List.of(m.getParameterTypes());
            }
        }
        throw new NoSuchMethodException(clazz.getName() + "." + methodName);
    }

    // ── Constructor inspection ────────────────────────────────────────────────

    public static int getConstructorCount(Class<?> clazz) {
        return clazz.getDeclaredConstructors().length;
    }

    /** Returns parameter counts for all declared constructors. */
    public static List<Integer> getConstructorParameterCounts(Class<?> clazz) {
        List<Integer> counts = new ArrayList<>();
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            counts.add(c.getParameterCount());
        }
        return counts;
    }

    // ── Hierarchy ─────────────────────────────────────────────────────────────

    /**
     * Returns all classes and interfaces in the inheritance hierarchy,
     * starting from clazz up to (but not including) Object.
     */
    public static List<Class<?>> getHierarchy(Class<?> clazz) {
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            hierarchy.add(current);
            current = current.getSuperclass();
        }
        return hierarchy;
    }

    /**
     * Returns all interfaces implemented by clazz and its superclasses (deduplicated).
     */
    public static Set<Class<?>> getAllInterfaces(Class<?> clazz) {
        Set<Class<?>> result = new LinkedHashSet<>();
        Class<?> current = clazz;
        while (current != null) {
            for (Class<?> iface : current.getInterfaces()) {
                result.add(iface);
                result.addAll(getAllInterfaces(iface));
            }
            current = current.getSuperclass();
        }
        return result;
    }

    // ── Dynamic class loading ─────────────────────────────────────────────────

    /**
     * Loads a class by fully-qualified name using the context class loader.
     * Returns empty if not found.
     */
    public static Optional<Class<?>> loadClass(String fqn) {
        try {
            return Optional.of(Class.forName(fqn));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    /** Returns the modifier string (public, private, static, final, ...). */
    public static String modifierString(int modifiers) {
        return Modifier.toString(modifiers);
    }
}
