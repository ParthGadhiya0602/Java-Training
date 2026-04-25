package com.javatraining.oop;

/**
 * TOPIC: Class anatomy - fields, constructors, this, static members,
 *        and the exact initialisation order the JVM follows.
 *
 * Key rules:
 *   • static fields/blocks run once when the class is loaded.
 *   • Instance fields/blocks run every time a constructor is called,
 *     BEFORE the constructor body.
 *   • this(...) must be the very first statement in a constructor -
 *     it delegates to another constructor in the same class.
 */
public class ClassAnatomy {

    // -------------------------------------------------------------------------
    // 1. Counter - static vs instance state
    // -------------------------------------------------------------------------
    static class Counter {

        // class-level: shared by ALL instances
        private static int totalCreated = 0;
        private static final int MAX_VALUE = 1_000;

        // static initialiser block - runs once at class load time
        static {
            System.out.println("[static block] Counter class loaded. MAX=" + MAX_VALUE);
        }

        // instance-level: each object has its own copy
        private final int id;         // set once in constructor, never changes
        private       int value;      // mutable instance state
        private final String label;

        // instance initialiser block - runs before every constructor body
        {
            totalCreated++;
            id = totalCreated;
            System.out.println("[instance block] Counter #" + id + " initialising");
        }

        // canonical constructor
        Counter(String label, int initialValue) {
            // instance block has already run at this point
            if (initialValue < 0 || initialValue > MAX_VALUE) {
                throw new IllegalArgumentException(
                    "Initial value must be 0.." + MAX_VALUE + ", got: " + initialValue);
            }
            this.label = label;
            this.value = initialValue;
        }

        // delegating constructor - calls canonical via this(...)
        Counter(String label) {
            this(label, 0);   // MUST be first statement
        }

        // no-arg convenience
        Counter() {
            this("counter");  // chain again
        }

        void increment()             { value = Math.min(value + 1, MAX_VALUE); }
        void increment(int by)       { value = Math.min(value + by, MAX_VALUE); }
        void reset()                 { value = 0; }
        int  value()                 { return value; }
        int  id()                    { return id; }
        static int totalCreated()    { return totalCreated; }

        // this used to pass the counter itself to another method
        Counter copyWith(int newValue) {
            Counter copy = new Counter(this.label + "-copy", newValue);
            return copy;
        }

        @Override
        public String toString() {
            return "Counter{id=" + id + ", label='" + label + "', value=" + value + "}";
        }
    }

    // -------------------------------------------------------------------------
    // 2. Temperature - demonstrates constructor chaining + unit conversion
    //    and static factory methods (named constructors)
    // -------------------------------------------------------------------------
    static class Temperature {

        private final double celsius; // canonical internal representation

        // private constructor - all creation goes through factories
        private Temperature(double celsius) {
            if (celsius < -273.15) {
                throw new IllegalArgumentException(
                    "Temperature below absolute zero: " + celsius + "°C");
            }
            this.celsius = celsius;
        }

        // static factory methods - named, self-documenting
        static Temperature ofCelsius(double c)    { return new Temperature(c); }
        static Temperature ofFahrenheit(double f) { return new Temperature((f - 32) * 5 / 9); }
        static Temperature ofKelvin(double k)     { return new Temperature(k - 273.15); }
        static Temperature absoluteZero()         { return new Temperature(-273.15); }

        double toCelsius()    { return celsius; }
        double toFahrenheit() { return celsius * 9 / 5 + 32; }
        double toKelvin()     { return celsius + 273.15; }

        boolean isBoiling()   { return celsius >= 100.0; }
        boolean isFreezing()  { return celsius <= 0.0; }

        Temperature add(Temperature other) {
            // adding temperatures in Kelvin (thermodynamic addition)
            return Temperature.ofKelvin(this.toKelvin() + other.toKelvin());
        }

        @Override
        public String toString() {
            return String.format("%.2f°C / %.2f°F / %.2fK",
                celsius, toFahrenheit(), toKelvin());
        }
    }

    // -------------------------------------------------------------------------
    // 3. Demonstrating exact initialisation sequence
    // -------------------------------------------------------------------------
    static class OrderDemo {
        // Step 1 & 2: static field, then static block
        static int staticField = initStatic();

        static int initStatic() {
            System.out.println("  1. static field initialised");
            return 1;
        }

        static {
            System.out.println("  2. static initialiser block");
        }

        // Step 3 & 4: instance field, then instance block
        int instanceField = initInstance();

        int initInstance() {
            System.out.println("  3. instance field initialised");
            return 2;
        }

        {
            System.out.println("  4. instance initialiser block");
        }

        OrderDemo() {
            System.out.println("  5. constructor body");
        }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void counterDemo() {
        System.out.println("\n=== Counter (static vs instance) ===");
        Counter a = new Counter("alpha", 5);
        Counter b = new Counter("beta");   // chains to ("beta", 0)
        Counter c = new Counter();         // chains to ("counter", 0)

        System.out.println("Total counters created: " + Counter.totalCreated());
        System.out.println(a);
        System.out.println(b);
        System.out.println(c);

        a.increment(10);
        b.increment(); b.increment();
        System.out.println("\nAfter increments:");
        System.out.println(a);
        System.out.println(b);

        Counter copy = a.copyWith(99);
        System.out.println("Copy of a: " + copy);
        System.out.println("Total now: " + Counter.totalCreated()); // 4
    }

    static void temperatureDemo() {
        System.out.println("\n=== Temperature (static factories, this) ===");
        Temperature boiling  = Temperature.ofCelsius(100);
        Temperature freezing = Temperature.ofCelsius(0);
        Temperature body     = Temperature.ofFahrenheit(98.6);
        Temperature absolute = Temperature.absoluteZero();

        System.out.println("Boiling:  " + boiling  + " | isBoiling: " + boiling.isBoiling());
        System.out.println("Freezing: " + freezing + " | isFreezing: " + freezing.isFreezing());
        System.out.println("Body:     " + body);
        System.out.println("Abs zero: " + absolute);

        try {
            Temperature.ofCelsius(-300);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }

    static void initialisationOrderDemo() {
        System.out.println("\n=== Initialisation Order ===");
        System.out.println("Creating first OrderDemo:");
        new OrderDemo();
        System.out.println("Creating second OrderDemo (static parts skip):");
        new OrderDemo();
    }

    public static void main(String[] args) {
        counterDemo();
        temperatureDemo();
        initialisationOrderDemo();
    }
}
