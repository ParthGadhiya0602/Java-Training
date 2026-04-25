package com.javatraining.patterns;

import java.util.*;
import java.util.function.Supplier;

/**
 * Module 25 - Creational Patterns
 *
 * Creational patterns abstract the object-creation process, decoupling
 * callers from the concrete classes they need.
 *
 * Patterns covered:
 *   Singleton      - one instance per JVM; thread-safe via init-on-demand holder
 *   Factory Method - subclass decides which object to create
 *   Abstract Factory - family of related objects without specifying concrete classes
 *   Builder        - step-by-step construction of complex objects
 *   Prototype      - clone an existing object as the starting point
 *   Object Pool    - reuse expensive-to-create objects
 *
 * Modern Java note:
 *   Records replace many simple value-object builders.
 *   Sealed interfaces replace many type-hierarchy factories.
 */
public class CreationalPatterns {

    // ── Singleton (init-on-demand holder) ────────────────────────────────────

    /**
     * Thread-safe lazy singleton without synchronisation overhead.
     * The inner class is not loaded until getInstance() is first called,
     * at which point the JVM's class-loading guarantee provides atomicity.
     */
    public static class AppConfig {
        private final Map<String, String> settings = new HashMap<>();

        private AppConfig() {
            settings.put("timeout", "30");
            settings.put("retries", "3");
        }

        private static class Holder {
            static final AppConfig INSTANCE = new AppConfig();
        }

        public static AppConfig getInstance() { return Holder.INSTANCE; }

        public String get(String key)              { return settings.get(key); }
        public void   set(String key, String value){ settings.put(key, value); }
        public Map<String, String> all()           { return Collections.unmodifiableMap(settings); }
    }

    // ── Factory Method ────────────────────────────────────────────────────────

    /**
     * The creator declares a factory method; subclasses override it to return
     * different concrete products.  The creator's other methods use the product
     * through the abstract interface - they never mention concrete types.
     */
    public interface Notification {
        void send(String recipient, String message);
        String type();
    }

    public static class EmailNotification implements Notification {
        @Override public void   send(String to, String msg) { /* send email */ }
        @Override public String type() { return "EMAIL"; }
    }

    public static class SmsNotification implements Notification {
        @Override public void   send(String to, String msg) { /* send SMS */ }
        @Override public String type() { return "SMS"; }
    }

    public static class PushNotification implements Notification {
        @Override public void   send(String to, String msg) { /* send push */ }
        @Override public String type() { return "PUSH"; }
    }

    /** Factory method: subclasses decide which Notification to create. */
    public abstract static class NotificationService {
        protected abstract Notification createNotification();  // factory method

        public void notify(String recipient, String message) {
            Notification n = createNotification();
            n.send(recipient, message);
        }
    }

    public static class EmailService extends NotificationService {
        @Override protected Notification createNotification() { return new EmailNotification(); }
    }

    public static class SmsService extends NotificationService {
        @Override protected Notification createNotification() { return new SmsNotification(); }
    }

    /**
     * Static factory variant (no subclassing needed):
     * A method that returns a new instance, possibly of a subtype.
     */
    public static Notification notificationFor(String channel) {
        return switch (channel.toUpperCase()) {
            case "EMAIL" -> new EmailNotification();
            case "SMS"   -> new SmsNotification();
            case "PUSH"  -> new PushNotification();
            default      -> throw new IllegalArgumentException("Unknown channel: " + channel);
        };
    }

    // ── Abstract Factory ──────────────────────────────────────────────────────

    /**
     * Abstract Factory creates families of related objects.
     * Here: UI widgets for different themes (light vs dark).
     * Changing the factory changes the entire look & feel consistently.
     */
    public interface Button  { String render(); }
    public interface Checkbox{ String render(); }

    public interface UIFactory {
        Button   createButton();
        Checkbox createCheckbox();
    }

    public static class LightButton   implements Button   { public String render() { return "LightButton"; } }
    public static class LightCheckbox implements Checkbox { public String render() { return "LightCheckbox"; } }
    public static class DarkButton    implements Button   { public String render() { return "DarkButton"; } }
    public static class DarkCheckbox  implements Checkbox { public String render() { return "DarkCheckbox"; } }

    public static class LightThemeFactory implements UIFactory {
        public Button   createButton()   { return new LightButton(); }
        public Checkbox createCheckbox() { return new LightCheckbox(); }
    }

    public static class DarkThemeFactory implements UIFactory {
        public Button   createButton()   { return new DarkButton(); }
        public Checkbox createCheckbox() { return new DarkCheckbox(); }
    }

    /** Application doesn't know which theme it's using - fully decoupled. */
    public static List<String> renderUI(UIFactory factory) {
        return List.of(
            factory.createButton().render(),
            factory.createCheckbox().render()
        );
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    /**
     * Builder separates construction of a complex object from its representation.
     * Solves the "telescoping constructor" anti-pattern.
     *
     * Modern Java note: prefer records for simple value objects.
     * Use Builder when:
     *   - Many optional parameters
     *   - Construction requires multiple steps
     *   - Immutable result is needed
     */
    public static final class HttpRequest {
        private final String  method;
        private final String  url;
        private final Map<String, String> headers;
        private final String  body;
        private final int     timeoutMs;
        private final boolean followRedirects;

        private HttpRequest(Builder b) {
            this.method          = b.method;
            this.url             = b.url;
            this.headers         = Collections.unmodifiableMap(new LinkedHashMap<>(b.headers));
            this.body            = b.body;
            this.timeoutMs       = b.timeoutMs;
            this.followRedirects = b.followRedirects;
        }

        public String              method()          { return method; }
        public String              url()             { return url; }
        public Map<String, String> headers()         { return headers; }
        public String              body()            { return body; }
        public int                 timeoutMs()       { return timeoutMs; }
        public boolean             followRedirects() { return followRedirects; }

        public static Builder newBuilder(String method, String url) {
            return new Builder(method, url);
        }

        public static final class Builder {
            private final String method;
            private final String url;
            private final Map<String, String> headers = new LinkedHashMap<>();
            private String  body            = null;
            private int     timeoutMs       = 5000;
            private boolean followRedirects = true;

            private Builder(String method, String url) {
                this.method = Objects.requireNonNull(method, "method");
                this.url    = Objects.requireNonNull(url, "url");
            }

            public Builder header(String name, String value) {
                headers.put(name, value); return this;
            }
            public Builder body(String body)                  { this.body = body; return this; }
            public Builder timeoutMs(int ms)                  { this.timeoutMs = ms; return this; }
            public Builder followRedirects(boolean follow)    { this.followRedirects = follow; return this; }

            public HttpRequest build() { return new HttpRequest(this); }
        }
    }

    // ── Prototype ─────────────────────────────────────────────────────────────

    /**
     * Prototype creates new objects by copying an existing one.
     * Avoids expensive re-initialisation when the base state is costly to build.
     */
    public interface Prototype<T> {
        T copy();
    }

    public static class DocumentTemplate implements Prototype<DocumentTemplate> {
        private String title;
        private String headerText;
        private String footerText;
        private final List<String> sections;

        public DocumentTemplate(String title, String header, String footer) {
            this.title      = title;
            this.headerText = header;
            this.footerText = footer;
            this.sections   = new ArrayList<>();
        }

        private DocumentTemplate(DocumentTemplate src) {
            this.title      = src.title;
            this.headerText = src.headerText;
            this.footerText = src.footerText;
            this.sections   = new ArrayList<>(src.sections);  // deep copy
        }

        @Override public DocumentTemplate copy() { return new DocumentTemplate(this); }

        public DocumentTemplate withTitle(String t)  { var c = copy(); c.title = t; return c; }
        public DocumentTemplate addSection(String s) { sections.add(s); return this; }

        public String  title()    { return title; }
        public String  header()   { return headerText; }
        public String  footer()   { return footerText; }
        public List<String> sections() { return Collections.unmodifiableList(sections); }
    }

    // ── Object Pool ───────────────────────────────────────────────────────────

    /**
     * Pool manages a set of pre-created, reusable objects.
     * Used for: DB connections, thread pools, ByteBuffer caches.
     * Reduces GC pressure for large objects.
     */
    public static class ObjectPool<T> {
        private final Deque<T>   available = new ArrayDeque<>();
        private final Set<T>     inUse     = new HashSet<>();
        private final Supplier<T> factory;
        private final int         maxSize;

        public ObjectPool(Supplier<T> factory, int maxSize) {
            this.factory = factory;
            this.maxSize = maxSize;
        }

        public synchronized T acquire() {
            if (!available.isEmpty()) {
                T obj = available.poll();
                inUse.add(obj);
                return obj;
            }
            if (inUse.size() < maxSize) {
                T obj = factory.get();
                inUse.add(obj);
                return obj;
            }
            throw new IllegalStateException("Pool exhausted (max=" + maxSize + ")");
        }

        public synchronized void release(T obj) {
            if (inUse.remove(obj)) available.offer(obj);
        }

        public synchronized int availableCount() { return available.size(); }
        public synchronized int inUseCount()     { return inUse.size(); }
        public synchronized int totalSize()      { return available.size() + inUse.size(); }
    }
}
