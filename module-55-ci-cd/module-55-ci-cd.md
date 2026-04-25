---
title: "Module 55 — CI/CD"
parent: "Phase 6 — Production & Architecture"
nav_order: 55
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-55-ci-cd/src){: .btn .btn-outline }

# Module 55 — CI/CD

## What this module covers

A production CI/CD pipeline using GitHub Actions: build → test → JaCoCo coverage →
SonarQube static analysis → Docker image build → (on tag) GitHub Release with semantic
versioning. The application demonstrates semantic-version validation in service logic,
tested with `@ParameterizedTest` + `@ValueSource`.

---

## Project structure

```
module-55-ci-cd/
├── .github/
│   └── workflows/
│       ├── ci.yml        # push/PR pipeline: verify → scan → docker
│       └── release.yml   # tag pipeline: version bump → package → GitHub Release
├── pom.xml               # enforcer, JaCoCo, sonar-maven-plugin
└── src/
    ├── main/java/com/javatraining/cicd/
    │   ├── CicdApplication.java
    │   ├── GlobalExceptionHandler.java   # IllegalArgumentException → 400
    │   └── release/
    │       ├── Release.java              # JPA entity: id, name, version, releasedAt
    │       ├── ReleaseRepository.java
    │       ├── ReleaseService.java       # semver validation + CRUD
    │       └── ReleaseController.java    # POST /releases, GET /releases/{id}
    └── test/java/com/javatraining/cicd/release/
        ├── ReleaseServiceTest.java       # @ParameterizedTest, no Spring (12 tests)
        └── ReleaseControllerTest.java    # @SpringBootTest + MockMvc (2 tests)
```

---

## CI workflow — `ci.yml`

```yaml
on:
  push:
    branches: [main, "release/**"]
  pull_request:
    branches: [main]

jobs:
  build-test-scan:
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0              # full history for SonarQube blame

      - uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: temurin
          cache: maven                # caches ~/.m2/repository between runs

      - run: mvn --batch-mode verify  # compile → test → JaCoCo report

      - name: SonarQube scan
        if: github.event_name == 'push'   # skip on PRs from forks (no secrets)
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
        run: mvn --batch-mode sonar:sonar

      - uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: target/site/jacoco/

  docker:
    needs: build-test-scan
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: docker/build-push-action@v5
        with:
          push: false
          tags: |
            java-training/module-55:${{ github.sha }}
            java-training/module-55:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

### Design decisions

| Decision | Reason |
|---|---|
| `fetch-depth: 0` on checkout | SonarQube needs full git history for blame annotations (which commit introduced each line) |
| `sonar:sonar` guarded by `event_name == 'push'` | Pull requests from forks cannot access repo secrets — running the step on PRs would silently fail or expose the token |
| `cache: maven` on `setup-java` | Restores `~/.m2/repository` from a GitHub Actions cache key; typically saves 1-3 minutes per run |
| `docker` job depends on `build-test-scan` | Ensures broken builds never produce a Docker image |
| Docker layer cache with `type=gha` | Reuses unchanged image layers across runs via GitHub Actions cache, not a registry push |

---

## Release workflow — `release.yml`

Triggered when a semantic version tag (`v1.2.3` or `v1.2.3-rc.1`) is pushed:

```yaml
on:
  push:
    tags:
      - "v[0-9]+.[0-9]+.[0-9]+"
      - "v[0-9]+.[0-9]+.[0-9]+-*"

steps:
  - name: Extract version
    run: echo "VERSION=${GITHUB_REF_NAME#v}" >> "$GITHUB_OUTPUT"

  - name: Set POM version
    run: mvn versions:set -DnewVersion=${{ steps.version.outputs.VERSION }} -DgenerateBackupPoms=false

  - name: Build
    run: mvn package -DskipTests

  - uses: softprops/action-gh-release@v2
    with:
      files: target/*.jar
      generate_release_notes: true
      prerelease: ${{ contains(github.ref_name, '-') }}
```

`${GITHUB_REF_NAME#v}` strips the leading `v` from the tag using Bash parameter expansion.
`mvn versions:set` updates `pom.xml` without creating a backup POM. The release JAR is
attached to the GitHub Release automatically. Pre-release tags (containing `-`) are
marked accordingly.

---

## Maven plugins

### Maven Enforcer

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <configuration>
        <rules>
            <requireJavaVersion><version>[21,)</version></requireJavaVersion>
            <requireMavenVersion><version>[3.9,)</version></requireMavenVersion>
        </rules>
    </configuration>
</plugin>
```

Enforcer runs at `validate` phase — the very first thing Maven does. A developer on Java 17
or a CI runner with an old Maven will fail immediately with a clear message, not halfway
through compilation.

### JaCoCo

```xml
<execution>
    <id>prepare-agent</id>
    <goals><goal>prepare-agent</goal></goals>    <!-- before test: instruments bytecode -->
</execution>
<execution>
    <id>report</id>
    <phase>verify</phase>
    <goals><goal>report</goal></goals>           <!-- after test: writes target/site/jacoco/ -->
</execution>
```

`mvn verify` generates `target/site/jacoco/jacoco.xml`. SonarQube reads this file
(configured via `<sonar.coverage.jacoco.xmlReportPaths>`) to display line and branch
coverage in the SonarQube dashboard.

### SonarQube

```xml
<!-- pom.xml <properties> -->
<sonar.projectKey>java-training_module-55</sonar.projectKey>
<sonar.coverage.jacoco.xmlReportPaths>target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
```

`mvn sonar:sonar` is NOT bound to a lifecycle phase — it runs only when explicitly invoked.
This prevents accidental scans on `mvn package` and keeps local builds fast.

---

## Semantic versioning in application code

`ReleaseService` enforces semver on the version field before persisting:

```java
private static final Pattern SEMVER = Pattern.compile("\\d+\\.\\d+\\.\\d+(-[\\w.]+)?");

public Release create(Release release) {
    if (release.getVersion() == null || !SEMVER.matcher(release.getVersion()).matches()) {
        throw new IllegalArgumentException(
                "Version must follow semantic versioning (e.g. 1.2.3 or 1.2.3-rc.1): "
                + release.getVersion());
    }
    return releaseRepository.save(release);
}
```

`GlobalExceptionHandler` maps `IllegalArgumentException` → `400 Bad Request`.

---

## Tests

### Parameterized unit tests — `ReleaseServiceTest`

```java
@ParameterizedTest
@ValueSource(strings = {"1.0.0", "2.3.4", "10.20.30", "1.0.0-rc.1", "3.14.0-beta"})
void create_accepts_valid_semantic_versions(String version) { ... }

@ParameterizedTest
@ValueSource(strings = {"1.0", "1", "v1.0.0", "1.0.0.0", "not-a-version", "", "1.0.0-"})
void create_rejects_invalid_semantic_versions(String version) { ... }
```

`@ParameterizedTest` with `@ValueSource` expands to one test per value — boundary cases
are checked exhaustively without writing a separate `@Test` method per case. The test
method name appears in the report as `create_accepts_valid_semantic_versions(1.0.0)` etc.

### Integration tests — `ReleaseControllerTest`

```java
@Test
void post_with_valid_semver_returns_201_with_location()

@Test
void post_with_invalid_semver_returns_400()
```

| Class | Type | Tests |
|---|---|---|
| `ReleaseServiceTest` | Unit, `@ParameterizedTest` | 12 |
| `ReleaseControllerTest` | `@SpringBootTest` + MockMvc | 2 |
| **Total** | | **14** |

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test`
Result: **14/14 pass**

---

## Key decisions

| Decision | Reason |
|---|---|
| `@ParameterizedTest` + `@ValueSource` over individual `@Test` methods | Covers more boundary cases with less code; each value appears as a separate test in CI reports |
| `sonar:sonar` not bound to a lifecycle phase | Prevents scans on every local `mvn verify`; CI runs it explicitly after `verify` |
| `fetch-depth: 0` in CI checkout | SonarQube blame requires full git history; shallow clones break the annotation feature |
| Semver regex in service, not controller | Business rule belongs in the service layer; controller stays thin and delegates validation |
| `prerelease: ${{ contains(github.ref_name, '-') }}` | Tags with `-` (e.g., `-rc.1`, `-beta`) are pre-releases by convention; the expression detects this automatically |
{% endraw %}
