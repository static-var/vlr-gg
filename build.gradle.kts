@file:Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.kapt) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.hilt.plugin) apply false
  id("io.gitlab.arturbosch.detekt").version("1.22.0-RC2")
}

buildscript {
  val hilt_version = "2.44.2"
  val kotlin_version = "1.7.20"
  repositories {
    google()
    mavenCentral()
  }

  dependencies {
    classpath("com.android.tools.build:gradle:8.0.0-alpha09")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlin_version")
    classpath("com.google.dagger:hilt-android-gradle-plugin:$hilt_version")
    classpath("com.google.gms:google-services:4.3.14")
    classpath("com.google.firebase:perf-plugin:1.4.2")
    classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.2")
  }
}

detekt {
  // Version of Detekt that will be used. When unspecified the latest detekt
  // version found will be used. Override to stay on the same version.
  toolVersion = "1.22.0-RC2"

  // The directories where detekt looks for source files.
  // Defaults to `files("src/main/java", "src/test/java", "src/main/kotlin", "src/test/kotlin")`.
  source = files("src/main/java", "src/main/kotlin")

  // Builds the AST in parallel. Rules are always executed in parallel.
  // Can lead to speedups in larger projects. `false` by default.
  parallel = true

  // Define the detekt configuration(s) you want to use.
  // Defaults to the default detekt configuration.
  config = files("${projectDir.path}/config/detekt/detekt.yml")

  // Applies the config files on top of detekt's default config file. `false` by default.
  buildUponDefaultConfig = false

  // Turns on all the rules. `false` by default.
  allRules = false

  // Disables all default detekt rulesets and will only run detekt with custom rules
  // defined in plugins passed in with `detektPlugins` configuration. `false` by default.
  disableDefaultRuleSets = false

  // Adds debug output during task execution. `false` by default.
  debug = false

  // If set to `true` the build does not fail when the
  // maxIssues count was reached. Defaults to `false`.
  ignoreFailures = true

  // Android: Don't create tasks for the specified build types (e.g. "release")
  ignoredBuildTypes = listOf("release")

  // Android: Don't create tasks for the specified build flavor (e.g. "production")
  ignoredFlavors = listOf("production")

  // Android: Don't create tasks for the specified build variants (e.g. "productionRelease")
  ignoredVariants = listOf("productionRelease")

  // Specify the base path for file paths in the formatted reports.
  // If not set, all file paths reported will be absolute file path.
  basePath = projectDir.path
}