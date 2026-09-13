/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.test) apply false
  alias(libs.plugins.baselineprofile) apply false
  alias(libs.plugins.android.kotlin.multiplatform.library) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.compose.multiplatform) apply false
  alias(libs.plugins.ksp.plugin) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.detekt)
  alias(libs.plugins.spotless.plugin)
  id("vlr.detekt")
  id("vlr.ktlint")
}

val composeMetricsEnabled =
  providers.gradleProperty("composeMetrics").map(String::toBoolean).orElse(false)
val composeMetricsOutput =
  providers.gradleProperty("composeMetricsOutput").orElse("build/compose-metrics")
val composeReportModules =
  setOf(
    "feature-about",
    "feature-events",
    "feature-home",
    "feature-matches",
    "feature-news",
    "feature-player",
    "feature-rankings",
    "feature-team",
    "shared-ui",
  )

subprojects {
  if (name !in composeReportModules) return@subprojects

  pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
    extensions.configure<org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension> {
      if (composeMetricsEnabled.get()) {
        val moduleOutput =
          rootProject.layout.projectDirectory
            .dir(composeMetricsOutput.get())
            .dir(project.name)
        metricsDestination = moduleOutput.dir("metrics")
        reportsDestination = moduleOutput.dir("reports")
      }
    }
  }
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
  }
}
