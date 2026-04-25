---
title: "Module 28 — Build Tools"
parent: "Phase 3 — Intermediate Engineering"
nav_order: 28
render_with_liquid: false
---
{% raw %}

# Module 28 — Build Tools

Maven and Gradle both compile code, resolve dependencies, run tests, and package
artifacts.  This module covers Maven in depth and introduces Gradle as its
modern counterpart.

---

## Maven

### Three Lifecycles

Maven has three independent lifecycles.  Every lifecycle is a sequence of phases;
running a phase runs all phases before it.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  DEFAULT lifecycle  (most used)                                              │
│                                                                              │
│  validate → compile → test → package → verify → install → deploy            │
│      │          │        │       │          │        │          │            │
│      │          │        │       │          │        │          └ push to    │
│      │          │        │       │          │        │            remote     │
│      │          │        │       │          │        └ copy JAR to ~/.m2     │
│      │          │        │       │          └ integration-tests              │
│      │          │        │       └ create JAR / WAR / EAR                   │
│      │          │        └ compile test sources; run unit tests              │
│      │          └ compile src/main/java  →  target/classes                  │
│      └ check pom.xml is well-formed; project structure is valid             │
│                                                                              │
│  Common invocations:                                                         │
│    mvn compile       runs: validate → compile                                │
│    mvn test          runs: validate → compile → test                         │
│    mvn package       runs: validate → compile → test → package               │
│    mvn install       runs: ... → package → install                           │
│    mvn deploy        runs: all phases                                        │
├──────────────────────────────────────────────────────────────────────────────┤
│  CLEAN lifecycle                                                             │
│                                                                              │
│  pre-clean → clean → post-clean                                             │
│                  └ deletes target/                                          │
├──────────────────────────────────────────────────────────────────────────────┤
│  SITE lifecycle                                                              │
│                                                                              │
│  pre-site → site → post-site → site-deploy                                 │
│                └ generates HTML docs in target/site/                        │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Phases vs Goals

```
  Phase      = a step in a lifecycle  (e.g. compile, test, package)
  Goal       = a plugin task          (e.g. compiler:compile, surefire:test)
  Binding    = a goal attached to a phase

  mvn test                  → runs the test phase (and everything before it)
  mvn surefire:test         → runs only the surefire test goal directly
  mvn compiler:compile test → compile goal + test phase
```

---

## Maven Multi-Module Projects

A multi-module (reactor) build groups related modules under one parent POM.
`mvn install` at the root builds all modules in dependency order automatically.

```
  module-28-build-tools/             ← parent POM  (packaging = pom)
  │   pom.xml
  │     └ <packaging>pom</packaging>
  │     └ <modules>
  │         <module>calculator-api</module>
  │         <module>calculator-impl</module>
  │       </modules>
  │     └ <dependencyManagement> — version pinning for all children
  │     └ <build><pluginManagement> — plugin config for all children
  │     └ <profiles> — optional build variants
  │
  ├── calculator-api/                 ← API module  (no implementation)
  │   pom.xml
  │     └ <parent> → module-28-build-tools
  │
  └── calculator-impl/                ← Implementation module
      pom.xml
        └ <parent> → module-28-build-tools
        └ <dependency> on calculator-api  ← inter-module dependency
```

**Build order** — Maven resolves the dependency graph and builds `calculator-api`
before `calculator-impl` automatically, even if you list them in reverse order in
`<modules>`.

### dependencyManagement vs dependencies

```xml
<!-- Parent POM — pins the version, does NOT add the dependency -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>   <!-- version set here once -->
            <scope>test</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Child POM — opts in, but omits <version> -->
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <!-- <version> inherited from parent's dependencyManagement -->
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Maven Dependency Scopes

```
  Scope          Compile  Test  Runtime  Transitive  Typical Use
  ─────────────  ───────  ────  ───────  ──────────  ──────────────────────────
  compile (def)    ✓        ✓     ✓        ✓         jackson-databind, guava
  test             ✗        ✓     ✗        ✗         junit-jupiter, mockito
  provided         ✓        ✓     ✗        ✗         servlet-api (server provides it)
  runtime          ✗        ✓     ✓        ✓         postgresql JDBC driver
  system           ✓        ✓     ✗        ✗         avoid — uses absolute local path

  Compile = on the compiler's classpath
  Runtime = on the JVM classpath when the app runs
  Transitive = propagated to modules that depend on this one
```

---

## Maven Profiles

Profiles activate alternate build configurations:

```xml
<profiles>
    <!-- mvn package -P fast  →  skip tests -->
    <profile>
        <id>fast</id>
        <properties>
            <skipTests>true</skipTests>
        </properties>
    </profile>

    <!-- mvn package -P strict  →  all warnings as errors -->
    <profile>
        <id>strict</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
                        <compilerArgs>
                            <arg>-Xlint:all</arg>
                            <arg>-Werror</arg>
                        </compilerArgs>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

Other activation strategies: OS, environment variable, JDK version, file presence.

---

## Useful Maven Commands

```bash
mvn compile                   # compile only
mvn test                      # compile + run tests
mvn package                   # build JAR/WAR
mvn package -DskipTests       # skip tests (faster packaging)
mvn package -P fast           # activate 'fast' profile
mvn install                   # install to local ~/.m2 repository
mvn dependency:tree           # show full dependency tree
mvn dependency:analyze        # find unused / undeclared deps
mvn versions:display-dependency-updates   # list available upgrades
mvn help:effective-pom        # show fully resolved POM
mvn -pl calculator-impl test  # run tests in one module only
mvn -pl calculator-impl -am test  # also build upstream modules (-am)
```

---

## Gradle

### Three Build Phases

```
┌───────────────────────────────────────────────────────────────────────────┐
│  1. INITIALIZATION                                                        │
│     Read settings.gradle.kts                                              │
│     Determine which projects are part of the build                        │
│     Create a Project object for each                                      │
├───────────────────────────────────────────────────────────────────────────┤
│  2. CONFIGURATION                                                         │
│     Evaluate every build.gradle.kts (root + all subprojects)             │
│     Configure all tasks and their dependencies                            │
│     Build the task execution graph                                        │
│     (All tasks are configured even if not executed)                       │
├───────────────────────────────────────────────────────────────────────────┤
│  3. EXECUTION                                                             │
│     Run only the requested tasks in dependency order                      │
│     Skip up-to-date tasks (incremental build)                            │
│     Use the build cache for tasks with matching inputs (if enabled)       │
└───────────────────────────────────────────────────────────────────────────┘
```

### Gradle Dependency Configurations

```
  Configuration      Compile  Runtime  Exposed to Consumers  Maven Equivalent
  ─────────────────  ───────  ───────  ────────────────────  ─────────────────
  implementation       ✓        ✓       ✗ (encapsulated)      compile (no leak)
  api                  ✓        ✓       ✓ (java-library plugin) compile (transitive)
  compileOnly          ✓        ✗       ✗                     provided
  runtimeOnly          ✗        ✓       ✗                     runtime
  testImplementation   ✓ (test) ✓ (test) ✗                   test
  testRuntimeOnly      ✗        ✓ (test) ✗                   test + runtime
```

**`implementation` vs `api`**

```
  Use implementation (default):
    compile-time dependency is hidden from consumers
    faster incremental recompilation (consumer only recompiles if API changes)

  Use api (requires java-library plugin):
    dependency leaks through to consumers
    consumers can use classes from your dependency directly
```

---

## Gradle Multi-Project Build

```
  gradle-demo/
  ├── settings.gradle.kts          ← declares the project structure
  │     rootProject.name = "gradle-demo"
  │     include("greeter-api", "greeter-impl")
  │
  ├── build.gradle.kts             ← root: shared config via subprojects { }
  │     subprojects {
  │         apply(plugin = "java")
  │         repositories { mavenCentral() }
  │         dependencies { testImplementation(...) }
  │         tasks.withType<Test> { useJUnitPlatform() }
  │     }
  │
  ├── greeter-api/
  │   └── build.gradle.kts         ← no extra config (inherits from root)
  │
  └── greeter-impl/
      └── build.gradle.kts
            dependencies {
                implementation(project(":greeter-api"))   ← project dependency
            }
```

---

## Maven vs Gradle — Side-by-Side

```
  Feature                  Maven                        Gradle (Kotlin DSL)
  ───────────────────────  ───────────────────────────  ─────────────────────────────
  Build file               pom.xml                      build.gradle.kts
  Language                 XML (declarative)            Kotlin/Groovy (imperative)
  Multi-module             <modules> in parent POM      include() in settings.kts
  Version pinning          <dependencyManagement>       platform() BOM or version catalog
  Plugin                   <plugin> in <build>          plugins { id("...") }
  Custom build logic       Mojo (Java class)            task { doLast { ... } }
  Incremental build        Limited                      First-class (input/output tracking)
  Build cache              No                           Yes (local + remote)
  Wrapper                  mvnw / .mvn/wrapper/         gradlew / gradle/wrapper/
  Android support          No                           Yes (required)
  Default test engine      Surefire detects JUnit       useJUnitPlatform() needed
  Parallel execution       -T option                    Default for independent tasks
```

---

## Gradle Build File Anatomy

```kotlin
// build.gradle.kts

plugins {
    java                                    // apply the Java plugin
    // id("application")                   // apply a community/ecosystem plugin
}

group   = "com.javatraining"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))   // reproducible JDK
    }
}

repositories {
    mavenCentral()                         // resolve from Maven Central
}

dependencies {
    implementation("com.google.guava:guava:33.2.0-jre")          // compile + runtime
    compileOnly("org.projectlombok:lombok:1.18.32")              // compile only
    runtimeOnly("org.postgresql:postgresql:42.7.3")              // runtime only
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2") // test only
}

tasks.test {
    useJUnitPlatform()     // enable JUnit 5
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}

// Custom task
tasks.register("hello") {
    doLast { println("Hello from ${project.name}!") }
}
```

---

## Gradle Wrapper

The Gradle wrapper ensures every developer and CI server uses the **exact same
Gradle version** — no installation required.

```
  gradle-demo/
  ├── gradlew               ← Unix shell script  (commit this)
  ├── gradlew.bat           ← Windows batch file (commit this)
  └── gradle/wrapper/
      ├── gradle-wrapper.jar         ← bootstrap binary (commit this)
      └── gradle-wrapper.properties  ← points to specific Gradle version
```

```properties
# gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.8-bin.zip
```

```bash
./gradlew tasks              # list available tasks
./gradlew test               # compile + run tests
./gradlew build              # compile + test + JAR
./gradlew :greeter-impl:test # run tests in one subproject
./gradlew dependencies       # show dependency tree
./gradlew --build-cache test # reuse outputs from cache
```

---

## Incremental Builds

Gradle tracks **inputs and outputs** for every task.  If they haven't changed
since the last run, the task is skipped:

```
  $ ./gradlew test
  > Task :greeter-api:compileJava UP-TO-DATE   ← nothing changed, skipped
  > Task :greeter-api:classes     UP-TO-DATE
  > Task :greeter-impl:compileJava UP-TO-DATE
  > Task :greeter-impl:test       UP-TO-DATE   ← tests skipped
  BUILD SUCCESSFUL in 0s

  Maven always reruns the full lifecycle phase — no equivalent optimisation.
```

---

## Module 28 — What Was Built

This module **is** a Maven multi-module project:

```
  module-28-build-tools/   ← parent POM (packaging=pom)
  ├── pom.xml                dependencyManagement, pluginManagement, profiles
  ├── calculator-api/        Calculator interface + MathUtils  (28 tests)
  │   ├── pom.xml            parent ref; junit test dep (no version)
  │   └── src/
  └── calculator-impl/       BasicCalculator + ScientificCalculator (27 tests)
      ├── pom.xml            parent ref; dep on calculator-api; junit test dep
      └── src/
```

Gradle equivalent lives in `gradle-demo/` — same two-module structure using
Kotlin DSL (`settings.gradle.kts`, `build.gradle.kts`, subproject build files).
Run it with `./gradlew build` if Gradle is installed, or generate the wrapper:
```bash
gradle wrapper --gradle-version 8.8
./gradlew build
```
{% endraw %}
