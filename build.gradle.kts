/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
plugins {
  alias(libs.plugins.android.application) apply false
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
