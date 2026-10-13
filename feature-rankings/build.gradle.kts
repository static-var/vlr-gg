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
    namespace = "dev.staticvar.vlr.featurerankings"
    compileSdk {
      version = release(37) { minorApiLevel = 1 }
    }
    minSdk = 24
    androidResources.enable = true

    withDeviceTestBuilder {
      sourceSetTreeName = "deviceTest"
    }.configure {
      instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
  }

  val fastIos = project.findProperty("fastIos") == "true"
  val iosTargets =
    if (fastIos) {
      listOf(iosSimulatorArm64())
    } else {
      listOf(iosArm64(), iosSimulatorArm64())
    }
  iosTargets.forEach { target ->
    target.binaries.framework {
      baseName = "FeatureRankings"
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
        implementation(libs.koin.core.viewmodel)
        api(libs.lifecycle.viewmodel.core)
        implementation(projects.core)
        implementation(projects.designsystem)
        implementation(projects.sharedUi)
        implementation(projects.domain)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.coroutine.test)
        implementation(libs.multiplatform.settings)
        implementation(libs.multiplatform.settings.test)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation(libs.activity.compose)
      }
    }

    val androidDeviceTest by getting {
      dependencies {
        implementation(libs.android.junit)
        implementation(libs.android.test.runner)
        implementation(libs.android.test.espresso)
        implementation("org.jetbrains.compose.ui:ui-test-junit4:${libs.versions.compose.multiplatform.get()}")
      }
    }
  }
}

dependencies {
  lintChecks(project(":lint"))
  androidRuntimeClasspath(libs.compose.ui.tooling.cmp)
}
