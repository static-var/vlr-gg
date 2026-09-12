/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.test)
  alias(libs.plugins.baselineprofile)
}

android {
  namespace = "dev.staticvar.baselineprofile"
  compileSdk = 37

  defaultConfig {
    minSdk = 28
    targetSdk = 35

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  targetProjectPath = ":androidApp"
}

kotlin {
  compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

baselineProfile {
  useConnectedDevices = true
  enableEmulatorDisplay = true
}

dependencies {
  implementation(libs.android.junit)
  implementation(libs.uiautomator)
  implementation(libs.benchmark.macro)
}
