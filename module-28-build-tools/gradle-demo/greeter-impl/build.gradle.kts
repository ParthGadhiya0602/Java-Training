/*
 * greeter-impl/build.gradle.kts
 * ════════════════════════════════════════════════════════════════════
 * Implementation module — depends on :greeter-api.
 * Maven equivalent: calculator-impl/pom.xml with
 *   <dependency> on calculator-api at compile scope.
 */

dependencies {
    // project(":greeter-api") is a project dependency — resolved from the
    // local build, not from Maven Central.
    // Maven equivalent: <dependency> on calculator-api at compile scope.
    implementation(project(":greeter-api"))
}
