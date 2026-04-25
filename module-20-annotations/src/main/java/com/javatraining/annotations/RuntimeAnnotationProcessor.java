package com.javatraining.annotations;

import com.javatraining.annotations.AnnotationDefinitions.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * Module 20 - Runtime Annotation Processing via Reflection
 *
 * Annotations with RetentionPolicy.RUNTIME are available at runtime
 * through java.lang.reflect:
 *
 *   Class.getAnnotation(Ann.class)           - single annotation
 *   Class.getAnnotationsByType(Ann.class)    - repeatable annotations (unwrapped)
 *   Class.getDeclaredAnnotations()           - all annotations on the element
 *   Class.isAnnotationPresent(Ann.class)     - boolean presence check
 *
 * Same methods exist on Method, Field, Constructor, Parameter.
 *
 * Typical use-cases: dependency injection, validation frameworks,
 * ORM mapping, test runners (JUnit itself uses annotations).
 */
public class RuntimeAnnotationProcessor {

    // ── Read class-level annotations ──────────────────────────────────────────

    /** Returns the @Author value, or "unknown" if not present. */
    public static String getAuthor(Class<?> clazz) {
        Author a = clazz.getAnnotation(Author.class);
        return a != null ? a.value() : "unknown";
    }

    /** Returns all @Tag values on a class (handles @Repeatable unwrapping). */
    public static List<String> getTags(Class<?> clazz) {
        Tag[] tags = clazz.getAnnotationsByType(Tag.class);
        List<String> result = new ArrayList<>();
        for (Tag t : tags) result.add(t.value());
        return result;
    }

    /** Returns all @Tag values on a method. */
    public static List<String> getMethodTags(Method method) {
        Tag[] tags = method.getAnnotationsByType(Tag.class);
        List<String> result = new ArrayList<>();
        for (Tag t : tags) result.add(t.value());
        return result;
    }

    public static boolean isBeta(Class<?> clazz) {
        return clazz.isAnnotationPresent(Beta.class);
    }

    public static boolean isMethodBeta(Method method) {
        return method.isAnnotationPresent(Beta.class);
    }

    // ── Scan methods ──────────────────────────────────────────────────────────

    /**
     * Returns a map of method name → list of required roles for all methods
     * annotated with @RequiresRoles on the given class.
     */
    public static Map<String, List<String>> getRequiredRoles(Class<?> clazz) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Method m : clazz.getDeclaredMethods()) {
            RequiresRoles rr = m.getAnnotation(RequiresRoles.class);
            if (rr != null) {
                result.put(m.getName(), List.of(rr.value()));
            }
        }
        return result;
    }

    /**
     * Returns the names of all fields annotated with @Inject on the given class.
     */
    public static List<String> getInjectableFields(Class<?> clazz) {
        List<String> result = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(Inject.class)) {
                result.add(f.getName());
            }
        }
        return result;
    }

    // ── Validation via annotations ────────────────────────────────────────────

    /**
     * Simple field validator: looks for @Inject fields and checks that the
     * target object has non-null values for each one.
     * Returns a list of field names that are null (i.e., not injected).
     */
    public static List<String> findNullInjectedFields(Object obj)
            throws IllegalAccessException {
        List<String> nullFields = new ArrayList<>();
        Class<?> clazz = obj.getClass();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(Inject.class)) {
                f.setAccessible(true);
                if (f.get(obj) == null) nullFields.add(f.getName());
            }
        }
        return nullFields;
    }

    // ── Inherited annotation check ────────────────────────────────────────────

    /**
     * Demonstrates @Inherited: if the superclass has @Component and the
     * subclass does not, isAnnotationPresent still returns true on the subclass
     * (because @Component is declared with @Inherited).
     */
    public static boolean hasComponent(Class<?> clazz) {
        return clazz.isAnnotationPresent(Component.class);
    }

    public static String getComponentName(Class<?> clazz) {
        Component c = clazz.getAnnotation(Component.class);
        return c != null ? c.name() : "";
    }

    // ── Full annotation report ────────────────────────────────────────────────

    /**
     * Builds a human-readable report of all runtime annotations on a class
     * and its declared methods.
     */
    public static String describeAnnotations(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        sb.append("Class: ").append(clazz.getSimpleName()).append("\n");

        Annotation[] classAnns = clazz.getDeclaredAnnotations();
        if (classAnns.length > 0) {
            sb.append("  Class annotations:\n");
            for (Annotation a : classAnns) {
                sb.append("    ").append(a.annotationType().getSimpleName()).append("\n");
            }
        }

        Method[] methods = clazz.getDeclaredMethods();
        if (methods.length > 0) {
            sb.append("  Methods:\n");
            for (Method m : methods) {
                Annotation[] mAnns = m.getDeclaredAnnotations();
                if (mAnns.length > 0) {
                    sb.append("    ").append(m.getName()).append(":\n");
                    for (Annotation a : mAnns) {
                        sb.append("      ").append(a.annotationType().getSimpleName()).append("\n");
                    }
                }
            }
        }
        return sb.toString().trim();
    }

    // ── KnownIssue scanner ────────────────────────────────────────────────────

    /**
     * Returns all @KnownIssue descriptions found on the class and its methods,
     * filtering by minimum severity.
     */
    public static List<String> getKnownIssues(Class<?> clazz,
                                               KnownIssue.Severity minSeverity) {
        List<String> issues = new ArrayList<>();

        KnownIssue classIssue = clazz.getAnnotation(KnownIssue.class);
        if (classIssue != null && classIssue.severity().ordinal() >= minSeverity.ordinal()) {
            issues.add("[class] " + classIssue.description());
        }

        for (Method m : clazz.getDeclaredMethods()) {
            KnownIssue mi = m.getAnnotation(KnownIssue.class);
            if (mi != null && mi.severity().ordinal() >= minSeverity.ordinal()) {
                issues.add("[" + m.getName() + "] " + mi.description());
            }
        }
        return issues;
    }
}
