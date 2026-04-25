package com.javatraining.build;

/**
 * Pure mathematical utilities.
 * Stateless - all methods are static, no dependencies.
 */
public final class MathUtils {

    private MathUtils() {}

    /** Greatest common divisor (Euclidean algorithm). */
    public static int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) { int t = b; b = a % b; a = t; }
        return a;
    }

    /** Least common multiple. */
    public static int lcm(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return Math.abs(a / gcd(a, b) * b);
    }

    /** Returns {@code true} if {@code n} is a prime number. */
    public static boolean isPrime(int n) {
        if (n < 2) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        for (int i = 3; (long) i * i <= n; i += 2)
            if (n % i == 0) return false;
        return true;
    }

    /** Clamps {@code value} to the range [{@code min}, {@code max}]. */
    public static double clamp(double value, double min, double max) {
        if (min > max) throw new IllegalArgumentException("min must be <= max");
        return Math.max(min, Math.min(max, value));
    }

    /** Returns the number of primes up to and including {@code n} (sieve). */
    public static int countPrimes(int n) {
        if (n < 2) return 0;
        boolean[] composite = new boolean[n + 1];
        for (int i = 2; (long) i * i <= n; i++)
            if (!composite[i])
                for (int j = i * i; j <= n; j += i)
                    composite[j] = true;
        int count = 0;
        for (int i = 2; i <= n; i++) if (!composite[i]) count++;
        return count;
    }
}
