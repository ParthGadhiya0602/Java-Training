package com.javatraining.reflection;

import java.util.List;

/**
 * Sample classes used by ClassInspector, FieldAndMethodAccess, and
 * ReflectionPatterns tests.  Kept in one file to avoid clutter.
 */
public class SampleClasses {

    // ── Basic POJO ────────────────────────────────────────────────────────────

    public static class Person {
        private String name;
        private int age;
        static final String SPECIES = "Homo sapiens";

        public Person() {}

        public Person(String name, int age) {
            this.name = name;
            this.age  = age;
        }

        public String getName() { return name; }
        public void   setName(String name) { this.name = name; }
        public int    getAge()  { return age; }
        public void   setAge(int age) { this.age = age; }

        private String secret() { return "hidden"; }
    }

    // ── Inheritance hierarchy ─────────────────────────────────────────────────

    public interface Flyable {
        String fly();
    }

    public interface Swimmable {
        String swim();
    }

    public static abstract class Animal {
        protected String name;
        public Animal(String name) { this.name = name; }
        public abstract String speak();
        public String getName() { return name; }
    }

    public static class Duck extends Animal implements Flyable, Swimmable {
        private int feathers;

        public Duck(String name, int feathers) {
            super(name);
            this.feathers = feathers;
        }

        @Override public String speak()  { return "quack"; }
        @Override public String fly()    { return name + " flies"; }
        @Override public String swim()   { return name + " swims"; }
        public int getFeathers()         { return feathers; }
    }

    // ── Class with private fields and methods ─────────────────────────────────

    public static class SecretBox {
        private String secret = "initial";
        private static int instanceCount = 0;

        public SecretBox() { instanceCount++; }

        private String getSecret()         { return secret; }
        private void   setSecret(String s) { this.secret = s; }
        private static int getInstanceCount() { return instanceCount; }
        public  String publicInfo()        { return "public: " + secret; }
    }

    // ── Generic field ─────────────────────────────────────────────────────────

    public static class Container<T> {
        private List<String> items;
        private T value;

        public Container() {}
        public Container(T value) { this.value = value; }
        public T getValue() { return value; }
    }

    public static class StringContainer extends Container<String> {
        public StringContainer() { super(); }
    }

    // ── Interface for proxy tests ─────────────────────────────────────────────

    public interface Calculator {
        int add(int a, int b);
        int multiply(int a, int b);
        String describe();
    }

    public static class SimpleCalculator implements Calculator {
        private int callCount = 0;

        @Override public int add(int a, int b)      { callCount++; return a + b; }
        @Override public int multiply(int a, int b) { callCount++; return a * b; }
        @Override public String describe()           { return "SimpleCalculator[calls=" + callCount + "]"; }
        public int getCallCount()                    { return callCount; }
    }

    // ── Interface with mutation methods for read-only proxy test ─────────────

    public interface MutableStore {
        void   set(String key, String value);
        String get(String key);
        void   clear();
    }

    public static class MapStore implements MutableStore {
        private final java.util.Map<String, String> map = new java.util.HashMap<>();
        @Override public void   set(String key, String value) { map.put(key, value); }
        @Override public String get(String key)               { return map.get(key); }
        @Override public void   clear()                       { map.clear(); }
    }

    // ── Class for plugin loading (has public no-arg constructor) ─────────────

    public interface Greeter {
        String greet(String name);
    }

    public static class EnglishGreeter implements Greeter {
        public EnglishGreeter() {}
        @Override public String greet(String name) { return "Hello, " + name + "!"; }
    }

    // ── POJO for mapper / copy tests ──────────────────────────────────────────

    public static class Product {
        private String sku;
        private int quantity;
        private double price;

        public Product() {}
        public Product(String sku, int quantity, double price) {
            this.sku = sku; this.quantity = quantity; this.price = price;
        }
        public String getSku()      { return sku; }
        public int    getQuantity() { return quantity; }
        public double getPrice()    { return price; }
        public void   setSku(String sku)           { this.sku = sku; }
        public void   setQuantity(int quantity)    { this.quantity = quantity; }
        public void   setPrice(double price)       { this.price = price; }
    }
}
