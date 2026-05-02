/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
  id("com.android.test")
  alias(libs.plugins.baselineprofile)
}

android {
  namespace = "com.example.benchmark"
  compileSdk = 34

  defaultConfig {
    minSdk = 23 // Macrobenchmark doesn't work with SDK lower than 23
    targetSdk = 34

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  // [END_EXCLUDE]
  // Note that your module name may have different name
  targetProjectPath = ":app"
  // Enable the benchmark to run separately from the app process
  experimentalProperties["android.experimental.self-instrumenting"] = true

  buildTypes {
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
      localProperties.load(FileInputStream(localPropertiesFile))
    }
    // declare a build type to match the target app's build type
    create("benchmark") {
      isDebuggable = true
      signingConfig = signingConfigs.getByName("debug")
      // Selects release buildType if the benchmark buildType not available in other modules.
      matchingFallbacks += mutableListOf("release")
    }
  }
}

kotlin {
  compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies { implementation(libs.bundles.benchmark) }
