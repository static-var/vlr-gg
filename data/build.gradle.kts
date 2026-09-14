/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
  applyDefaultHierarchyTemplate()

  androidLibrary {
    namespace = "dev.staticvar.vlr.data"
    compileSdk = 37
    minSdk = 24

    withHostTestBuilder {}

    withDeviceTestBuilder {
      sourceSetTreeName = "test"
    }.configure {
      instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
  }

  val fastIos = project.findProperty("fastIos") == "true"
  val iosTargets = if (fastIos) {
    listOf(iosSimulatorArm64())
  } else {
    listOf(iosArm64(), iosSimulatorArm64())
  }
  iosTargets.forEach { iosTarget ->
    iosTarget.binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable>().configureEach {
      linkerOpts("-lsqlite3")
    }
    iosTarget.binaries.framework {
      baseName = "data"
      isStatic = true
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.kotlin.stdlib)
        implementation(libs.coroutines.core)
        implementation(libs.koin.core)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.serialization)

        implementation(libs.sqldelight.coroutines)

        implementation(projects.core)
        implementation(projects.domain)
        implementation(projects.remoteSource)
        implementation(projects.localSource)
      }
    }

    val iosTest by getting {
      dependencies {
        implementation(libs.sqldelight.native.driver)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.koin.test)
        implementation(libs.coroutine.test)
        implementation(libs.turbine)
      }
    }
  }
}

tasks.register("test") {
  dependsOn("iosSimulatorArm64Test")
}
