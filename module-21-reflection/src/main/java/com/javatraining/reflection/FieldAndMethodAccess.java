package com.javatraining.reflection;

import java.lang.reflect.*;
import java.util.*;

/**
 * Module 21 - Reflection: Reading and Writing Fields, Invoking Methods
 *
 * setAccessible(true) bypasses access control (public/private/protected).
 * From Java 9+, the module system can restrict this - requires opens in
 * module-info.java or --add-opens JVM flags for non-test code.
 *
 * Key risk: reflection breaks encapsulation and disables compiler checks.
 * Use only when necessary (DI containers, serialization, test utilities).
 *
 * Performance: reflective calls are slower than direct calls due to:
 *   - Security manager checks (if present)
 *   - No JIT inlining at call sites
 * For hot paths, cache Method/Field objects rather than looking them up each call.
 */
public class FieldAndMethodAccess {

    // ── Reading fields ────────────────────────────────────────────────────────

    /**
     * Reads the value of a named field from obj, including private fields.
     */
    public static Object readField(Object obj, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(obj.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    /** Reads a static field value. */
    public static Object readStaticField(Class<?> clazz, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(clazz, fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    // ── Writing fields ────────────────────────────────────────────────────────

    /**
     * Sets the value of a named field on obj, including private fields.
     */
    public static void writeField(Object obj, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(obj.getClass(), fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    /** Sets a static field value. */
    public static void writeStaticField(Class<?> clazz, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(clazz, fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    // ── Invoking methods ──────────────────────────────────────────────────────

    /**
     * Invokes a named method on obj with the given arguments.
     * Searches the declared class and superclasses.
     */
    public static Object invokeMethod(Object obj, String methodName, Object... args)
            throws ReflectiveOperationException {
        Class<?>[] paramTypes = Arrays.stream(args)
            .map(Object::getClass)
            .toArray(Class[]::new);
        Method method = findMethod(obj.getClass(), methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(obj, args);
    }

    /** Invokes a static method. */
    public static Object invokeStaticMethod(Class<?> clazz, String methodName, Object... args)
            throws ReflectiveOperationException {
        Class<?>[] paramTypes = Arrays.stream(args)
            .map(Object::getClass)
            .toArray(Class[]::new);
        Method method = findMethod(clazz, methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    /**
     * Invokes a no-argument method by name.
     * Useful when the method has no parameters to derive types from.
     */
    public static Object invokeNoArgMethod(Object obj, String methodName)
            throws ReflectiveOperationException {
        Method method = findMethodNoArgs(obj.getClass(), methodName);
        method.setAccessible(true);
        return method.invoke(obj);
    }

    // ── Constructor invocation ────────────────────────────────────────────────

    /**
     * Creates an instance using the constructor matching the given argument types.
     */
    public static <T> T newInstance(Class<T> clazz, Object... args)
            throws ReflectiveOperationException {
        Class<?>[] boxedTypes = Arrays.stream(args)
            .map(Object::getClass)
            .toArray(Class[]::new);
        // Try exact (boxed) types first, then scan constructors for a primitive match
        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor(boxedTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (NoSuchMethodException ignored) {}
        // Fall back: find the first constructor whose parameter count matches
        // and whose parameter types are assignable (handles int vs Integer, etc.)
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (c.getParameterCount() != args.length) continue;
            Class<?>[] pts = c.getParameterTypes();
            boolean match = true;
            for (int i = 0; i < pts.length; i++) {
                Class<?> pt = pts[i];
                Class<?> at = args[i].getClass();
                if (!pt.isAssignableFrom(at) && !boxedToPrimitive(at).equals(pt)) {
                    match = false; break;
                }
            }
            if (match) {
                @SuppressWarnings("unchecked") Constructor<T> ctor = (Constructor<T>) c;
                ctor.setAccessible(true);
                return ctor.newInstance(args);
            }
        }
        throw new NoSuchMethodException("No matching constructor found in " + clazz.getName());
    }

    private static Class<?> boxedToPrimitive(Class<?> boxed) {
        if (boxed == Integer.class)   return int.class;
        if (boxed == Long.class)      return long.class;
        if (boxed == Double.class)    return double.class;
        if (boxed == Float.class)     return float.class;
        if (boxed == Boolean.class)   return boolean.class;
        if (boxed == Byte.class)      return byte.class;
        if (boxed == Short.class)     return short.class;
        if (boxed == Character.class) return char.class;
        return boxed;
    }

    /** Creates an instance using the no-argument constructor. */
    public static <T> T newInstanceNoArg(Class<T> clazz)
            throws ReflectiveOperationException {
        Constructor<T> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    // ── Generic type introspection ────────────────────────────────────────────

    /**
     * Returns the actual type argument of a generic field.
     * E.g. for List<String> the result is String.class.
     * Returns Object.class if the type is raw or not parameterised.
     */
    public static Class<?> getGenericFieldType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class<?> c) return c;
        }
        return Object.class;
    }

    /**
     * Returns the actual type argument at position index from a generic superclass.
     * E.g. class StringList extends ArrayList<String>:
     *   getGenericSuperclassTypeArg(StringList.class, 0) → String.class
     */
    public static Class<?> getGenericSuperclassTypeArg(Class<?> clazz, int index) {
        Type superType = clazz.getGenericSuperclass();
        if (superType instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (index < args.length && args[index] instanceof Class<?> c) return c;
        }
        return Object.class;
    }

    // ── Snapshot / copy utilities ─────────────────────────────────────────────

    /**
     * Returns a map of field name → value for all declared fields of obj.
     * Useful for quick state snapshots in tests or debugging.
     */
    public static Map<String, Object> snapshot(Object obj) throws IllegalAccessException {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Field f : obj.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            result.put(f.getName(), f.get(obj));
        }
        return result;
    }

    /**
     * Shallow-copies all declared fields from src to dst (must be same type).
     */
    public static void shallowCopyFields(Object src, Object dst)
            throws IllegalAccessException {
        if (!src.getClass().equals(dst.getClass()))
            throw new IllegalArgumentException("src and dst must be the same class");
        for (Field f : src.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);
            f.set(dst, f.get(src));
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Searches clazz and superclasses for a field by name. */
    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try { return current.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) {}
            current = current.getSuperclass();
        }
        throw new NoSuchFieldException(clazz.getName() + "#" + name);
    }

    /** Searches clazz and superclasses for a method matching name + paramTypes. */
    private static Method findMethod(Class<?> clazz, String name, Class<?>[] paramTypes)
            throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null) {
            try { return current.getDeclaredMethod(name, paramTypes); }
            catch (NoSuchMethodException ignored) {}
            current = current.getSuperclass();
        }
        // also search interfaces
        for (Class<?> iface : clazz.getInterfaces()) {
            try { return iface.getDeclaredMethod(name, paramTypes); }
            catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(clazz.getName() + "." + name);
    }

    /** Searches for a no-argument method by name. */
    private static Method findMethodNoArgs(Class<?> clazz, String name)
            throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null) {
            for (Method m : current.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 0) return m;
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException(clazz.getName() + "." + name + "()");
    }
}
