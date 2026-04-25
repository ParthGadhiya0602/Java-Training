# ── Stage 1: Build ────────────────────────────────────────────────────────────
# Uses the full JDK + Maven image only during the build stage.
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy POM first and resolve dependencies separately so this layer is cached
# as long as pom.xml doesn't change — even if source code changes.
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
# Slim JRE-only image: no JDK, no Maven, no build tools.
# eclipse-temurin:21-jre-alpine is ~180 MB vs ~600 MB for the JDK image.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as a non-root user — principle of least privilege
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]
