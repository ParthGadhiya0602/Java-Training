/*
 * settings.gradle.kts — Gradle Settings Script
 * ════════════════════════════════════════════════════════════════════
 * Evaluated first in every Gradle build (Initialization phase).
 * Responsibilities:
 *   1. Name the root project
 *   2. Declare which subprojects exist
 *   3. Configure the plugin / dependency resolution strategy (optional)
 *
 * Maven equivalent: the <modules> section in a parent pom.xml.
 */

rootProject.name = "gradle-demo"

// Declare subprojects — Gradle looks for <name>/build.gradle.kts in each
include("greeter-api", "greeter-impl")

/*
 * Multi-project structure created by the include() calls above:
 *
 *  gradle-demo/           ← root project (this file lives here)
 *  ├── build.gradle.kts   ← root build: shared config for all subprojects
 *  ├── greeter-api/
 *  │   └── build.gradle.kts
 *  └── greeter-impl/
 *      └── build.gradle.kts   (depends on :greeter-api)
 */
