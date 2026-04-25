---
title: "Module 37 — Spring Boot"
nav_order: 37
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-37-spring-boot/src){: .btn .btn-outline }

# Module 37 — Spring Boot

Spring Boot makes Spring-based applications runnable with minimal configuration:
**Auto-configuration** — infers and wires beans based on classpath and properties;
**Starters** — curated dependency groups that pull in everything a feature needs;
**Profiles** — swap configuration between environments without code changes;
**@ConfigurationProperties** — type-safe, validated binding from property files to Java classes;
**Actuator** — production-ready observability endpoints out of the box.

---

## How Auto-Configuration Works

```
  Your application starts
       |
       v  SpringApplication.run()
       |
       v  Load application.properties / environment
       |
       v  @EnableAutoConfiguration (inside @SpringBootApplication)
       |    reads META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
       |    each listed class is a @Configuration with conditions
       |
       v  For each auto-configuration class:
       |    evaluate conditions (@ConditionalOnClass, @ConditionalOnMissingBean, ...)
       |    if all pass: register the @Bean methods
       |    if any fail: skip the whole configuration
       |
       v  ApplicationContext ready
```

### Key Conditional Annotations

```java
// Bean created only if a class is on the classpath
// (e.g. JPA auto-config runs only when Hibernate is present)
@ConditionalOnClass(DataSource.class)

// Bean created only if no other bean of that type exists yet
// (lets users override the default by defining their own)
@ConditionalOnMissingBean(DataSource.class)

// Bean created when a property has a specific value
@ConditionalOnProperty(
    prefix = "app.feature-flags",
    name = "notifications-enabled",
    havingValue = "true",
    matchIfMissing = false)   // absent property = condition fails

// Bean created when the app runs as a web application
@ConditionalOnWebApplication

// Bean created when a specific bean exists
@ConditionalOnBean(SomeService.class)
```

### @ConditionalOnMissingBean — The Override Pattern

```
  Spring Boot ships: DataSourceAutoConfiguration creates a DataSource bean.
  You need a custom DataSource? Just define your own @Bean DataSource.
  Spring Boot sees your bean → @ConditionalOnMissingBean → skips the default.

  This pattern appears everywhere in Spring Boot auto-configuration:
    "Provide a sensible default, but step aside when the user takes over."
```

```java
// In your @Configuration — overrides Spring Boot's default:
@Bean
public DataSource myDataSource() {
    return new HikariDataSource(customConfig());
}
// Spring Boot's DataSourceAutoConfiguration is now skipped.
```

---

## Starters

```
  A starter is a single Maven/Gradle dependency that brings in a coherent
  set of libraries, auto-configuration, and managed versions.

  spring-boot-starter-web
    includes: spring-webmvc, spring-core, jackson-databind,
              spring-boot-starter-tomcat, spring-boot-starter-validation
    auto-configures: DispatcherServlet, Jackson ObjectMapper,
                     embedded Tomcat server

  spring-boot-starter-data-jpa
    includes: hibernate-core, spring-data-jpa, spring-orm
    auto-configures: EntityManagerFactory, JpaTransactionManager,
                     Spring Data repository proxies

  spring-boot-starter-actuator
    includes: micrometer-core, spring-boot-actuator
    auto-configures: /actuator endpoints, health indicators, metrics

  You pick the starters you need. Spring Boot wires everything together.
  No XML, no manual bean registration.
```

---

## @ConfigurationProperties

### Why Not @Value?

```
  @Value("${app.max-connections}")    @ConfigurationProperties(prefix = "app")
  private int maxConnections;         public class AppProperties {
                                          @Min(1) @Max(200)
                                          private int maxConnections;
                                      }

  @Value problems:
    Scattered across the codebase — no single place to see all config
    No type validation — wrong value → runtime crash
    No IDE autocompletion
    Repeated prefix everywhere

  @ConfigurationProperties benefits:
    All related properties in one class
    @Validated: fail fast on startup with a clear error
    IDE completion (with spring-boot-configuration-processor)
    Relaxed binding: max-connections, maxConnections, MAX_CONNECTIONS all work
```

### Binding Setup

```java
// Option 1: @ConfigurationPropertiesScan on the main class (preferred)
@SpringBootApplication
@ConfigurationPropertiesScan          // discovers all @ConfigurationProperties in package tree
public class MyApp { ... }

// Option 2: @EnableConfigurationProperties per class
@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class, DatabaseProperties.class})
public class MyApp { ... }
```

### Simple and Nested Properties

```java
// application.properties:
//   app.name=Java Training App
//   app.max-connections=50
//   app.feature-flags.notifications-enabled=true
//   app.feature-flags.analytics-enabled=false

@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {

    @NotBlank
    private String name;

    @Min(1) @Max(200)
    private int maxConnections;

    @Valid                            // cascades validation into the nested object
    private FeatureFlags featureFlags = new FeatureFlags();

    // getters + setters required for binding

    public static class FeatureFlags {
        private boolean notificationsEnabled;  // ← app.feature-flags.notifications-enabled
        private boolean analyticsEnabled;
        // getters + setters
    }
}
```

### Relaxed Binding Rules

```
  Property key form       Example                     Maps to Java field
  ──────────────────────────────────────────────────────────────────────
  Kebab-case (canonical)  app.max-connections         maxConnections
  Camel-case              app.maxConnections           maxConnections
  Underscore              app.max_connections          maxConnections
  SCREAMING_SNAKE         APP_MAX_CONNECTIONS          maxConnections
                          (environment variable form)

  All four forms bind to the same field.
  Canonical form (kebab-case) is recommended in .properties files.
```

### Validation on Startup

```
  @Validated on the @ConfigurationProperties class activates Bean Validation
  (Hibernate Validator) on the bound values at startup.

  If app.max-connections=0 (violates @Min(1)):
  → Application refuses to start
  → Error: "Field error in object 'app' on field 'maxConnections': rejected value [0]"
  → Explicit, actionable error — not a NullPointerException 3 layers deep
```

---

## Profiles

### Profile-Specific Beans

```java
// Loaded only when "dev" profile is active
@Component
@Profile("dev")
public class DevEnvironmentInfo implements EnvironmentInfo {
    public String getName() { return "development"; }
    public boolean isDebugEnabled() { return true; }
}

// Loaded only when "prod" profile is active
@Component
@Profile("prod")
public class ProdEnvironmentInfo implements EnvironmentInfo {
    public String getName() { return "production"; }
    public boolean isDebugEnabled() { return false; }
}
```

### Profile-Specific Properties

```
  File                          Loaded when
  ──────────────────────────────────────────────────────────────
  application.properties        always (base values)
  application-dev.properties    "dev" profile active (overrides base)
  application-prod.properties   "prod" profile active (overrides base)

  Merge rule: base + profile-specific; profile wins on conflict.
```

```
  application.properties        application-dev.properties
  ─────────────────────────     ─────────────────────────────────
  app.database.url=defaultdb    app.database.url=devdb    ← wins
  app.database.pool-size=10     app.database.pool-size=5  ← wins
  app.name=Java Training App    (not set) → inherits "Java Training App"
```

### Activating Profiles

```
  CLI argument:      java -jar app.jar --spring.profiles.active=dev
  Property file:     spring.profiles.active=dev  (not recommended in prod)
  Environment var:   SPRING_PROFILES_ACTIVE=dev
  Test annotation:   @ActiveProfiles("dev")

  Multiple profiles: --spring.profiles.active=dev,oauth
```

---

## Actuator

Actuator adds production-ready HTTP endpoints to your application.

### Exposing Endpoints

```properties
# application.properties
management.endpoints.web.exposure.include=health,info,metrics,env
management.endpoint.health.show-details=always   # show component-level details
management.info.env.enabled=true                 # expose info.* properties
```

### Built-in Endpoints

```
  Endpoint          URL                   Purpose
  ─────────────────────────────────────────────────────────────────────────
  /actuator/health  GET /actuator/health  UP/DOWN + per-component details
  /actuator/info    GET /actuator/info    Application metadata
  /actuator/metrics GET /actuator/metrics Micrometer counters/gauges/timers
  /actuator/env     GET /actuator/env     All environment properties + sources
  /actuator/beans   GET /actuator/beans   All Spring beans in the context
  /actuator/conditions  GET ...           Which auto-configs matched/missed
```

### /actuator/health Response

```json
{
  "status": "UP",
  "components": {
    "app": {
      "status": "UP",
      "details": { "app": "Java Training App", "maxConnections": 50 }
    },
    "diskSpace": { "status": "UP", "details": { "free": 120000000000 } },
    "ping": { "status": "UP" }
  }
}
```

### Custom HealthIndicator

```java
@Component
public class AppHealthIndicator implements HealthIndicator {

    private final AppProperties props;

    public AppHealthIndicator(AppProperties props) {
        this.props = props;
    }

    @Override
    public Health health() {
        if (props.getMaxConnections() > 0) {
            return Health.up()
                    .withDetail("app", props.getName())
                    .withDetail("maxConnections", props.getMaxConnections())
                    .build();
        }
        return Health.down()
                .withDetail("reason", "maxConnections must be positive")
                .build();
    }
}
```

```
  Spring Boot auto-discovers all HealthIndicator beans.
  The overall /health status is the WORST of all component statuses:
    all UP → UP
    any DOWN → DOWN
    any OUT_OF_SERVICE → OUT_OF_SERVICE
```

### Custom InfoContributor

```java
@Component
public class BuildInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("build", Map.of(
                "artifact", "my-app",
                "javaVersion", System.getProperty("java.version")
        ));
    }
}
```

```json
// /actuator/info response:
{
  "app": { "name": "Java Training App", "version": "1.0.0" },
  "build": { "artifact": "my-app", "javaVersion": "21.0.1" }
}
```

---

## Lesson: Stale Test Resources in Maven

```
  Problem: src/test/resources/application.properties was created, then deleted.
  But Maven had already compiled it into target/test-classes/application.properties.
  The stale file shadowed src/main/resources/application.properties.
  Result: app.name was null → @NotBlank validation failed → context refused to start.

  Rule: test classpath (target/test-classes) has higher priority than main
  classpath (target/classes). A same-named file in test resources shadows the
  main one entirely — it does not merge.

  Fix: mvn clean test clears target/ before compiling. When in doubt after
  deleting or renaming resources, always clean first.

  Alternative: use @SpringBootTest(properties = {"key=value"}) for test-specific
  properties rather than a properties file, to avoid the shadowing problem.
```

---

## Module 37 — What Was Built

```
  module-37-spring-boot/
  ├── pom.xml     (Spring Boot 3.3.5, spring-boot-starter-web,
  │               spring-boot-starter-actuator, spring-boot-starter-validation,
  │               spring-boot-starter-test)
  └── src/
      ├── main/
      │   ├── java/com/javatraining/springboot/
      │   │   ├── SpringBootDemoApplication.java   — @ConfigurationPropertiesScan
      │   │   ├── config/
      │   │   │   ├── AppProperties.java            — @ConfigurationProperties(prefix="app")
      │   │   │   │                                   nested FeatureFlags, @Validated
      │   │   │   ├── DatabaseProperties.java        — @ConfigurationProperties(prefix="app.database")
      │   │   │   └── ConditionalConfig.java         — @ConditionalOnProperty, @ConditionalOnMissingBean
      │   │   ├── profile/
      │   │   │   ├── EnvironmentInfo.java            — interface
      │   │   │   ├── DevEnvironmentInfo.java         — @Profile("dev")
      │   │   │   └── ProdEnvironmentInfo.java        — @Profile("prod")
      │   │   └── actuator/
      │   │       ├── AppHealthIndicator.java         — custom HealthIndicator
      │   │       └── BuildInfoContributor.java       — custom InfoContributor
      │   └── resources/
      │       ├── application.properties             — base config
      │       ├── application-dev.properties         — dev overrides
      │       └── application-prod.properties        — prod overrides
      └── test/java/com/javatraining/springboot/
          ├── ConfigurationPropertiesTest.java  10 tests — binding, nested,
          │                                              @ConditionalOnProperty, @ConditionalOnMissingBean
          ├── ProfileDevTest.java               6 tests — dev bean, debug, URL override
          ├── ProfileProdTest.java              5 tests — prod bean, no debug, URL override
          └── ActuatorTest.java                 6 tests — /health UP, custom indicator,
                                                          /info env + custom contributor
```

All tests: **27 passing**.

---

## Key Takeaways

```
  Auto-configuration    @Conditional* annotations decide whether each @Bean is registered
  @ConditionalOnMissing "Provide a default unless the user defines their own" pattern
  Starters              One dependency = all libraries + auto-config for a feature

  @ConfigurationProperties  Type-safe property binding; prefix groups related config
  @Validated                Fail fast with clear validation errors at startup
  Relaxed binding       max-connections, maxConnections, MAX_CONNECTIONS all map to same field
  @ConfigurationPropertiesScan  Auto-discovers all @ConfigurationProperties in the package tree

  Profiles              @Profile("dev") / @Profile("prod") swap beans per environment
  Profile properties    application-{profile}.properties merges on top of base; profile wins
  @ActiveProfiles       Test annotation to activate profiles in @SpringBootTest

  Actuator /health      UP/DOWN aggregated from all HealthIndicator beans
  Actuator /info        Merged from InfoContributor beans + info.* properties
  Custom indicators     Implement HealthIndicator / InfoContributor → auto-discovered

  Stale test resources  target/test-classes/application.properties shadows main properties.
                        Always mvn clean test after deleting resource files.
```
