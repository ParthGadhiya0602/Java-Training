/*
 * Root build.gradle.kts — shared configuration for all subprojects
 * ════════════════════════════════════════════════════════════════════
 * Maven equivalent: parent pom.xml with <dependencyManagement> and
 * <build><pluginManagement>.
 *
 * This file itself produces no artifact (like a parent POM with
 * packaging=pom).  It only configures the subprojects block.
 */

// Version catalog — centralise dependency versions in one place
// (Maven equivalent: <properties> or BOM import)
val junitVersion = "5.10.2"

/*
 * subprojects { } applies the same configuration to every subproject.
 * Use allprojects { } to include the root project too.
 */
subprojects {

    // Apply the Java plugin to every subproject
    // Maven equivalent: maven-compiler-plugin is inherited via pluginManagement
    apply(plugin = "java")

    group   = "com.javatraining"
    version = "1.0.0"

    // Java toolchain — reproducible builds regardless of installed JDK
    // Maven equivalent: <maven.compiler.source> / <maven.compiler.target>
    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    // All subprojects resolve dependencies from Maven Central
    repositories {
        mavenCentral()
    }

    // Shared test dependencies — inherited by all subprojects
    dependencies {
        // "testImplementation" is a configuration (Maven: test scope)
        "testImplementation"("org.junit.jupiter:junit-jupiter:$junitVersion")
        // junit-platform-launcher needed by Gradle's test engine in newer versions
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    // Tell Gradle to use JUnit Platform (Jupiter) as the test engine
    // Maven equivalent: maven-surefire-plugin ≥ 3.x auto-detects this
    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // ── Custom task demo ──────────────────────────────────────────────────
    // Maven equivalent: a custom plugin goal or exec-maven-plugin execution
    tasks.register("buildInfo") {
        group       = "help"
        description = "Prints basic project information"
        doLast {
            println("Project  : ${project.name}")
            println("Version  : ${project.version}")
            println("Java     : ${System.getProperty("java.version")}")
            println("Classpath: ${project.configurations.findByName("compileClasspath")?.files?.size ?: 0} files")
        }
    }

    // ── JAR manifest ─────────────────────────────────────────────────────
    // Maven equivalent: maven-jar-plugin <archive><manifest>
    tasks.withType<Jar> {
        manifest {
            attributes(
                "Implementation-Title"   to project.name,
                "Implementation-Version" to project.version,
                "Built-By"               to System.getProperty("user.name")
            )
        }
    }
}
