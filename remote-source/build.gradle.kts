/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp.plugin)
  // poko removed
}

kotlin {
  applyDefaultHierarchyTemplate()

  androidLibrary {
    namespace = "dev.staticvar.vlr.remotesource"
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
        implementation(projects.core)
        implementation(libs.kotlin.stdlib)
        implementation(libs.coroutines.core)
        implementation(libs.kotlinx.serialization)
        implementation(libs.ktor.core)
        api(libs.ktor.http)
        implementation(libs.ktor.negotiation)
        implementation(libs.ktor.kotlinx.json)
        implementation(libs.ktor.logging)
        implementation(libs.ktor.encoding)
        implementation(libs.koin.core)
        implementation(libs.ksoup)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.koin.test)
        implementation(libs.coroutine.test)
        implementation(libs.ktor.mock)
        implementation(libs.kotlinx.io.core)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation(libs.koin.android)
        implementation(libs.ktor.okhttp)
        implementation(libs.logging.interceptor)
      }
    }

    val iosMain by getting {
      dependencies { implementation(libs.ktor.darwin) }
    }

    val iosTest by getting {
      resources.srcDir("src/commonTest/resources")
    }

    val desktopMain by getting {
      dependencies { implementation(libs.ktor.java) }
    }

    val desktopTest by getting {
      resources.srcDir("src/commonTest/resources")
      dependencies { implementation(kotlin("test-junit")) }
    }
  }
}

listOf("iosX64", "iosArm64", "iosSimulatorArm64").forEach { targetPrefix ->
  tasks.matching { it.name.startsWith(targetPrefix) && it.name.endsWith("Test") }.configureEach {
    enabled = false
  }
}
