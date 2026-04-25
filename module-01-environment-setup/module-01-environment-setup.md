---
title: "01 - Environment Setup"
parent: "Phase 1 - Fundamentals"
nav_order: 1
render_with_liquid: false
---

{% raw %}

# Module 01 - Environment Setup

Before writing a single line of Java, you need a solid, reproducible toolchain.
This module covers every tool you will use throughout this training and explains
**why** each one exists.

---

## What You Will Set Up

| Tool               | Purpose                                                    |
| ------------------ | ---------------------------------------------------------- |
| SDKMAN             | Manage multiple JDK versions side-by-side                  |
| Java 21 (Temurin)  | The Java runtime + compiler                                |
| Maven              | Build tool, dependency management (used in early modules)  |
| Gradle             | Build tool with a programmable DSL (used in later modules) |
| IntelliJ IDEA (CE) | IDE - the standard for professional Java development       |
| Git                | Version control                                            |
| Docker Desktop     | Containers - needed from Module 22 onward                  |

---

## 1. Install SDKMAN

SDKMAN lets you install and switch between JDK versions with one command.
This matters in the real world - different projects pin different JDK versions.

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

Verify:

```bash
sdk version
```

---

## 2. Install Java 21 (Eclipse Temurin)

Temurin is the free, production-grade OpenJDK distribution from the Eclipse
Adoptium project. It is what most companies run in production.

```bash
sdk install java 21.0.3-tem
```

Set it as default:

```bash
sdk default java 21.0.3-tem
```

Verify:

```bash
java -version
javac -version
```

Expected output:

```
openjdk version "21.0.3" 2024-04-16
OpenJDK Runtime Environment Temurin-21.0.3+9 (build 21.0.3+9)
OpenJDK 64-Bit Server VM Temurin-21.0.3+9 (build 21.0.3+9, mixed mode)
```

### Why Java 21?

Java 21 is the current LTS (Long-Term Support) release. Key features we will use:

- **Records** (Java 16+) - concise immutable data classes
- **Sealed classes** (Java 17+) - controlled type hierarchies
- **Pattern matching** (Java 21) - switch with type patterns
- **Virtual threads** (Java 21) - lightweight concurrency via Project Loom
- **Sequenced collections** (Java 21) - consistent first/last API across collections

---

## 3. Install Maven

```bash
sdk install maven
```

Verify:

```bash
mvn -version
```

Expected:

```
Apache Maven 3.9.x
Maven home: /Users/you/.sdkman/candidates/maven/current
Java version: 21.0.3, vendor: Eclipse Adoptium
```

---

## 4. Install Gradle

```bash
sdk install gradle
```

Verify:

```bash
gradle -version
```

---

## 5. Install Git

macOS:

```bash
brew install git       # if you have Homebrew
# or: xcode-select --install  (installs git via Xcode CLI tools)
```

Ubuntu/Debian:

```bash
sudo apt update && sudo apt install git
```

Configure your identity (required for commits):

```bash
git config --global user.name  "Your Name"
git config --global user.email "you@example.com"
git config --global init.defaultBranch main
```

---

## 6. Install IntelliJ IDEA Community Edition

Download from: https://www.jetbrains.com/idea/download/

Or via Homebrew on macOS:

```bash
brew install --cask intellij-idea-ce
```

### Recommended IntelliJ Plugins

Install these from **Settings → Plugins → Marketplace**:

| Plugin           | Why                                                    |
| ---------------- | ------------------------------------------------------ |
| SonarLint        | Static analysis, catches bugs before they reach review |
| CheckStyle-IDEA  | Enforces coding standards                              |
| Lombok           | Reduces boilerplate (used later in Spring modules)     |
| Docker           | Docker integration for container modules               |
| GitToolBox       | Inline blame, better git UX                            |
| Rainbow Brackets | Visual bracket matching - saves sanity in deep nesting |
| Maven Helper     | Analyze dependency conflicts                           |

---

## 7. Install Docker Desktop

Download from: https://www.docker.com/products/docker-desktop/

Verify after install:

```bash
docker --version
docker compose version
```

Docker is not needed until Module 22, but installing it now avoids interrupting
your learning flow later.

---

## 8. Verify the Full Toolchain

Run this checklist - every command should return a version number, not an error:

```bash
java -version       # openjdk 21...
javac -version      # javac 21...
mvn -version        # Apache Maven 3.9...
gradle -version     # Gradle 8...
git --version       # git version 2...
docker --version    # Docker version 26...
```

---

## 9. Understand the JAVA_HOME Variable

`JAVA_HOME` is an environment variable that tells tools like Maven and Gradle
where the JDK lives. SDKMAN sets this automatically, but you should know what
it is:

```bash
echo $JAVA_HOME
# /Users/you/.sdkman/candidates/java/current
```

If a tool complains about JAVA_HOME, you can set it explicitly:

```bash
export JAVA_HOME=$(sdk home java current)
```

---

## 10. Your First Java Program (Manual Compilation)

Before relying on any IDE or build tool, understand what happens under the hood.

Create a file `Hello.java`:

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Java toolchain works.");
        System.out.println("Java version: " + Runtime.version());
    }
}
```

Compile and run manually:

```bash
javac Hello.java      # produces Hello.class (bytecode)
java Hello            # JVM executes the bytecode
```

This is exactly what Maven and Gradle automate - `javac` + classpath management

- test runner + packaging. Knowing the manual steps demystifies what build tools do.

---

## 11. Managing Multiple JDK Versions

In real projects you may need to switch between Java versions per project.
SDKMAN handles this.

Install an additional version:

```bash
sdk install java 17.0.11-tem
```

Switch for the current terminal session:

```bash
sdk use java 17.0.11-tem
java -version
```

Switch back to 21:

```bash
sdk use java 21.0.3-tem
```

Pin a specific version per project directory using `.sdkmanrc`:

```bash
# Inside a project directory:
sdk env init          # creates .sdkmanrc with current version
sdk env               # reads .sdkmanrc and switches automatically
```

---

## Checkpoint

Before moving to Module 02, confirm you can:

- [ ] Run `java -version` and see Java 21
- [ ] Run `mvn -version` and see Maven 3.9+
- [ ] Run `gradle -version` and see Gradle 8+
- [ ] Manually compile and run `Hello.java`
- [ ] Open IntelliJ IDEA and create a new project

---

## Next

[Module 02 - Java Basics](../module-02-java-basics/)
{% endraw %}
