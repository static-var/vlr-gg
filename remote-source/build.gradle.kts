/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

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

  }
}

tasks.withType<KotlinNativeSimulatorTest>().configureEach {
  val fixtures = layout.projectDirectory.dir("src/commonTest/resources")
  inputs.dir(fixtures).withPathSensitivity(PathSensitivity.RELATIVE)
  environment("SIMCTL_CHILD_VLR_TEST_FIXTURES", fixtures.asFile.absolutePath)
}
