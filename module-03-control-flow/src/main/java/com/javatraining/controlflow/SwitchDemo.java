package com.javatraining.controlflow;

/**
 * TOPIC: switch — from the old fall-through statement to modern switch
 * expressions and Java 21 pattern matching.
 *
 * Covers:
 * - Traditional switch statement (fall-through, break)
 * - Switch expression with arrow syntax (Java 14+)
 * - yield for multi-statement cases (Java 13+)
 * - Multiple labels per case
 * - Switch on String and enum
 * - Pattern matching in switch (Java 21)
 * - Exhaustive switch with sealed types
 */
public class SwitchDemo {

    // -------------------------------------------------------------------------
    // 1. Traditional switch statement — requires break, has fall-through
    // -------------------------------------------------------------------------
    static void traditionalSwitch(int quarter) {
        System.out.print("Quarter " + quarter + " months: ");
        switch (quarter) {
            case 1:
                System.out.print("Jan ");
                // fall-through to case 2 intentionally here? No — forgot break!
            case 2:
                System.out.print("Feb ");
            case 3:
                System.out.print("Mar ");
                break;
            case 4:
                System.out.println("Oct Nov Dec");
                break;
            default:
                System.out.println("Invalid quarter");
        }
        // quarter=1 → prints "Jan Feb Mar" because of fall-through!
        // That is probably a bug. Always use break unless fall-through is intentional.
    }

    // -------------------------------------------------------------------------
    // 2. Intentional fall-through — grouping cases
    // -------------------------------------------------------------------------
    static String seasonOf(int month) {
        String season;
        switch (month) {
            case 12:
            case 1:
            case 2:
                season = "Winter";  // fall-through is intentional here: same result
                break;
            case 3:
            case 4:
            case 5:
                season = "Spring";
                break;
            case 6:
            case 7:
            case 8:
                season = "Summer";
                break;
            case 9:
            case 10:
            case 11:
                season = "Autumn";
                break;
            default:
                throw new IllegalArgumentException("Invalid month: " + month);
        }
        return season;
    }

    // -------------------------------------------------------------------------
    // 3. Switch expression (Java 14+) — no fall-through, produces a value
    // -------------------------------------------------------------------------
    static String seasonOfModern(int month) {
        // Much cleaner — no break, no fall-through, multiple labels per case
        return switch (month) {
            case 12, 1, 2  -> "Winter";
            case 3,  4, 5  -> "Spring";
            case 6,  7, 8  -> "Summer";
            case 9, 10, 11 -> "Autumn";
            default -> throw new IllegalArgumentException("Invalid month: " + month);
        };
    }

    // -------------------------------------------------------------------------
    // 4. yield — multi-statement case in a switch expression
    // -------------------------------------------------------------------------
    static int daysInMonth(int month, int year) {
        return switch (month) {
            case 1, 3, 5, 7, 8, 10, 12 -> 31;
            case 4, 6, 9, 11           -> 30;
            case 2 -> {
                // Leap year needs logic — use a block with yield
                boolean leap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
                yield leap ? 29 : 28;
                // yield is the switch-expression equivalent of return
                // it produces the value for this case
            }
            default -> throw new IllegalArgumentException(
                "Month must be 1–12, got: " + month);
        };
    }

    // -------------------------------------------------------------------------
    // 5. Switch on String — case-sensitive matching
    // -------------------------------------------------------------------------
    static String httpStatus(String code) {
        return switch (code) {
            case "200" -> "OK";
            case "201" -> "Created";
            case "400" -> "Bad Request";
            case "401" -> "Unauthorized";
            case "403" -> "Forbidden";
            case "404" -> "Not Found";
            case "500" -> "Internal Server Error";
            default    -> "Unknown status: " + code;
        };
        // String switch is case-sensitive: "get" != "GET"
        // The switch checks using .equals(), NOT ==
    }

    // -------------------------------------------------------------------------
    // 6. Switch on enum
    // -------------------------------------------------------------------------
    enum Direction { NORTH, SOUTH, EAST, WEST }

    static String describeDirection(Direction dir) {
        return switch (dir) {
            case NORTH -> "Moving up";
            case SOUTH -> "Moving down";
            case EAST  -> "Moving right";
            case WEST  -> "Moving left";
            // No default needed — all enum constants are covered.
            // If you add a new constant to Direction and forget to add a case here,
            // the compiler (or switch exhaustiveness checker) will warn you.
        };
    }

    // -------------------------------------------------------------------------
    // 7. Pattern matching in switch (Java 21)
    //    — match by TYPE, not just by value
    // -------------------------------------------------------------------------
    static String describe(Object obj) {
        return switch (obj) {
            case Integer i when i < 0    -> "Negative integer: " + i;
            case Integer i when i == 0   -> "Zero";
            case Integer i               -> "Positive integer: " + i;
            case Double  d               -> String.format("Double: %.2f", d);
            case String  s when s.isEmpty() -> "Empty string";
            case String  s               -> "String of length " + s.length() + ": \"" + s + "\"";
            case int[]   arr             -> "int array of length " + arr.length;
            case null                    -> "null reference";
            default                      -> "Unknown type: " + obj.getClass().getSimpleName();
        };
        // 'when' adds a guard condition to a pattern case
        // More specific patterns must come before less specific ones
    }

    // -------------------------------------------------------------------------
    // 8. Exhaustive switch with sealed classes
    //    — compiler verifies ALL cases are covered, no default needed
    // -------------------------------------------------------------------------
    sealed interface Notification permits EmailNotification, SmsNotification, PushNotification {}
    record EmailNotification(String to, String subject) implements Notification {}
    record SmsNotification(String phone, String message) implements Notification {}
    record PushNotification(String deviceId, String title) implements Notification {}

    static String formatNotification(Notification n) {
        return switch (n) {
            case EmailNotification e ->
                String.format("Email to %s: [%s]", e.to(), e.subject());
            case SmsNotification s ->
                String.format("SMS to %s: %s", s.phone(), s.message());
            case PushNotification p ->
                String.format("Push to device %s: %s", p.deviceId(), p.title());
            // No default needed — sealed means compiler knows all subtypes.
            // Adding a 4th subtype to the sealed interface will cause a compile error here,
            // forcing you to handle the new case. This is exhaustiveness checking.
        };
    }

    public static void main(String[] args) {
        System.out.println("=== Traditional switch (fall-through bug) ===");
        traditionalSwitch(1);  // prints "Jan Feb Mar" due to missing break
        System.out.println();

        System.out.println("\n=== Season (old vs modern) ===");
        for (int m : new int[]{1, 4, 7, 10}) {
            System.out.printf("Month %2d → old: %-6s  modern: %s%n",
                m, seasonOf(m), seasonOfModern(m));
        }

        System.out.println("\n=== Days in Month (with yield) ===");
        System.out.println("Feb 2024 (leap): " + daysInMonth(2, 2024));
        System.out.println("Feb 2023:        " + daysInMonth(2, 2023));
        System.out.println("April 2024:      " + daysInMonth(4, 2024));

        System.out.println("\n=== HTTP Status Codes ===");
        for (String code : new String[]{"200", "404", "500", "999"}) {
            System.out.println(code + " → " + httpStatus(code));
        }

        System.out.println("\n=== Enum Switch ===");
        for (Direction d : Direction.values()) {
            System.out.println(d + " → " + describeDirection(d));
        }

        System.out.println("\n=== Pattern Matching in Switch (Java 21) ===");
        Object[] testValues = {42, -7, 0, 3.14, "hello", "", new int[]{1, 2, 3}, null};
        for (Object v : testValues) {
            System.out.println(describe(v));
        }

        System.out.println("\n=== Sealed Class Exhaustive Switch ===");
        Notification[] notifications = {
            new EmailNotification("user@example.com", "Welcome!"),
            new SmsNotification("+91-9876543210", "OTP: 4821"),
            new PushNotification("device-abc-123", "New message")
        };
        for (Notification n : notifications) {
            System.out.println(formatNotification(n));
        }
    }
}
