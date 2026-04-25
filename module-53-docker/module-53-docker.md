---
title: "Module 53 — Docker & Containers"
nav_order: 53
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-53-docker/src){: .btn .btn-outline }

# Module 53 — Docker & Containers

## What this module covers

Packaging a Spring Boot application as a container image two ways: a hand-written
multi-stage `Dockerfile` and Google Jib (daemon-free, via `jib-maven-plugin`).
The module also covers `docker-compose.yml` for local orchestration and the
JVM container-awareness flags that prevent memory over-allocation inside cgroups.

---

## Project structure

```
module-53-docker/
├── Dockerfile                       # multi-stage: builder + slim runtime
├── docker-compose.yml               # single-service Compose file
├── pom.xml                          # Jib plugin configuration
└── src/main/java/com/javatraining/docker/
    ├── DockerApplication.java
    └── product/
        ├── Product.java             # JPA entity
        ├── ProductRepository.java   # JpaRepository
        └── ProductController.java   # GET /products, GET /products/{id}, POST /products
```

---

## Multi-stage Dockerfile

```dockerfile
# Stage 1: Build — full JDK + Maven, used only during image construction
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q        # cached layer — only re-runs if pom.xml changes
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Runtime — slim JRE only (~180 MB vs ~600 MB JDK image)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring                             # non-root user
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

### Why two stages?

The builder stage contains the JDK, Maven, source code, and all build artifacts.
None of that belongs in the production image. By copying only the fat JAR into a fresh
runtime image, the final layer is minimal — no source, no compiler, no Maven cache.

### Dependency caching

`COPY pom.xml . && RUN mvn dependency:go-offline` is a dedicated layer. Docker's layer
cache keeps it until `pom.xml` changes. Subsequent builds with only source changes skip
the dependency download.

### Container-aware JVM flags

| Flag | Effect |
|------|--------|
| `-XX:+UseContainerSupport` | Reads cgroup memory/CPU limits instead of host totals (default on Java 11+, explicit for clarity) |
| `-XX:MaxRAMPercentage=75.0` | Heap = 75% of the cgroup memory limit, leaving headroom for non-heap |

Without `UseContainerSupport`, the JVM reads host RAM (e.g., 16 GB) and sizes the heap
accordingly, crashing the container when it exceeds the cgroup limit.

---

## Jib

```xml
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>3.4.2</version>
    <configuration>
        <from>
            <image>eclipse-temurin:21-jre-alpine</image>
        </from>
        <to>
            <image>java-training/module-53-docker</image>
            <tags><tag>latest</tag><tag>${project.version}</tag></tags>
        </to>
        <container>
            <ports><port>8080</port></ports>
            <jvmFlags>
                <jvmFlag>-XX:+UseContainerSupport</jvmFlag>
                <jvmFlag>-XX:MaxRAMPercentage=75.0</jvmFlag>
            </jvmFlags>
        </container>
    </configuration>
</plugin>
```

| Goal | What it does |
|------|--------------|
| `mvn jib:build` | Builds and pushes directly to a remote registry — no Docker daemon required |
| `mvn jib:dockerBuild` | Loads image into the local Docker daemon |
| `mvn jib:buildTar` | Writes `target/jib-image.tar` — useful in CI where Docker isn't available |

Jib separates the application into layers (dependencies, resources, classes) so that
only the changed layer is pushed on rebuild — much faster than re-pushing a fat JAR layer.

### Dockerfile vs Jib

| | Dockerfile | Jib |
|---|---|---|
| Docker daemon needed to build | Yes | No |
| Layer optimisation | Manual | Automatic (deps / resources / classes) |
| Non-root user | Manual `adduser` | Default (`nonroot` user) |
| Build speed (unchanged deps) | Good if layers cached | Excellent (only changed layer) |
| Transparency | Full control | Convention over configuration |

---

## Docker Compose

```yaml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 20s
    restart: unless-stopped
```

`healthcheck` lets Compose (and Docker Swarm) know when the container is actually
ready to accept traffic, not just started. Dependent services can use `condition: service_healthy`.

---

## REST API

```
GET  /products          → 200 [ { id, name, price, category }, … ]
GET  /products/{id}     → 200 { id, name, price, category }
                        → 404 if not found
POST /products          → 201 { id, name, price, category }
                           Location: /products/{id}
```

The `POST` endpoint returns `201 Created` with a `Location` header pointing to the
new resource URI — the standard REST convention for creation responses.

---

## Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        productRepository.saveAll(List.of(
            new Product(null, "Widget", new BigDecimal("9.99"),  "Tools"),
            new Product(null, "Gadget", new BigDecimal("19.99"), "Electronics")
        ));
    }
```

`deleteAll()` + re-seed in `@BeforeEach` gives each test a clean, known state
without relying on transaction rollback or test execution order.

| Test | Assertion |
|------|-----------|
| `get_all_returns_all_products` | 200, array size 2, correct names |
| `get_by_id_returns_product` | 200, correct name and category |
| `get_by_unknown_id_returns_404` | 404 |
| `post_creates_product_and_returns_201_with_location` | 201, id assigned, Location header set |

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test`
Result: **4/4 pass**

---

## Key decisions

| Decision | Reason |
|---|---|
| Multi-stage build separates JDK from JRE | Final image contains only the JRE and fat JAR — no compiler, no Maven cache, ~420 MB smaller |
| `COPY pom.xml` before `COPY src` | Separates dependency resolution into its own layer; Docker cache skips it when only source changes |
| Non-root `USER spring` in Dockerfile | Container escapes root by default; if the process is exploited, the attacker has no root privileges on the host |
| `UseContainerSupport` + `MaxRAMPercentage` | JVM reads cgroup limits, not host RAM — prevents OOM kills when the container has a memory limit |
| Jib alongside Dockerfile | Jib is the faster, daemon-free path for CI; Dockerfile gives full control and is universal |
| `ResponseEntity.created(location)` for POST | Returns 201 + Location header — the standard HTTP contract for resource creation |
