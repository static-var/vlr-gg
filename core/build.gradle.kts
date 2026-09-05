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
    namespace = "dev.staticvar.vlr.core"
    compileSdk = 36
    minSdk = 24
  }

  jvm("desktop")

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64(),
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "Core"
      isStatic = true
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.kotlin.stdlib)
        implementation(libs.coroutines.core)
        implementation(libs.koin.core)
        implementation(libs.multiplatform.settings)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.multiplatform.settings.test)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation(libs.koin.android)
      }
    }
  }
}
