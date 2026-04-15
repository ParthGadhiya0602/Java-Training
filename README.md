# Java Training — Beginner to Production-Ready

A structured, module-by-module Java training curriculum. Each module contains
real, non-trivial examples designed to force genuine understanding — not just
copy-paste familiarity.

---

## How This Repo Works

- Each module lives in its own directory: `module-NN-topic-name/`
- Every module has a `README.md` with **theory + diagrams + annotated snippets**
  before any runnable code — read the README first, run the code second
- Code examples are intentionally non-trivial — simple enough to follow,
  complex enough to be worth studying
- Build tool progression: early modules use **Maven**, later modules introduce
  **Gradle**, production modules use both side-by-side
- Work through modules **in order** — later modules build on earlier ones

---

## Curriculum Roadmap

> **Total: 59 modules across 6 phases.**

---

### Phase 1 — Java Fundamentals
> Goal: Write correct, idiomatic Java. Understand the type system, OOP model,
> and exception handling before touching any library or framework.

| # | Module | Build Tool | Status |
|---|--------|------------|--------|
| [01](module-01-environment-setup/) | **Environment Setup** — JDK 21, SDKMAN, Maven, Gradle, IntelliJ, Docker | — | Done |
| [02](module-02-java-basics/) | **Java Basics** — primitives, literals, widening/narrowing cast, autoboxing, Integer cache, BigDecimal, all operators | Maven | Done |
| [03](module-03-control-flow/) | **Control Flow** — if/else, switch expressions (arrow + yield), for/while/do-while, break/continue/labels | Maven | — |
| [04](module-04-methods/) | **Methods** — overloading, varargs, recursion, call stack, pass-by-value vs pass-by-reference | Maven | — |
| [05](module-05-arrays-strings/) | **Arrays, Strings & Regex** — arrays, multi-dimensional, StringBuilder, String pool, String API, regex patterns | Maven | — |
| [06](module-06-enums/) | **Enums** — fields, constructors, methods, abstract methods per-constant, EnumSet, EnumMap, strategy via enum | Maven | — |
| [07](module-07-oop-classes/) | **OOP: Classes & Objects** — constructors, `this`, static members, records, object lifecycle, `equals`/`hashCode` | Maven | — |
| [08](module-08-oop-inheritance/) | **OOP: Inheritance & Polymorphism** — extends, super, method overriding, casting, sealed classes, `final` | Maven | — |
| [09](module-09-oop-interfaces/) | **OOP: Interfaces & Abstract Classes** — default/static/private methods, functional interfaces, abstract classes | Maven | — |
| [10](module-10-oop-encapsulation/) | **OOP: Encapsulation** — access modifiers, immutability, defensive copying, builder pattern | Maven | — |
| [11](module-11-nested-classes/) | **Nested & Inner Classes** — static nested, inner, local, anonymous classes — when and why each exists | Maven | — |
| [12](module-12-exceptions/) | **Exception Handling** — checked/unchecked, custom exception hierarchy, try-with-resources, best practices | Maven | — |

---

### Phase 2 — Core Java APIs
> Goal: Use the standard library confidently. Understand concurrency deeply,
> model Java's memory guarantees, and know how the JVM executes your code.

| # | Module | Build Tool | Status |
|---|--------|------------|--------|
| [13](module-13-collections/) | **Collections Framework** — List, Set, Map, Queue, Deque, Comparator, Comparable, iteration patterns | Maven | — |
| [14](module-14-generics/) | **Generics** — type parameters, bounded types, wildcards (`?`, `extends`, `super`), type erasure | Maven | — |
| [15](module-15-functional/) | **Functional Programming** — Lambda, Stream API, Optional, method references, custom collectors | Maven | — |
| [16](module-16-io-nio/) | **I/O, NIO.2 & Serialization** — File API, Paths, Channels, WatchService, object serialization, Externalizable | Maven | — |
| [17](module-17-multithreading/) | **Multithreading & Thread Management** — Thread lifecycle & states, Runnable/Callable, `synchronized`, `volatile`, `wait`/`notify`, `join`, daemon threads, ThreadLocal, thread safety pitfalls | Maven | — |
| [18](module-18-java-memory-model/) | **Java Memory Model & Advanced Concurrency** — happens-before, visibility guarantees, memory barriers, ExecutorService, thread pools, ReentrantLock, Semaphore, CountDownLatch, CyclicBarrier, atomic classes, concurrent collections, CompletableFuture, ForkJoinPool, deadlock/livelock/starvation | Maven | — |
| [19](module-19-networking/) | **Networking & Sockets** — TCP/UDP sockets, ServerSocket, HTTP client (Java 11+), URL handling, basic protocols | Maven | — |
| [20](module-20-annotations/) | **Annotations** — built-in annotations, custom annotations, retention policies, targets, annotation processors | Maven | — |
| [21](module-21-reflection/) | **Reflection API** — Class objects, inspecting fields/methods/constructors, dynamic invocation, proxies, use-cases | Maven | — |
| [22](module-22-modern-java/) | **Modern Java (9–21)** — JPMS module system, `jshell`, text blocks, records, sealed classes, pattern matching in switch, virtual threads (Project Loom) | Maven | — |
| [23](module-23-algorithms/) | **Algorithms & Data Structures** — Big-O analysis, sorting (merge, quick, heap), binary search, linked list, stack, queue, tree, graph (BFS/DFS), hash map internals | Maven | — |
| [24](module-24-jvm-internals/) | **JVM Internals** — heap/stack/metaspace, GC algorithms (G1, ZGC, Shenandoah), classloaders, JIT compilation, safepoints | — | — |

---

### Phase 3 — Intermediate Engineering
> Goal: Write production-quality code. Apply design patterns, test thoroughly,
> master build tooling, and debug systematically.

| # | Module | Build Tool | Status |
|---|--------|------------|--------|
| [25](module-25-design-patterns/) | **Design Patterns** — GoF creational (singleton, factory, builder), structural (proxy, decorator, adapter), behavioral (strategy, observer, command, template method) | Maven | — |
| [26](module-26-solid-principles/) | **SOLID Principles** — before/after refactoring examples for every principle, DRY, YAGNI, LoD | Maven | — |
| [27](module-27-unit-testing/) | **Unit Testing** — JUnit 5 (lifecycle, parameterized, nested), Mockito, AssertJ, test doubles, TDD workflow | Maven | — |
| [28](module-28-build-tools/) | **Build Tools Deep Dive** — Maven lifecycle/plugins/profiles, Gradle tasks/DSL/incremental builds, multi-module projects | Maven + Gradle | — |
| [29](module-29-logging/) | **Logging** — SLF4J facade, Logback, Log4j2, MDC (contextual logging), structured/JSON logs, log levels strategy | Maven | — |
| [30](module-30-debugging/) | **Debugging & Code Quality** — IntelliJ debugger (breakpoints, watches, evaluate), heap dump analysis, SonarQube, Checkstyle, SpotBugs, PMD | — | — |

---

### Phase 4 — Databases & Persistence
> Goal: Interact with relational and NoSQL databases correctly — transactions,
> relationships, migrations, connection pooling, and ORM internals.

| # | Module | Build Tool | Status |
|---|--------|------------|--------|
| [31](module-31-jdbc/) | **JDBC** — DriverManager, PreparedStatement, ResultSet, transaction management, HikariCP connection pool, batch updates | Maven | — |
| [32](module-32-jpa-hibernate/) | **JPA & Hibernate** — EntityManager, Session/SessionFactory, entity lifecycle, relationships (OneToOne/OneToMany/ManyToMany), fetch types (LAZY/EAGER), dirty checking, L1 & L2 cache, N+1 problem & fix, HQL, JPQL, Criteria API, `@Transactional`, Hibernate Validator | Maven | — |
| [33](module-33-spring-data/) | **Spring Data JPA** — repositories, derived query methods, `@Query`, projections, pagination, sorting, auditing (`@CreatedDate`/`@LastModifiedDate`) | Gradle | — |
| [34](module-34-nosql/) | **NoSQL** — MongoDB with Spring Data (documents, aggregations), Redis with Spring Data (hash, list, pub/sub), use-case comparison | Gradle | — |
| [35](module-35-db-migration/) | **Database Migration** — Flyway (versioned migrations, repeatable, undo), Liquibase (changeSets, rollback), best practices for zero-downtime migrations | Gradle | — |

---

### Phase 5 — Spring Ecosystem
> Goal: Build production Spring Boot applications — REST APIs, security,
> reactive, batch processing, and server-side rendering.

| # | Module | Build Tool | Status |
|---|--------|------------|--------|
| [36](module-36-spring-core/) | **Spring Core** — IoC container, DI (constructor/setter/field), bean lifecycle, scopes, AOP (aspects, pointcuts, advice) | Gradle | — |
| [37](module-37-spring-boot/) | **Spring Boot** — auto-configuration internals, starters, profiles, externalized config (`@ConfigurationProperties`), Actuator endpoints | Gradle | — |
| [38](module-38-lombok-mapstruct/) | **Lombok & MapStruct** — reducing boilerplate (`@Data`, `@Builder`, `@Slf4j`), DTO ↔ entity mapping, Lombok pitfalls | Gradle | — |
| [39](module-39-spring-rest/) | **Spring REST APIs** — `@RestController`, Bean Validation, global error handling (`@ControllerAdvice`), HATEOAS, content negotiation | Gradle | — |
| [40](module-40-spring-security/) | **Spring Security** — authentication, JWT (issuing/validating), OAuth2 / OIDC, method-level security (`@PreAuthorize`) | Gradle | — |
| [41](module-41-spring-testing/) | **Spring Testing** — `@SpringBootTest`, test slices (`@WebMvcTest`, `@DataJpaTest`), Testcontainers, WireMock, consumer-driven contract tests | Gradle | — |
| [42](module-42-spring-webflux/) | **Spring WebFlux & Reactive** — Project Reactor (`Mono`/`Flux`), reactive repositories, backpressure, SSE, when to use reactive vs imperative | Gradle | — |
| [43](module-43-thymeleaf/) | **Thymeleaf** — template syntax, layouts/fragments, form handling, Spring MVC integration, security dialect | Gradle | — |
| [44](module-44-spring-data-rest/) | **Spring Data REST** — auto-generated REST from repositories, HAL/HAL-FORMS, projections, event handlers, customisation | Gradle | — |
| [45](module-45-spring-batch/) | **Spring Batch** — Job/Step model, ItemReader/Processor/Writer, chunk processing, partitioning, retry/skip, scheduling | Gradle | — |

---

### Phase 6 — Production & Architecture
> Goal: Design, deploy, and operate production Java systems — microservices,
> messaging, observability, containers, cloud, and security hardening.

| # | Module | Build Tool | Status |
|---|--------|------------|--------|
| [46](module-46-microservices/) | **Microservices Architecture** — decomposition strategies, inter-service communication, saga pattern, outbox pattern | Gradle | — |
| [47](module-47-spring-cloud/) | **Spring Cloud** — Config Server, Eureka service discovery, API Gateway, Circuit Breaker (Resilience4j), distributed tracing (Micrometer Tracing) | Gradle | — |
| [48](module-48-messaging/) | **Messaging** — Kafka (producers, consumers, Kafka Streams), RabbitMQ (exchanges, routing, dead-letter queues), exactly-once semantics | Gradle | — |
| [49](module-49-caching/) | **Caching** — Spring Cache abstraction, Redis cache-aside pattern, cache warming, TTL, eviction policies, cache invalidation | Gradle | — |
| [50](module-50-api-design/) | **API Design** — OpenAPI 3 / Swagger, REST versioning strategies, API contracts, consumer-driven contract testing (Pact) | Gradle | — |
| [51](module-51-graphql/) | **GraphQL with Spring** — schema-first design, queries/mutations/subscriptions, DataFetcher, N+1 problem (DataLoader), federation basics | Gradle | — |
| [52](module-52-grpc/) | **gRPC** — Protocol Buffers (proto3), unary/server-streaming/bidirectional RPCs, gRPC-Java, interceptors, vs REST trade-offs | Gradle | — |
| [53](module-53-docker/) | **Docker & Containers** — Dockerfile, multi-stage builds, Docker Compose, health checks, image size optimization, Jib (build without Dockerfile) | Gradle | — |
| [54](module-54-observability/) | **Observability** — Micrometer metrics, Prometheus, Grafana dashboards, distributed tracing (OpenTelemetry), log aggregation (ELK/Loki) | Gradle | — |
| [55](module-55-ci-cd/) | **CI/CD** — GitHub Actions pipelines, build/test/scan/deploy stages, SonarQube quality gates, artifact publishing, semantic versioning | Both | — |
| [56](module-56-performance/) | **Performance & Profiling** — async-profiler, Java Mission Control, GC tuning, JMeter load testing, virtual threads (Loom) benchmarks | Both | — |
| [57](module-57-security-hardening/) | **Security Hardening** — OWASP Top 10 for Java (SQLi, XSS, IDOR, etc.), dependency vulnerability scanning (OWASP Dependency-Check), HashiCorp Vault for secrets | Gradle | — |
| [58](module-58-cloud-deployment/) | **Cloud Deployment** — Spring Boot on AWS (ECS, EKS, RDS, ElastiCache), GCP (Cloud Run, Cloud SQL), 12-factor app, environment parity | Gradle | — |
| [59](module-59-capstone/) | **Capstone Project** — end-to-end production system: multi-service app, secured REST + gRPC + Kafka, Dockerized, deployed to cloud, CI/CD pipeline | Both | — |

---

## Coverage Summary

| Phase | Modules | Key Topics |
|-------|---------|------------|
| 1 — Fundamentals | 01–12 | Language, OOP, exceptions |
| 2 — Core APIs | 13–24 | Collections, concurrency, JMM, JVM |
| 3 — Intermediate | 25–30 | Patterns, testing, build tools |
| 4 — Databases | 31–35 | JDBC, Hibernate/JPA, migrations |
| 5 — Spring | 36–45 | Boot, REST, security, reactive, batch |
| 6 — Production | 46–59 | Microservices, cloud, observability |
| **Total** | **59 modules** | |

---

## Prerequisites

- macOS, Linux, or Windows (WSL recommended)
- Basic terminal / command-line comfort
- Nothing else — we install everything from Module 01

## Java Version

This training targets **Java 21 LTS**. Modern language features (records,
sealed classes, switch expressions, text blocks, virtual threads, pattern
matching) are used throughout — they are production-standard today.

---

> Start here: [Module 01 — Environment Setup](module-01-environment-setup/)
