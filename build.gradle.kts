@file:Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.kapt) apply false
  alias(libs.plugins.hilt.plugin) apply false
  alias(libs.plugins.ksp.plugin) apply false
  alias(libs.plugins.secrets.plugin) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.kotlin.parcelize) apply false
  alias(libs.plugins.baselineprofile) apply false
  alias(libs.plugins.gms.plugin) apply false
  alias(libs.plugins.detekt)
  alias(libs.plugins.spotless.plugin)
  alias(libs.plugins.firebase.perf) apply false
  id("vlr.detekt")
  id("vlr.ktfmt")
  alias(libs.plugins.androidTest) apply false
  alias(libs.plugins.jetbrainsKotlinJvm) apply false
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
  }
}
