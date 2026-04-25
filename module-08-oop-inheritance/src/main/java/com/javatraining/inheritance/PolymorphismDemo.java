package com.javatraining.inheritance;

import java.util.*;

/**
 * TOPIC: Polymorphism, casting, instanceof pattern matching, and LSP
 *
 * Polymorphism means "many forms" - one reference type, many runtime behaviours.
 * The reference type controls WHAT you can call at compile time.
 * The object type controls WHICH implementation runs at runtime.
 *
 * Casting rules:
 *   Widening  - subtype → supertype, always safe, implicit
 *   Narrowing - supertype → subtype, may throw ClassCastException, must be explicit
 *
 * Liskov Substitution Principle (LSP):
 *   If S is a subtype of T, then objects of type T may be replaced with S
 *   without altering the correctness of the program.
 *   In practice: a subclass must HONOUR the contract of its parent.
 */
public class PolymorphismDemo {

    // -------------------------------------------------------------------------
    // 1. Animal hierarchy - classic polymorphism demo
    // -------------------------------------------------------------------------
    static abstract class Animal {
        private final String name;
        private final int    age;

        Animal(String name, int age) {
            this.name = name;
            this.age  = age;
        }

        String name() { return name; }
        int    age()  { return age;  }

        // Every animal makes a sound - each subtype decides what it is
        abstract String sound();

        // Template method: uses the abstract hook
        void greet() {
            System.out.printf("  %s (%s, age %d) says: %s%n",
                name, getClass().getSimpleName(), age, sound());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + name + ", " + age + "}";
        }
    }

    static class Dog extends Animal {
        private final String breed;

        Dog(String name, int age, String breed) {
            super(name, age);
            this.breed = breed;
        }

        @Override public String sound() { return "Woof!"; }

        // Subtype-specific method - only accessible via Dog reference
        void fetch(String item) {
            System.out.println("  " + name() + " fetches the " + item + "!");
        }

        String breed() { return breed; }
    }

    static class Cat extends Animal {
        private final boolean isIndoor;

        Cat(String name, int age, boolean isIndoor) {
            super(name, age);
            this.isIndoor = isIndoor;
        }

        @Override public String sound() { return "Meow~"; }

        void purr() { System.out.println("  " + name() + " purrs..."); }

        boolean isIndoor() { return isIndoor; }
    }

    static class Parrot extends Animal {
        private final String phrase;

        Parrot(String name, int age, String phrase) {
            super(name, age);
            this.phrase = phrase;
        }

        @Override public String sound() { return "\"" + phrase + "\""; }

        void mimic(String text) {
            System.out.println("  " + name() + " mimics: " + text);
        }
    }

    // -------------------------------------------------------------------------
    // 2. LSP violation vs correct design
    //    The classic Rectangle/Square LSP problem - we show both the WRONG
    //    approach and why it breaks, then the correct sealed-type solution.
    // -------------------------------------------------------------------------

    // WRONG approach - Square inherits Rectangle and overrides setters
    // This violates LSP: code that works with Rectangle breaks with Square.
    static class Rectangle {
        protected int width;
        protected int height;

        Rectangle(int width, int height) {
            this.width  = width;
            this.height = height;
        }

        void setWidth(int w)  { this.width  = w; }
        void setHeight(int h) { this.height = h; }
        int  area()           { return width * height; }

        @Override public String toString() {
            return "Rectangle(" + width + "×" + height + ")";
        }
    }

    static class SquareLSPViolation extends Rectangle {
        SquareLSPViolation(int side) { super(side, side); }

        // Keeps width == height - but breaks Rectangle's implicit contract
        @Override public void setWidth(int w)  { super.setWidth(w);  super.setHeight(w); }
        @Override public void setHeight(int h) { super.setWidth(h);  super.setHeight(h); }
    }

    /** Consumer that expects Rectangle to behave as Rectangle. */
    static void resizeAndAssert(Rectangle r) {
        r.setWidth(5);
        r.setHeight(3);
        int expected = 15;
        int actual   = r.area();
        System.out.printf("  %s → area=%d (expected %d) LSP %s%n",
            r, actual, expected, actual == expected ? "OK" : "VIOLATED!");
    }

    // -------------------------------------------------------------------------
    // 3. Casting + instanceof pattern matching (Java 16+)
    // -------------------------------------------------------------------------
    static void processAnimal(Animal a) {
        // Old-style (pre-16): if (a instanceof Dog) { Dog d = (Dog) a; ... }
        // Pattern matching (Java 16+): bind + cast in one expression

        if (a instanceof Dog dog) {
            // 'dog' is already typed as Dog - no explicit cast
            dog.fetch("stick");
            System.out.println("  breed: " + dog.breed());

        } else if (a instanceof Cat cat && cat.isIndoor()) {
            // Guard condition: instanceof + condition combined
            cat.purr();

        } else if (a instanceof Parrot parrot) {
            parrot.mimic("Hello, World!");

        } else {
            System.out.println("  " + a.name() + " has no special trick.");
        }
    }

    // -------------------------------------------------------------------------
    // 4. Polymorphic collection processing
    // -------------------------------------------------------------------------
    static void processAll(List<Animal> animals) {
        System.out.println("\n--- Greetings ---");
        for (Animal a : animals) {
            a.greet();  // runtime dispatch - right sound() for each type
        }

        System.out.println("\n--- Special tricks ---");
        for (Animal a : animals) {
            processAnimal(a);
        }

        System.out.println("\n--- Type statistics ---");
        long dogs    = animals.stream().filter(a -> a instanceof Dog).count();
        long cats    = animals.stream().filter(a -> a instanceof Cat).count();
        long parrots = animals.stream().filter(a -> a instanceof Parrot).count();
        System.out.printf("  Dogs: %d  Cats: %d  Parrots: %d%n", dogs, cats, parrots);
    }

    // -------------------------------------------------------------------------
    // 5. Widening and narrowing casts
    // -------------------------------------------------------------------------
    static void castingDemo() {
        System.out.println("\n=== Casting ===");

        Dog rex = new Dog("Rex", 3, "German Shepherd");

        // Widening - safe, implicit
        Animal a = rex;
        System.out.println("Widened to Animal: " + a);

        // Narrowing - explicit, risky
        if (a instanceof Dog dog) {
            System.out.println("Pattern-matched back to Dog: breed=" + dog.breed());
        }

        // Classic cast (still valid, just more verbose)
        Animal a2 = new Cat("Whiskers", 2, true);
        try {
            Dog wrongCast = (Dog) a2;  // will throw ClassCastException
        } catch (ClassCastException e) {
            System.out.println("ClassCastException caught: " + e.getMessage());
        }

        // Safe check before cast (old style - still common in legacy code)
        if (a2 instanceof Cat c) {
            System.out.println("Safe pattern match to Cat: indoor=" + c.isIndoor());
        }
    }

    static void lspDemo() {
        System.out.println("\n=== Liskov Substitution Principle ===");
        System.out.println("Testing with Rectangle:");
        resizeAndAssert(new Rectangle(10, 10));

        System.out.println("Testing with Square (LSP violation):");
        resizeAndAssert(new SquareLSPViolation(10));
        // Square.setWidth(5) also sets height to 5, so area=25, not 15
    }

    public static void main(String[] args) {
        System.out.println("=== Polymorphism Demo ===");

        List<Animal> animals = List.of(
            new Dog("Rex",      3, "German Shepherd"),
            new Cat("Nala",     5, true),
            new Parrot("Polly", 2, "Polly wants a cracker"),
            new Cat("Tiger",    1, false),
            new Dog("Buddy",    7, "Labrador")
        );

        processAll(animals);
        castingDemo();
        lspDemo();
    }
}
