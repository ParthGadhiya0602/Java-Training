---
title: "Module 36 — Spring Core"
nav_order: 36
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-36-spring-core/src){: .btn .btn-outline }

# Module 36 — Spring Core

The foundation that every other Spring module builds on:
**IoC container** — Spring creates and manages object instances;
**Dependency Injection** — Spring wires dependencies automatically;
**Bean lifecycle** — hooks to run code at startup and shutdown;
**Bean scopes** — singleton vs prototype instance policies;
**AOP** — cross-cutting concerns (logging, timing, security) without modifying business code.

---

## IoC Container

```
  Traditional code                      Spring IoC
  ────────────────────────────────────  ─────────────────────────────────────────
  UserService s = new UserService(      ApplicationContext ctx = SpringApplication.run(...)
      new InMemoryUserRepository());    UserService s = ctx.getBean(UserService.class);

  You control object creation.          Spring controls object creation.
  You wire dependencies manually.       Spring wires dependencies automatically.
  Hard to swap implementations.         Swap by changing @Primary or @Qualifier.
  Hard to test (new = tight coupling).  Easy to test: inject a mock instead.
```

The IoC container reads your `@Component`, `@Service`, `@Repository`, `@Controller`, and `@Bean` declarations, instantiates each class, resolves their dependencies, and stores the result in an internal registry called the **ApplicationContext**.

---

## Dependency Injection Styles

### Constructor Injection (preferred)

```java
@Service
public class UserService {

    private final UserRepository userRepository;  // final → immutable

    // @Autowired optional when exactly one constructor (Spring 4.3+)
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

```
  Pros:
    Dependency is explicit and mandatory — cannot construct without it
    Field can be final — immutability guaranteed
    Class is testable without Spring: new UserService(new FakeRepo())
  Cons:
    Verbose for many dependencies (use @RequiredArgsConstructor from Lombok)
```

### Setter Injection

```java
@Service
public class ReportService {

    private NotificationService notificationService;

    @Autowired
    @Qualifier("smsNotificationService")
    public void setNotificationService(NotificationService svc) {
        this.notificationService = svc;
    }
}
```

```
  Use when: dependency is optional, or you need to swap it after construction.
  Avoid for: mandatory dependencies (field stays null until setter is called).
```

### Field Injection (avoid in production code)

```java
@Service
public class SomeService {
    @Autowired                        // NOT recommended
    private UserRepository repo;
}
```

```
  Problems:
    Cannot be final — mutability risk
    Cannot test without Spring (no constructor or setter to inject a mock)
    Hidden dependency — not visible in the constructor signature
```

---

## @Primary and @Qualifier

```
  Problem: Two beans implement the same interface.
  Spring doesn't know which one to inject — ambiguous dependency.

  Solution 1 — @Primary: mark one implementation as the default.
  Solution 2 — @Qualifier: specify the exact bean name at the injection point.
```

```java
@Service @Primary
public class EmailNotificationService implements NotificationService { ... }

@Service
public class SmsNotificationService implements NotificationService { ... }

// Injection — @Primary wins by default:
@Autowired
NotificationService notification;           // → EmailNotificationService

// @Qualifier overrides @Primary:
@Autowired
@Qualifier("smsNotificationService")        // bean name = class name camelCased
NotificationService smsOnly;               // → SmsNotificationService
```

---

## @Configuration and @Bean

```java
// Use @Configuration + @Bean when:
// - The class is from a library (can't add @Component to it)
// - You need to configure the bean before registration
// - You want wiring logic to be explicit and centralized

@Configuration
public class AppConfig {

    @Bean
    public AppProperties appProperties() {
        return new AppProperties("My App", "1.0.0", 100);
    }
}

// AppProperties is now a Spring-managed singleton — @Autowired works anywhere:
@Autowired AppProperties props;
```

```
  @Configuration uses CGLIB proxying:
    appProperties()  ← called from another @Bean method
    → Spring intercepts the call and returns the existing singleton
    → NOT a second new AppProperties() instance
```

---

## Bean Lifecycle

```
  1  Constructor called — Spring instantiates the bean
  2  Dependencies injected — @Autowired fields/setters populated
  3  @PostConstruct — your initialization code runs
  4  Bean in active use — handles application calls
  5  @PreDestroy — cleanup code runs when context shuts down
  6  Object garbage collected
```

```java
@Service
public class AuditService {

    @PostConstruct
    void init() {
        // Runs after all dependencies are injected.
        // Safe to call injected beans here.
        // Use for: opening connections, loading config, warming caches
    }

    @PreDestroy
    void shutdown() {
        // Runs when ApplicationContext.close() is called.
        // Use for: closing connections, flushing buffers, releasing resources
    }
}
```

```
  Note: Spring does NOT call @PreDestroy on prototype-scoped beans.
  The caller is responsible for prototype bean lifecycle management.
```

---

## Bean Scopes

### Singleton (default)

```
  One instance per ApplicationContext.
  All injection points receive the same object.
  State mutations are visible across the whole application.
```

```java
@Component                         // default scope = singleton
public class SingletonCounter {
    private int count = 0;
    public void increment() { count++; }
}

// Two injections — same object:
@Autowired SingletonCounter a;
@Autowired SingletonCounter b;
a.increment();
b.getCount();  // → 1 (a and b are the same instance)
```

### Prototype

```
  New instance every time the bean is requested from the context.
  State is isolated per instance.
  @PreDestroy NOT called by Spring.
```

```java
@Component
@Scope("prototype")
public class PrototypeTask {
    private String status = "NEW";
    public void execute() { status = "DONE"; }
}

// ObjectProvider — correct way to get prototypes inside a singleton:
@Autowired ObjectProvider<PrototypeTask> taskProvider;

PrototypeTask t1 = taskProvider.getObject();
PrototypeTask t2 = taskProvider.getObject();
t1.execute();
t1.getStatus();  // "DONE"
t2.getStatus();  // "NEW" — separate instance, isolated state
```

```
  Scope         Instances   State sharing   Use case
  ─────────────────────────────────────────────────────────────────────────
  singleton     1           Shared          Services, repositories, configs
  prototype     N (1/req)   Isolated        Stateful tasks, mutable helpers
  request       1/HTTP req  Per-request     Web: request-scoped data
  session       1/HTTP ses  Per-session     Web: shopping cart, user prefs
```

---

## AOP — Aspect-Oriented Programming

AOP separates **cross-cutting concerns** (logging, timing, transactions, security) from business logic. Instead of adding log statements inside every method, one aspect intercepts all matching methods automatically.

### Concepts

```
  Join point  — a point in execution (method call, field access, exception)
                Spring AOP supports only method execution join points.

  Pointcut    — an expression that matches a set of join points.

  Advice      — code to run at a join point: @Before, @After, @Around, etc.

  Aspect      — class that holds pointcuts + advice (@Aspect).

  Proxy       — Spring wraps the target bean in a CGLIB proxy.
                Advice fires when the proxy's method is called.
                Internal this.method() calls bypass the proxy — no advice.
```

### Pointcut Expression Syntax

```
  execution( [modifier] returnType [declaring-type].methodName(params) )

  execution(* com.example.service.*.*(..))
    *     — any return type
    com…service.*  — any class in the service package
    .*  — any method name
    (..) — any parameters

  execution(public String com.example.UserService.find*(String))
    — public methods returning String, starting with "find",
      taking one String parameter, in UserService
```

### Advice Types

```java
@Aspect @Component
public class LoggingAspect {

    @Pointcut("execution(* com.example.service.UserService.*(..))")
    public void userServiceMethods() {}

    // Runs before the method — cannot prevent execution
    @Before("userServiceMethods()")
    public void logBefore(JoinPoint jp) {
        // jp.getSignature().getName() → method name
        // jp.getArgs() → method arguments
    }

    // Runs after successful return — binds the return value
    @AfterReturning(pointcut = "userServiceMethods()", returning = "result")
    public void logAfterReturning(JoinPoint jp, Object result) { }

    // Runs when the method throws — does NOT suppress the exception
    @AfterThrowing(pointcut = "userServiceMethods()", throwing = "ex")
    public void logAfterThrowing(JoinPoint jp, Throwable ex) { }

    // Runs always, after return or throw (like finally)
    @After("userServiceMethods()")
    public void logAfter(JoinPoint jp) { }

    // Wraps the entire call — can modify args, return value, suppress exception
    @Around("execution(* com.example.service.ReportService.*(..))")
    public Object timeAround(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        Object result = pjp.proceed();   // calls the actual method
        log.info("elapsed: {}ns", System.nanoTime() - start);
        return result;
    }
}
```

### Advice Execution Order

```
  Normal return:
    @Around (before proceed)
      @Before
        → target method executes
      @AfterReturning
    @After
    @Around (after proceed)

  Exception thrown:
    @Around (before proceed)
      @Before
        → target method throws
      @AfterThrowing
    @After
    @Around rethrows (unless it catches and swallows)
```

### AOP Proxy Limitation

```java
// PROBLEM — internal call bypasses the proxy:
@Service
public class OrderService {
    public void placeOrder(...) {
        validate(...);   // this.validate() — NOT intercepted by AOP
    }
    public void validate(...) { ... }
}

// SOLUTION — inject self-reference OR move the method to a separate bean:
@Service
public class OrderValidator {
    public void validate(...) { ... }   // called from outside → intercepted
}
```

---

## Module 36 — What Was Built

```
  module-36-spring-core/
  ├── pom.xml     (Spring Boot 3.3.5, spring-boot-starter, spring-boot-starter-aop,
  │               spring-boot-starter-test)
  └── src/
      ├── main/java/com/javatraining/springcore/
      │   ├── SpringCoreApplication.java
      │   ├── config/
      │   │   ├── AppConfig.java          — @Configuration + @Bean registration
      │   │   └── AppProperties.java      — record registered as a Spring bean
      │   ├── repository/
      │   │   ├── UserRepository.java     — interface
      │   │   └── InMemoryUserRepository.java  — @Repository implementation
      │   ├── service/
      │   │   ├── UserService.java        — constructor injection (preferred style)
      │   │   ├── NotificationService.java — interface with two implementations
      │   │   ├── EmailNotificationService.java  — @Primary default
      │   │   ├── SmsNotificationService.java    — @Qualifier("smsNotificationService")
      │   │   ├── ReportService.java      — setter injection + @Qualifier
      │   │   └── OrderService.java       — throws to trigger @AfterThrowing
      │   ├── lifecycle/
      │   │   └── AuditService.java       — @PostConstruct / @PreDestroy
      │   ├── scope/
      │   │   ├── SingletonCounter.java   — default singleton scope
      │   │   └── PrototypeTask.java      — @Scope("prototype")
      │   └── aop/
      │       └── LoggingAspect.java      — @Before, @AfterReturning, @AfterThrowing,
      │                                     @After, @Around with named @Pointcut
      └── test/java/com/javatraining/springcore/
          ├── DependencyInjectionTest.java  5 tests — constructor injection, @Primary, @Qualifier
          ├── BeanConfigTest.java           3 tests — @Configuration/@Bean, context lookup, singleton
          ├── BeanLifecycleTest.java        4 tests — @PostConstruct, usability after init
          ├── BeanScopeTest.java            5 tests — singleton identity, prototype isolation,
          │                                           ObjectProvider
          └── AopTest.java                  9 tests — all five advice types, ordering,
                                                      exception propagation
```

All tests: **26 passing**.

---

## Key Takeaways

```
  IoC              Spring creates beans; you declare what you need
  Constructor DI   Preferred: explicit, final fields, testable without Spring
  @Primary         Default when multiple beans match the interface
  @Qualifier       Overrides @Primary — selects by bean name
  @Configuration   Explicit bean registration for third-party or parameterized beans

  @PostConstruct   Runs after injection — open connections, warm caches
  @PreDestroy      Runs on context close — close resources
  Prototype        @Scope("prototype") — fresh instance per getObject() call
  ObjectProvider   Correct way to fetch prototypes from inside a singleton

  AOP Proxy        CGLIB proxy wraps the bean — advice fires on external calls only
  @Before          Runs before; cannot stop execution
  @AfterReturning  Runs on success; can inspect return value
  @AfterThrowing   Runs on exception; does NOT suppress it
  @After           Runs always (like finally)
  @Around          Full control: proceed(), modify return, suppress exception
```
{% endraw %}
