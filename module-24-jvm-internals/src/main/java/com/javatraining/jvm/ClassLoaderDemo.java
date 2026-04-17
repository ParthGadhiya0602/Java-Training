package com.javatraining.jvm;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Module 24 — ClassLoaders
 *
 * The JVM loads classes lazily and on demand via a hierarchy of ClassLoaders.
 *
 * ClassLoader hierarchy (Java 9+ module system):
 *   Bootstrap ClassLoader   — loads rt.jar / JDK core classes (java.lang, java.util …)
 *                             implemented in native code; getParent() returns null
 *   Platform ClassLoader    — loads java.* modules not in bootstrap (formerly ext)
 *   Application ClassLoader — loads classpath entries (your code + dependencies)
 *
 * Delegation model (parent-first):
 *   1. Ask parent to load the class
 *   2. If parent returns null (not found), try this loader's own search path
 *   3. If still not found, throw ClassNotFoundException
 *
 * This prevents user code from replacing java.lang.String with a malicious copy.
 *
 * Key ClassLoader API:
 *   ClassLoader.getSystemClassLoader()   — application class loader
 *   clazz.getClassLoader()               — loader that defined the class
 *   Thread.currentThread().getContextClassLoader()  — context loader (for frameworks)
 *   loader.loadClass("pkg.Name")         — trigger loading (with parent delegation)
 *   loader.findClass("pkg.Name")         — override point for custom loaders
 *
 * Custom ClassLoader use-cases:
 *   - Plugin / hot-reload systems
 *   - Loading classes from network or database
 *   - Bytecode transformation (instrumentation agents)
 *   - Isolation: two plugins loading different versions of a library
 */
public class ClassLoaderDemo {

    // ── ClassLoader hierarchy inspection ─────────────────────────────────────

    /** Returns the name of the ClassLoader that loaded the given class. */
    public static String loaderName(Class<?> clazz) {
        ClassLoader cl = clazz.getClassLoader();
        return cl == null ? "bootstrap" : cl.getClass().getSimpleName();
    }

    /**
     * Returns the full delegation chain for a class loader as a list of names,
     * starting from the given loader up to the bootstrap loader.
     */
    public static List<String> delegationChain(ClassLoader loader) {
        List<String> chain = new ArrayList<>();
        ClassLoader current = loader;
        while (current != null) {
            chain.add(current.getClass().getSimpleName());
            current = current.getParent();
        }
        chain.add("bootstrap");
        return chain;
    }

    /**
     * Returns true if cls1 and cls2 were loaded by the same ClassLoader instance.
     * Two classes loaded by different loaders are NEVER equal even if same name.
     */
    public static boolean sameLoader(Class<?> cls1, Class<?> cls2) {
        return cls1.getClassLoader() == cls2.getClassLoader();
    }

    // ── System class loader ───────────────────────────────────────────────────

    public static ClassLoader systemLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    public static ClassLoader platformLoader() {
        return ClassLoader.getPlatformClassLoader();
    }

    public static ClassLoader contextLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    // ── loadClass vs Class.forName ────────────────────────────────────────────

    /**
     * loadClass(name) — does NOT run static initialisers (initialize=false).
     * Class.forName(name) — runs static initialisers immediately.
     * Class.forName(name, false, loader) — load without initialising.
     */
    public static Optional<Class<?>> tryLoadClass(String fqn) {
        try {
            return Optional.of(Class.forName(fqn));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    public static Optional<Class<?>> tryLoadWithLoader(String fqn, ClassLoader loader) {
        try {
            return Optional.of(loader.loadClass(fqn));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    // ── Custom ClassLoader (loads from byte array) ────────────────────────────

    /**
     * A ClassLoader that can define a class directly from a byte array.
     * Demonstrates defineClass() — the primitive operation all loaders use.
     * Useful for: bytecode generation (ASM), instrumentation, testing.
     */
    public static class ByteArrayClassLoader extends ClassLoader {

        public ByteArrayClassLoader(ClassLoader parent) {
            super(parent);
        }

        /**
         * Defines and returns a Class from raw bytecode.
         * After this call, the class is linked and ready to use.
         */
        public Class<?> define(String binaryName, byte[] bytecode) {
            return defineClass(binaryName, bytecode, 0, bytecode.length);
        }
    }

    // ── URLClassLoader ────────────────────────────────────────────────────────

    /**
     * Creates a URLClassLoader that searches the given directory for class files.
     * The parent is the system class loader so standard library is still available.
     */
    public static URLClassLoader urlLoader(Path directory) throws IOException {
        URL url = directory.toUri().toURL();
        return new URLClassLoader(new URL[]{url}, ClassLoader.getSystemClassLoader());
    }

    // ── Class identity check ──────────────────────────────────────────────────

    /**
     * Demonstrates that the same class name loaded by two different loaders
     * produces two incompatible Class objects.
     *
     * Returns a map with:
     *   "sameClass"   — whether both Class objects are the same reference
     *   "castable"    — whether an instance from loader1 can be cast to loader2's type
     */
    public static Map<String, Boolean> classIdentityDemo(ClassLoader loader1,
                                                           ClassLoader loader2,
                                                           String className)
            throws ReflectiveOperationException {
        Class<?> c1 = loader1.loadClass(className);
        Class<?> c2 = loader2.loadClass(className);

        boolean sameClass   = c1 == c2;
        boolean castable    = c1.isAssignableFrom(c2);

        return Map.of("sameClass", sameClass, "castable", castable);
    }

    // ── Resource loading ──────────────────────────────────────────────────────

    /**
     * Loads a classpath resource as a String using the context class loader.
     * getResourceAsStream returns null if not found; returns empty Optional.
     */
    public static Optional<String> loadResource(String resourcePath) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream is = cl.getResourceAsStream(resourcePath)) {
            if (is == null) return Optional.empty();
            return Optional.of(new String(is.readAllBytes()));
        }
    }

    // ── ClassLoader statistics ────────────────────────────────────────────────

    /**
     * Returns basic metadata about a ClassLoader as a map.
     */
    public static Map<String, Object> describe(ClassLoader loader) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("type",   loader.getClass().getSimpleName());
        info.put("name",   loader.getName() != null ? loader.getName() : "(unnamed)");
        info.put("parent", loader.getParent() != null
                           ? loader.getParent().getClass().getSimpleName()
                           : "bootstrap");
        return info;
    }
}
