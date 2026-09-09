/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
plugins {
  kotlin("multiplatform")
  id("com.android.kotlin.multiplatform.library")
}

kotlin {
  applyDefaultHierarchyTemplate()

  androidLibrary {
    namespace = "dev.staticvar.vlr.domain"
    compileSdk = 37
    minSdk = 24

    withHostTestBuilder {}

    withDeviceTestBuilder {
      sourceSetTreeName = "test"
    }.configure {
      instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
  }

  listOf(
    iosArm64(),
    iosSimulatorArm64(),
  )

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.kotlin.stdlib)
        implementation(libs.coroutines.core)
        implementation(libs.kotlinx.datetime)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.coroutine.test)
      }
    }

    val androidMain by getting

    val iosMain by getting
  }
}

tasks.register("test") {
  dependsOn("iosSimulatorArm64Test")
}
