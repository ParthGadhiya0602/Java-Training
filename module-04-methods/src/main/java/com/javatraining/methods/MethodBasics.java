package com.javatraining.methods;

/**
 * TOPIC: Method anatomy, return types, static vs instance, guard clauses.
 */
public class MethodBasics {

    // -------------------------------------------------------------------------
    // STATIC METHODS - no object needed, pure computation on inputs
    // -------------------------------------------------------------------------

    /** Converts Celsius to Fahrenheit. */
    public static double celsiusToFahrenheit(double c) {
        return (c * 9.0 / 5.0) + 32;
    }

    /** Clamps a value between a min and max bound. */
    public static int clamp(int value, int min, int max) {
        if (min > max)
            throw new IllegalArgumentException(
                "min (" + min + ") must be <= max (" + max + ")");
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    // -------------------------------------------------------------------------
    // EARLY RETURN / GUARD CLAUSES
    // Reject invalid inputs at the top; happy path flows naturally at the bottom.
    // -------------------------------------------------------------------------

    /**
     * Divides two integers safely.
     * Guard clauses handle every invalid case before the real logic.
     */
    public static double safeDivide(double numerator, double denominator) {
        if (denominator == 0) return Double.NaN; // guard: undefined
        return numerator / denominator;
    }

    /**
     * Computes the Body Mass Index.
     * Uses early return for every impossible input before the formula.
     */
    public static double bmi(double weightKg, double heightM) {
        if (weightKg <= 0)
            throw new IllegalArgumentException("Weight must be positive, got: " + weightKg);
        if (heightM <= 0)
            throw new IllegalArgumentException("Height must be positive, got: " + heightM);
        if (heightM > 3.0)
            throw new IllegalArgumentException("Height seems unrealistic: " + heightM + " m");
        return weightKg / (heightM * heightM);
    }

    public static String bmiCategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Normal weight";
        if (bmi < 30.0) return "Overweight";
        return "Obese";
    }

    // -------------------------------------------------------------------------
    // INSTANCE METHODS - operate on object's own state via 'this'
    // -------------------------------------------------------------------------
    private final String name;
    private int score;

    public MethodBasics(String name, int initialScore) {
        this.name  = name;
        this.score = initialScore;
    }

    /** Instance method - reads and modifies THIS object's state. */
    public void addPoints(int points) {
        if (points < 0)
            throw new IllegalArgumentException("Points must be non-negative");
        this.score += points;
    }

    public int getScore()  { return score; }
    public String getName() { return name; }

    /**
     * Compares this player's score to another player's.
     * Instance method that works with two objects: 'this' and 'other'.
     */
    public String compareWith(MethodBasics other) {
        if (this.score > other.score)
            return this.name + " leads by " + (this.score - other.score);
        if (this.score < other.score)
            return other.name + " leads by " + (other.score - this.score);
        return "Tied at " + this.score;
    }

    // -------------------------------------------------------------------------
    // void methods with early return - useful for skipping invalid work
    // -------------------------------------------------------------------------
    public static void printIfPositive(int n) {
        if (n <= 0) return;   // early return: nothing to do
        System.out.println("Positive: " + n);
    }

    public static void main(String[] args) {
        System.out.println("=== Static Methods ===");
        System.out.printf("0°C  = %.1f°F%n", celsiusToFahrenheit(0));
        System.out.printf("100°C = %.1f°F%n", celsiusToFahrenheit(100));
        System.out.printf("-40°C = %.1f°F%n", celsiusToFahrenheit(-40)); // same in both scales

        System.out.println("\n=== Clamp ===");
        System.out.println(clamp(50, 0, 100));   // 50 - within range
        System.out.println(clamp(-5, 0, 100));   // 0  - below min
        System.out.println(clamp(150, 0, 100));  // 100 - above max

        System.out.println("\n=== BMI ===");
        double b = bmi(70, 1.75);
        System.out.printf("BMI = %.1f → %s%n", b, bmiCategory(b));

        System.out.println("\n=== Instance Methods ===");
        MethodBasics alice = new MethodBasics("Alice", 100);
        MethodBasics bob   = new MethodBasics("Bob",   85);
        alice.addPoints(20);
        System.out.println(alice.compareWith(bob)); // Alice leads by 35
        bob.addPoints(35);
        System.out.println(alice.compareWith(bob)); // Tied at 120

        System.out.println("\n=== Early Return in void ===");
        printIfPositive(-3);  // prints nothing
        printIfPositive(0);   // prints nothing
        printIfPositive(7);   // Positive: 7
    }
}
