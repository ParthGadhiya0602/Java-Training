/*
 * greeter-api/build.gradle.kts
 * ════════════════════════════════════════════════════════════════════
 * Pure API module - contains only the Greeter interface.
 * Inherits all configuration from the root build.gradle.kts.
 * No additional dependencies needed here.
 *
 * Maven equivalent: calculator-api/pom.xml with <parent> reference
 * and no extra <dependencies>.
 */

// Dependency configurations available (from root's "apply plugin: java"):
//
//   implementation   - compile + runtime; NOT exposed to consumers
//                      Maven equivalent: compile scope (but NOT transitive to dependents)
//
//   api              - compile + runtime; exposed to consumers
//                      (requires "java-library" plugin, not just "java")
//                      Maven equivalent: compile scope (transitive)
//
//   compileOnly      - compile only; absent at runtime
//                      Maven equivalent: provided scope
//
//   runtimeOnly      - runtime only; absent during compilation
//                      Maven equivalent: runtime scope
//
//   testImplementation - test compile + runtime
//                        Maven equivalent: test scope

// This module has no additional dependencies beyond what the root provides.
