/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

kotlin {
  explicitApi()
  applyDefaultHierarchyTemplate()

  androidLibrary {
    namespace = "dev.staticvar.vlr.featureevents"
    compileSdk = 36
    minSdk = 24
    androidResources.enable = true
  }

  jvm("desktop")

  val fastIos = project.findProperty("fastIos") == "true"
  val iosTargets =
    if (fastIos) {
      listOf(iosSimulatorArm64())
    } else {
      listOf(iosX64(), iosArm64(), iosSimulatorArm64())
    }
  iosTargets.forEach { target ->
    target.binaries.framework {
      baseName = "FeatureEvents"
      isStatic = true
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(compose.ui)
        implementation(compose.components.resources)
        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.coroutines.core)
        implementation(libs.koin.core)
        implementation(projects.core)
        implementation(projects.designsystem)
        implementation(projects.domain)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.coroutine.test)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation(libs.activity.compose)
      }
    }
  }
}

dependencies {
  lintChecks(project(":lint"))
  androidRuntimeClasspath(libs.compose.ui.tooling.cmp)
}
