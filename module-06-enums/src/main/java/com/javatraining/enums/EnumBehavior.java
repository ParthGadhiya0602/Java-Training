package com.javatraining.enums;

/**
 * TOPIC: Enums with behaviour - abstract methods (constant-specific body),
 * interface implementation, and the Strategy pattern via enum.
 */
public class EnumBehavior {

    // -------------------------------------------------------------------------
    // 1. Abstract method - each constant provides its own implementation
    //    This is the most powerful feature of Java enums.
    // -------------------------------------------------------------------------
    enum Operation {
        ADD("+") {
            @Override
            public double apply(double x, double y) { return x + y; }
        },
        SUBTRACT("-") {
            @Override
            public double apply(double x, double y) { return x - y; }
        },
        MULTIPLY("×") {
            @Override
            public double apply(double x, double y) { return x * y; }
        },
        DIVIDE("÷") {
            @Override
            public double apply(double x, double y) {
                if (y == 0) throw new ArithmeticException("Division by zero");
                return x / y;
            }
        },
        MODULO("%") {
            @Override
            public double apply(double x, double y) {
                if (y == 0) throw new ArithmeticException("Modulo by zero");
                return x % y;
            }
        },
        POWER("^") {
            @Override
            public double apply(double x, double y) { return Math.pow(x, y); }
        };

        private final String symbol;

        Operation(String symbol) { this.symbol = symbol; }

        // Every constant MUST override this - compiler enforces it
        public abstract double apply(double x, double y);

        @Override
        public String toString() { return symbol; }
    }

    // -------------------------------------------------------------------------
    // 2. Enum implementing an interface - pluggable behaviour
    // -------------------------------------------------------------------------
    interface Taxable {
        double taxRate();
        default double withTax(double amount) { return amount * (1 + taxRate()); }
    }

    enum ProductCategory implements Taxable {
        FOOD        (0.05),   //  5% GST
        ELECTRONICS (0.18),   // 18% GST
        LUXURY      (0.28),   // 28% GST
        MEDICINE    (0.00),   //  0% GST - exempt
        SERVICES    (0.18);   // 18% GST

        private final double rate;

        ProductCategory(double rate) { this.rate = rate; }

        @Override
        public double taxRate() { return rate; }

        public String displayRate() {
            return String.format("%.0f%%", rate * 100);
        }
    }

    // -------------------------------------------------------------------------
    // 3. Strategy pattern via enum - discount calculation
    //    No class explosion, no factory, no boilerplate.
    // -------------------------------------------------------------------------
    enum DiscountStrategy {
        NONE("No discount") {
            @Override
            public double apply(double price) { return price; }
        },
        SEASONAL("10% seasonal") {
            @Override
            public double apply(double price) { return price * 0.90; }
        },
        STUDENT("20% student") {
            @Override
            public double apply(double price) { return price * 0.80; }
        },
        SENIOR("25% senior citizen") {
            @Override
            public double apply(double price) { return price * 0.75; }
        },
        EMPLOYEE("40% employee") {
            @Override
            public double apply(double price) { return price * 0.60; }
        },
        FLASH_SALE("50% flash sale") {
            @Override
            public double apply(double price) { return price * 0.50; }
        };

        private final String description;

        DiscountStrategy(String description) { this.description = description; }

        public abstract double apply(double price);

        public double savings(double originalPrice) {
            return originalPrice - apply(originalPrice);
        }

        @Override
        public String toString() { return description; }
    }

    // -------------------------------------------------------------------------
    // 4. Combining interface + abstract method - richer behaviour
    // -------------------------------------------------------------------------
    interface Describable {
        String describe();
    }

    enum Season implements Describable {
        SPRING {
            @Override public double avgTempCelsius() { return 22.0; }
            @Override public String describe() { return "Warm and blooming"; }
        },
        SUMMER {
            @Override public double avgTempCelsius() { return 36.0; }
            @Override public String describe() { return "Hot and bright"; }
        },
        AUTUMN {
            @Override public double avgTempCelsius() { return 18.0; }
            @Override public String describe() { return "Cool and colourful"; }
        },
        WINTER {
            @Override public double avgTempCelsius() { return 10.0; }
            @Override public String describe() { return "Cold and bare"; }
        };

        public abstract double avgTempCelsius();

        public double avgTempFahrenheit() {
            return avgTempCelsius() * 9.0 / 5.0 + 32;
        }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void operationDemo() {
        System.out.println("=== Operations ===");
        double x = 10, y = 3;
        for (Operation op : Operation.values()) {
            try {
                System.out.printf("  %5.1f %s %4.1f = %.4f%n", x, op, y, op.apply(x, y));
            } catch (ArithmeticException e) {
                System.out.printf("  %5.1f %s %4.1f = ERROR: %s%n", x, op, y, e.getMessage());
            }
        }
    }

    static void taxDemo() {
        System.out.println("\n=== Product Categories with Tax ===");
        double price = 1000.0;
        System.out.printf("  %-12s  %-6s  %-10s  %-10s%n",
            "Category", "Rate", "Price", "With Tax");
        System.out.println("  " + "─".repeat(50));
        for (ProductCategory cat : ProductCategory.values()) {
            System.out.printf("  %-12s  %-6s  ₹%8.2f  ₹%8.2f%n",
                cat, cat.displayRate(), price, cat.withTax(price));
        }
    }

    static void discountDemo() {
        System.out.println("\n=== Discount Strategies ===");
        double originalPrice = 2500.0;
        System.out.printf("  Original price: ₹%.2f%n%n", originalPrice);
        System.out.printf("  %-25s  %-10s  %-10s%n", "Strategy", "Final", "Savings");
        System.out.println("  " + "─".repeat(50));
        for (DiscountStrategy d : DiscountStrategy.values()) {
            System.out.printf("  %-25s  ₹%8.2f  ₹%8.2f%n",
                d, d.apply(originalPrice), d.savings(originalPrice));
        }
    }

    static void seasonDemo() {
        System.out.println("\n=== Seasons ===");
        for (Season s : Season.values()) {
            System.out.printf("  %-6s | %s | %.0f°C / %.0f°F%n",
                s, s.describe(), s.avgTempCelsius(), s.avgTempFahrenheit());
        }
    }

    public static void main(String[] args) {
        operationDemo();
        taxDemo();
        discountDemo();
        seasonDemo();
    }
}
