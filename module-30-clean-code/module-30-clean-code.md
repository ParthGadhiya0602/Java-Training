---
title: "Module 30 - Clean Code & Refactoring"
parent: "Phase 3 - Intermediate Engineering"
nav_order: 30
render_with_liquid: false
---

{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-30-clean-code/src){: .btn .btn-outline }

# Module 30 - Clean Code & Refactoring

Clean code is not about style preferences - it is about reducing the cost of change.
This module covers the five SOLID principles with before/after examples, the most
common code smells, and the refactoring moves that fix them.

---

## SOLID - Overview

```
  S  Single Responsibility  - A class should have only one reason to change
  O  Open/Closed            - Open for extension; closed for modification
  L  Liskov Substitution    - Subtypes must be substitutable for their base type
  I  Interface Segregation  - No client should depend on methods it does not use
  D  Dependency Inversion   - Depend on abstractions, not on concretions
```

---

## S - Single Responsibility Principle

One class, one reason to change. A "God class" that validates, persists, notifies,
and invoices has four independent reasons to change - and four teams editing the
same file.

```
  ┌────────────────────────────────────────────────────────────────────────────┐
  │  BEFORE - OrderServiceGod                                                  │
  │                                                                            │
  │  processOrder(order) {                                                     │
  │      validate()       ← reason 1: business rules change                   │
  │      save()           ← reason 2: DB schema changes                       │
  │      sendEmail()      ← reason 3: email template changes                  │
  │      generateInvoice()← reason 4: invoice format changes                  │
  │  }                                                                         │
  │                                                                            │
  │  Problem: any change forces re-testing the entire method.                  │
  └────────────────────────────────────────────────────────────────────────────┘

  ┌────────────────────────────────────────────────────────────────────────────┐
  │  AFTER - each class has exactly one responsibility                         │
  │                                                                            │
  │  OrderValidator      → validate(order)         1 reason to change         │
  │  OrderRepository     → save(order)             1 reason to change         │
  │  NotificationService → buildMessage(order)     1 reason to change         │
  │  InvoiceGenerator    → generate(order)         1 reason to change         │
  │                                                                            │
  │  Benefit: change the email template → touch NotificationService only.     │
  │           change the DB schema      → touch OrderRepository only.         │
  └────────────────────────────────────────────────────────────────────────────┘
```

---

## O - Open/Closed Principle

Software entities should be **open for extension** but **closed for modification**.
New behaviour is added by adding new code, not by editing existing code.

```
  ┌────────────────────────────────────────────────────────────────────────────┐
  │  BEFORE - if/else ladder (OCP violation)                                   │
  │                                                                            │
  │  double calculate(double total, String type) {                             │
  │      if (type.equals("VIP"))      return total * 0.20;                     │
  │      if (type.equals("REGULAR"))  return total * 0.10;                     │
  │      // Adding "SEASONAL" → must edit this method ← OCP broken            │
  │      return 0;                                                             │
  │  }                                                                         │
  └────────────────────────────────────────────────────────────────────────────┘

  ┌────────────────────────────────────────────────────────────────────────────┐
  │  AFTER - polymorphism (OCP compliant)                                      │
  │                                                                            │
  │  interface DiscountPolicy { double calculate(double total); }              │
  │                                                                            │
  │  DiscountPolicies.NONE       → returns 0                                   │
  │  DiscountPolicies.regular()  → 10%                                         │
  │  DiscountPolicies.vip()      → 20%                                         │
  │  DiscountPolicies.seasonal() → flat $N  ← new type, zero edits elsewhere  │
  │                                                                            │
  │  PricingEngine(DiscountPolicy policy) - never needs to change              │
  └────────────────────────────────────────────────────────────────────────────┘
```

---

## L - Liskov Substitution Principle

Any code that works with a base type must work correctly with any of its subtypes,
without knowing the concrete type.

```
  ┌────────────────────────────────────────────────────────────────────────────┐
  │  CLASSIC VIOLATION - Square extends Rectangle                              │
  │                                                                            │
  │  class Rectangle { int width, height; }                                    │
  │  class Square extends Rectangle {                                          │
  │      setWidth(int w)  { width = w; height = w; }  ← breaks contract       │
  │      setHeight(int h) { width = h; height = h; }  ← width changes too!    │
  │  }                                                                         │
  │                                                                            │
  │  void stretchWidth(Rectangle r) {                                          │
  │      r.setWidth(r.getWidth() * 2);                                         │
  │      // Expected: only width changes. Square: height also changes.         │
  │      // → LSP broken; Square is NOT substitutable for Rectangle            │
  │  }                                                                         │
  └────────────────────────────────────────────────────────────────────────────┘

  ┌────────────────────────────────────────────────────────────────────────────┐
  │  FIX - parallel hierarchy via interface                                    │
  │                                                                            │
  │  interface Shape { double area(); double perimeter(); }                    │
  │                                                                            │
  │  record Rectangle(double width, double height) implements Shape { ... }   │
  │  record Square(double side)                    implements Shape { ... }   │
  │                                                                            │
  │  ShapeCalculator.totalArea(List<Shape> shapes)                             │
  │      works with any Shape, no instanceof checks, no surprises             │
  └────────────────────────────────────────────────────────────────────────────┘
```

---

## I - Interface Segregation Principle

Clients should not be forced to implement interfaces they do not use.
A fat interface forces implementing classes to provide stubs for irrelevant methods.

```
  ┌────────────────────────────────────────────────────────────────────────────┐
  │  BEFORE - fat Worker interface (ISP violation)                             │
  │                                                                            │
  │  interface Worker { void work(); void eat(); void charge(); }              │
  │                                                                            │
  │  class Robot implements Worker {                                           │
  │      void eat()    { throw new UnsupportedOperationException(); }  ← stub  │
  │      void charge() { ... }                                                 │
  │      void work()   { ... }                                                 │
  │  }                                                                         │
  └────────────────────────────────────────────────────────────────────────────┘

  ┌────────────────────────────────────────────────────────────────────────────┐
  │  AFTER - segregated interfaces                                             │
  │                                                                            │
  │  interface Workable     { String work(); }                                 │
  │  interface Feedable     { String eat(String food); }                       │
  │  interface Rechargeable { String charge(int percent); }                    │
  │                                                                            │
  │  HumanWorker  implements Workable, Feedable      ← only what makes sense  │
  │  RobotWorker  implements Workable, Rechargeable  ← no forced eat() stub   │
  │                                                                            │
  │  Code that only needs Workable accepts both Human and Robot.               │
  └────────────────────────────────────────────────────────────────────────────┘
```

---

## D - Dependency Inversion Principle

High-level modules should not depend on low-level modules.
Both should depend on abstractions.

```
  ┌────────────────────────────────────────────────────────────────────────────┐
  │  BEFORE - tight coupling (DIP violation)                                   │
  │                                                                            │
  │  class AlertService {                                                      │
  │      private final EmailSender sender = new EmailSender(); ← hard-coded   │
  │      // To switch to SMS: edit AlertService - DIP broken                  │
  │  }                                                                         │
  └────────────────────────────────────────────────────────────────────────────┘

  ┌────────────────────────────────────────────────────────────────────────────┐
  │  AFTER - depend on the abstraction                                         │
  │                                                                            │
  │  interface MessageSender { String send(recipient, message); }             │
  │                                                                            │
  │  class EmailMessageSender implements MessageSender { ... }  ← low-level   │
  │  class SmsMessageSender   implements MessageSender { ... }  ← low-level   │
  │                                                                            │
  │  class AlertService {                                                      │
  │      AlertService(MessageSender sender) { ... }  ← injected abstraction   │
  │      // Switch email→SMS: change nothing in AlertService                  │
  │  }                                                                         │
  │                                                                            │
  │  Dependency flow:                                                          │
  │    AlertService → MessageSender (interface)                                │
  │    EmailSender  → MessageSender (interface)                                │
  │    Both point AT the abstraction, not at each other.                       │
  └────────────────────────────────────────────────────────────────────────────┘
```

---

## Common Code Smells & Refactoring Moves

```
  ┌──────────────────────────┬────────────────────────────────────────────────┐
  │  Code Smell              │  Refactoring Move                              │
  ├──────────────────────────┼────────────────────────────────────────────────┤
  │  God Class               │  Extract Class - split into focused classes    │
  │  Long Method             │  Extract Method - name the intent              │
  │  Duplicate Code          │  Extract Method / Pull Up Method               │
  │  Primitive Obsession     │  Replace Primitive with Object (value object)  │
  │  Data Clump              │  Extract Class - group always-together fields  │
  │  Magic Number / String   │  Replace Magic Number with Named Constant      │
  │  Feature Envy            │  Move Method - method belongs in another class │
  │  Switch/if-else chain    │  Replace Conditional with Polymorphism         │
  │  Refused Bequest         │  Replace Inheritance with Delegation           │
  │  Inappropriate Intimacy  │  Move Field / Extract Class                    │
  └──────────────────────────┴────────────────────────────────────────────────┘
```

### Primitive Obsession → Value Object

```java
// BEFORE - raw String; no validation, no behaviour
void register(String email, String phone) { ... }

// AFTER - always-valid objects with built-in behaviour
record EmailAddress(String value) {
    EmailAddress {
        if (!value.contains("@")) throw new IllegalArgumentException(...);
    }
    String domain()    { return value.substring(value.indexOf('@') + 1); }
    String localPart() { return value.substring(0, value.indexOf('@')); }
}

void register(EmailAddress email, PhoneNumber phone) { ... }
```

### Data Clump → Money Value Object

```java
// BEFORE - amount and currency travel everywhere as separate primitives
void charge(double amount, String currency) { ... }
void refund(double amount, String currency) { ... }
// Easy to pass mismatched values, no precision guarantee

// AFTER - encapsulated, validated, precision-correct
record Money(BigDecimal amount, String currency) {
    Money add(Money other) { ... }       // currency mismatch is a compile error
    Money subtract(Money other) { ... }  // can't go negative accidentally
}
```

### Duplicate Code → Extract Method

```java
// BEFORE - header/footer duplicated in every generator
String generatePdf() {
    String header = "=== Report - " + LocalDate.now() + " ==="; // duplicated
    String footer = "=== END ===";                               // duplicated
    ...
}
String generateCsv() {
    String header = "=== Report - " + LocalDate.now() + " ==="; // same
    ...
}

// AFTER - extracted once, called everywhere
private String buildHeader() { return "=== " + title + " - " + LocalDate.now() + " ==="; }
private String buildFooter(int count) { return "=== END (" + count + " items) ==="; }
```

### Magic Numbers → Named Constants

```java
// BEFORE
if (items.size() > 100) { ... }        // what is 100?
double tax = total * 0.175;            // what is 0.175?

// AFTER
private static final int  MAX_ITEMS_PER_PAGE = 100;
private static final double VAT_RATE         = 0.175;

if (items.size() > MAX_ITEMS_PER_PAGE) { ... }
double tax = total * VAT_RATE;
```

---

## Naming Rules

```
  Classes     → noun or noun phrase    OrderValidator, EmailAddress, Money
  Methods     → verb or verb phrase    validate(), buildHeader(), isValid()
  Booleans    → is/has/can prefix      isValid(), hasItems(), canProcess()
  Constants   → SCREAMING_SNAKE_CASE   MAX_ITEMS_PER_PAGE, VAT_RATE
  Variables   → camelCase, meaningful  customerEmail not em, orderTotal not t

  Avoid:
    Abbreviations          mgr, svc, btn  →  manager, service, button
    Generic names          data, info, obj, temp  →  use the actual concept
    Type in name           customerList  →  customers  (the type is obvious)
    Numbered variables     item1, item2  →  use a collection
```

---

## Method Design Rules

```
  ✓  Do one thing - if you can't summarise a method in one sentence, split it
  ✓  Keep it short - aim for methods visible without scrolling (~20 lines max)
  ✓  One level of abstraction per method
  ✓  No side effects - a method named get* should never mutate state
  ✓  Command-Query Separation - a method either does something or returns something
  ✗  No flag arguments - boolean param means the method does two things
     bad:  render(boolean isMobile)
     good: renderMobile() / renderDesktop()
```

---

## Module 30 - What Was Built

```
  module-30-clean-code/
  ├── pom.xml
  └── src/
      ├── main/java/com/javatraining/cleancode/
      │   ├── solid/srp/
      │   │   ├── OrderServiceGod.java     ← SRP violation (before)
      │   │   ├── Order.java
      │   │   ├── OrderValidator.java      ← 1 responsibility: validate
      │   │   ├── OrderRepository.java     ← 1 responsibility: persist
      │   │   ├── NotificationService.java ← 1 responsibility: notify
      │   │   └── InvoiceGenerator.java    ← 1 responsibility: invoice
      │   ├── solid/ocp/
      │   │   ├── DiscountPolicy.java      ← abstraction (interface)
      │   │   ├── DiscountPolicies.java    ← concrete policies (extensible)
      │   │   └── PricingEngine.java       ← closed for modification
      │   ├── solid/lsp/
      │   │   ├── Shape.java               ← substitutable abstraction
      │   │   ├── Rectangle.java
      │   │   ├── Square.java
      │   │   └── ShapeCalculator.java
      │   ├── solid/isp/
      │   │   ├── Workable.java / Feedable.java / Rechargeable.java
      │   │   ├── HumanWorker.java
      │   │   └── RobotWorker.java
      │   ├── solid/dip/
      │   │   ├── MessageSender.java       ← abstraction
      │   │   ├── EmailMessageSender.java / SmsMessageSender.java
      │   │   └── AlertService.java        ← depends on abstraction only
      │   └── smells/
      │       ├── EmailAddress.java        ← primitive obsession fix
      │       ├── Money.java               ← data clump + float precision fix
      │       └── ReportBuilder.java       ← duplicate code + magic number fix
      └── test/java/com/javatraining/cleancode/
          ├── SolidPrinciplesTest.java     29 tests (5 @Nested classes)
          └── CodeSmellsTest.java          26 tests (3 @Nested classes)
```

Total: **55 tests**, all passing.
{% endraw %}
