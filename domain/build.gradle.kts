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
    compileSdk = 36
    minSdk = 24

    withHostTestBuilder {}

    withDeviceTestBuilder {
      sourceSetTreeName = "test"
    }.configure {
      instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
  }

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64(),
  )
  jvm("desktop")

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

    val desktopMain by getting

    val desktopTest by getting {
      dependencies {
        implementation(kotlin("test-junit"))
      }
    }
  }
}

// iOS test disabling - not needed when iOS targets are commented out
// listOf("iosX64", "iosArm64", "iosSimulatorArm64").forEach { targetPrefix ->
//   tasks.matching { it.name.startsWith(targetPrefix) && it.name.endsWith("Test") }.configureEach {
//     enabled = false
//   }
// }

tasks.register("test") {
  dependsOn("desktopTest")
}
