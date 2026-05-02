/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
  FileInputStream(localPropertiesFile).use(localProperties::load)
}

val envProperties = Properties()
val envFile = rootProject.file(".env")
if (envFile.exists()) {
  FileInputStream(envFile).use(envProperties::load)
}

val authToken =
  (
    System.getenv("VLR_AUTH_TOKEN")
      ?: localProperties.getProperty("TOKEN")
      ?: localProperties.getProperty("VLR_AUTH_TOKEN")
      ?: envProperties.getProperty("VLR_AUTH_TOKEN")
      ?: ""
    )
    .trim()
    .removeSurrounding("\"")
    .removeSurrounding("'")
val escapedAuthToken = authToken.replace("\\", "\\\\").replace("\"", "\\\"")

android {
  namespace = "dev.staticvar.vlr.android"
  compileSdk = 36

  defaultConfig {
    applicationId = "dev.staticvar.vlr"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0.0"
    buildConfigField("String", "TOKEN", "\"$escapedAuthToken\"")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

dependencies {
  // Compose Multiplatform
  implementation(compose.runtime)
  implementation(compose.foundation)
  implementation(compose.material3)
  implementation(compose.ui)
  implementation(compose.components.resources)
  implementation(libs.compose.ui.tooling.preview)

  // Preview support
  debugImplementation(libs.compose.ui.tooling.cmp)

  // Activity Compose
  implementation(libs.activity.compose)
  implementation(libs.koin.android)

  // Project modules
  implementation(projects.shared)
  implementation(projects.designsystem)
  lintChecks(project(":lint"))
}
