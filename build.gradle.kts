@file:Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.kapt) apply false
  alias(libs.plugins.hilt.plugin) apply false
  alias(libs.plugins.ksp.plugin) apply false
  alias(libs.plugins.secrets.plugin) apply false
  alias(libs.plugins.detekt)
}

buildscript {
  repositories {
    google()
    mavenCentral()
  }

  dependencies {
    classpath(libs.build.agp)
    classpath(libs.build.kotlin)
    classpath(libs.kotlin.serialization)
    classpath(libs.hilt.android.gradle.plugin)
    classpath(libs.google.services)
    classpath(libs.perf.plugin)
    classpath(libs.firebase.crashlytics.gradle)
  }
}

detekt {
  buildUponDefaultConfig = true // preconfigure defaults
  allRules = false // activate all available (even unstable) rules.
  autoCorrect = true
  parallel = true
  config = files("config/detekt/detekt.yml")
  baseline = file("config/detekt/detekt-baseline.xml")
  autoCorrect = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
  reports {
    html.required.set(true) // observe findings in your browser with structure and code snippets
    txt.required.set(true) // similar to the console output, contains issue signature to manually edit baseline files
    md.required.set(true) // simple Markdown format
  }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
  jvmTarget = "17"
}
tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
  jvmTarget = "17"
}