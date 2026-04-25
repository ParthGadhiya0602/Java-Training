---
title: "Module 26 — Unit Testing"
nav_order: 26
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-26-unit-testing/src){: .btn .btn-outline }

# Module 26 — Unit Testing

Unit tests verify individual classes and methods in isolation, catching regressions
before they reach production.  This module covers JUnit 5 (Jupiter) end-to-end and
Mockito for mocking collaborators.

---

## JUnit 5 Architecture

JUnit 5 is made up of three components:

| Component | Role |
|-----------|------|
| **JUnit Platform** | Launcher foundation; integrates with Maven Surefire, Gradle, IDEs |
| **JUnit Vintage**  | Runs JUnit 3/4 tests on the Platform for migration |
| **JUnit Jupiter** | The new programming and extension model (what you write) |

The `junit-jupiter` artifact is an aggregator that pulls in the API, engine,
and parameterized-tests support.

---

## Lifecycle Annotations

```java
@BeforeAll   // runs once before the first test; must be static (per-method lifecycle)
static void suiteSetUp() { ... }

@AfterAll    // runs once after the last test; must be static
static void suiteTearDown() { ... }

@BeforeEach  // runs before every test method — use to reset mutable state
void setUp() { subject = new Calculator(); }

@AfterEach   // runs after every test method
void tearDown() { ... }
```

The default lifecycle is `PER_METHOD`: a new test instance is created for each
test, so `@BeforeEach` has a clean slate to work with every time.

---

## Core Assertions

```java
assertEquals(5,    calculator.add(2, 3));
assertNotNull(result);
assertTrue(calculator.isPrime(7));
assertFalse(calculator.isPrime(4));

// assertAll: all assertions run even if an earlier one fails
assertAll("calculator",
    () -> assertEquals(5,  calculator.add(2, 3),      "add"),
    () -> assertEquals(12, calculator.multiply(3, 4),  "multiply")
);

// assertThrows: verify exception type and inspect the instance
ArithmeticException ex = assertThrows(ArithmeticException.class,
    () -> calculator.divide(10, 0));
assertEquals("Division by zero", ex.getMessage());

// assertTimeout: enforce a wall-clock time budget
assertTimeout(Duration.ofSeconds(1), () -> calculator.factorial(20));
```

---

## @DisplayName

```java
@Test
@DisplayName("2 + 3 = 5")
void addition() { ... }
```

Appears verbatim in IDE and CI reports — use it to describe observable behaviour
rather than repeating the method name.

---

## @Nested — Logical Grouping

```java
@Nested
@DisplayName("Prime number detection")
class PrimeTests {
    @Test void two_is_prime() { assertTrue(calculator.isPrime(2)); }
    @ParameterizedTest @ValueSource(ints = {2, 3, 5, 7}) void known_primes(int n) { ... }
}
```

Nested classes can have their own `@BeforeEach`/`@AfterEach` that stack with the
outer class's lifecycle methods.

---

## Parameterized Tests

```java
// @ValueSource: single argument, one primitive per line
@ParameterizedTest @ValueSource(ints = {2, 3, 5, 7}) void primes(int n) { ... }

// @CsvSource: multiple arguments per row
@ParameterizedTest
@CsvSource({"1, 1, 2", "0, 0, 0", "-1, 1, 0"})
void addition(int a, int b, int expected) { assertEquals(expected, calc.add(a, b)); }

// @MethodSource: any type, including complex objects
static Stream<Arguments> divisionCases() {
    return Stream.of(Arguments.of(10.0, 2.0, 5.0), ...);
}
@ParameterizedTest @MethodSource("divisionCases")
void division(double a, double b, double expected) { ... }

// @EnumSource: test against enum constants
@ParameterizedTest
@EnumSource(value = DayOfWeek.class, names = {"MONDAY", ..., "FRIDAY"})
void weekdays(DayOfWeek day) { assertTrue(day.getValue() <= 5); }

// @NullSource / @EmptySource / @NullAndEmptySource: edge-case null/empty inputs
@ParameterizedTest @NullSource
void null_input(String s) { assertFalse(StringUtils.isPalindrome(s)); }
```

---

## @RepeatedTest

```java
@RepeatedTest(value = 3, name = "run {currentRepetition}/{totalRepetitions}")
void idempotent_operation(RepetitionInfo info) {
    assertEquals(7, calculator.max(7, 3, 5));
}
```

Useful for operations that should produce the same result regardless of how
many times they're called (idempotency, randomness bounds, etc.).

---

## Assumptions

```java
// Aborts (skips) the test if the condition is false — not a failure
assumeTrue(System.getenv("CI") != null, "Only run in CI");

// Runs the assertion only if the condition holds; test always passes otherwise
assumingThat(!onWindows, () -> assertNotNull(System.getenv("HOME")));
```

---

## @Tag and @Disabled

```java
@Test @Tag("slow")           // mvn test -Dgroups=slow to run only tagged tests
void expensive_test() { ... }

@Test @Disabled("WIP")       // shows as skipped, not failed
void future_test() { ... }
```

---

## @TestMethodOrder

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MyTest {
    @Test @Order(1) void first()  { ... }
    @Test @Order(2) void second() { ... }
    // Unordered tests get Integer.MAX_VALUE / 2 as their implicit order
}
```

---

## Mockito — Core Concepts

| Concept | Meaning |
|---------|---------|
| **Mock** | A fully controlled fake; all methods return defaults; void methods do nothing |
| **Stub** | A programmed return value: `when(x.method()).thenReturn(y)` |
| **Spy**  | A partial mock wrapping a real object; real methods run unless individually stubbed |
| **Verify** | Assert that a mock method was (or was not) called |
| **Captor** | Intercept and inspect the actual argument passed to a mock |

---

## @ExtendWith(MockitoExtension.class)

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository    repository;
    @Mock PaymentGateway     paymentGateway;
    @Mock NotificationService notifications;

    @InjectMocks OrderService orderService;   // mocks injected via constructor

    @Captor ArgumentCaptor<Order> orderCaptor;
    @Spy BankAccount account = new BankAccount("acc", 100.0);
}
```

`MockitoExtension` uses **STRICT_STUBS** by default:
- Unused stubbings fail the test (tells you what to remove)
- Argument mismatches in stubbings are reported

---

## Stubbing

```java
// Return value
when(gateway.charge("cust-1", 100.0)).thenReturn(PaymentResult.success("txn-1"));

// Argument matchers
when(gateway.charge(any(), anyDouble())).thenReturn(PaymentResult.failure("Declined"));

// Chain: different value on consecutive calls
when(gateway.charge(any(), anyDouble()))
    .thenReturn(PaymentResult.failure("First fail"))
    .thenReturn(PaymentResult.success("txn-retry"));

// Throw exception
when(gateway.charge(any(), anyDouble())).thenThrow(new RuntimeException("network error"));

// Dynamic answer via lambda
when(repo.findById(anyString()))
    .thenAnswer(inv -> inv.getArgument(0, String.class).startsWith("valid-")
        ? Optional.of(new Order(...))
        : Optional.empty());

// Void method on spy (doReturn / doThrow)
doReturn(9999.0).when(spiedAccount).balance();
doThrow(new RuntimeException()).when(notifications).sendOrderConfirmation(any(), any());
```

---

## Verification

```java
// Called exactly once (default)
verify(repository).save(any(Order.class));

// Times variants
verify(notifications, times(1)).sendOrderConfirmation(any(), any());
verify(repository, never()).save(any());
verify(gateway, atLeastOnce()).charge(any(), anyDouble());

// Nothing touched at all
verifyNoInteractions(paymentGateway, notifications);

// No further interactions beyond what was already verified
verifyNoMoreInteractions(repository);

// Ordering across mocks
InOrder inOrder = inOrder(paymentGateway, repository, notifications);
inOrder.verify(paymentGateway).charge("cust-1", 30.0);
inOrder.verify(repository).save(any());
inOrder.verify(notifications).sendOrderConfirmation(eq("cust-1"), any());
```

---

## ArgumentCaptor

```java
@Captor ArgumentCaptor<Order> orderCaptor;

orderService.placeOrder("cust-1", List.of("book"), 75.0);

verify(repository).save(orderCaptor.capture());
Order saved = orderCaptor.getValue();
assertEquals(OrderStatus.CONFIRMED, saved.status());
```

Use `ArgumentCaptor` when you need to assert on complex object state that was
passed to a collaborator — not just that the method was called.

---

## Argument Matchers

```java
any()              // any non-null object
any(Order.class)   // any non-null Order
anyString()        // any non-null String
anyDouble()        // any double primitive
eq("exact")        // exact equality (needed when mixing with any())
argThat(o -> o.total() > 0)  // custom predicate
```

Rule: when using `any()` matchers in a stubbing or verify, **all** arguments must
use matchers — you cannot mix literals and matchers.

---

## @Spy — Partial Mocking

```java
@Spy BankAccount account = new BankAccount("acc", 200.0);

// Real methods run
account.deposit(50.0);
verify(account).deposit(50.0);
assertEquals(250.0, account.balance());   // real state

// Override a specific method
doReturn(9999.0).when(account).balance();
assertEquals(9999.0, account.balance());  // stubbed
```

Use spy for legacy code or when testing that one real method delegates correctly
to another without stubbing the entire class.

---

## Test Design Principles

### AAA — Arrange / Act / Assert

```java
@Test
void deposit_increases_balance_by_exact_amount() {
    // Arrange
    double depositAmount = 200.0;

    // Act
    account.deposit(depositAmount);

    // Assert
    assertEquals(INITIAL_BALANCE + depositAmount, account.balance());
}
```

### F.I.R.S.T.

| Letter | Meaning |
|--------|---------|
| **F**ast | Tests run in milliseconds; slow tests are skipped |
| **I**solated | Each test sets up its own state; no shared mutable fields |
| **R**epeatable | Same result every run, on every machine |
| **S**elf-validating | Pass/fail — no human inspection needed |
| **T**imely | Written alongside (or before) the code |

### Name tests as sentences

```java
void withdraw_entire_balance_results_in_zero()
void failed_withdrawal_leaves_state_unchanged()
void transaction_list_is_unmodifiable()
```

### Test one concept per test

Small, focused tests are easier to diagnose.  If a test fails, you know exactly
which behaviour broke.

### State integrity after failure

If an operation throws, the object's state must not change:

```java
assertThrows(InsufficientFundsException.class, () -> account.withdraw(overdraft));
assertEquals(INITIAL_BALANCE, account.balance(), "balance unchanged");
assertTrue(account.transactions().isEmpty(), "no partial transaction");
```

### Named constants instead of magic numbers

```java
static final double INITIAL_BALANCE = 500.0;
assertEquals(INITIAL_BALANCE + depositAmount, account.balance());
// vs: assertEquals(700.0, account.balance())  — cryptic and brittle
```
