/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.sentry.kmp)
}

sentryKmp {
  linker.xcodeprojPath.set(rootProject.file("iosApp/iosApp.xcodeproj").absolutePath)
}

dependencies {
  lintChecks(project(":lint"))
  androidRuntimeClasspath(libs.compose.ui.tooling.cmp)
}

kotlin {
  applyDefaultHierarchyTemplate()

  androidLibrary {
    namespace = "dev.staticvar.vlr.shared"
    compileSdk = 37
    minSdk = 24
  }

  val fastIos = project.findProperty("fastIos") == "true"
  val iosTargets = if (fastIos) {
    listOf(iosSimulatorArm64())
  } else {
    listOf(iosArm64(), iosSimulatorArm64())
  }
  iosTargets.forEach { target ->
    target.binaries.framework {
      baseName = "shared"
      isStatic = true
      // Explicit bundle ID to silence Kotlin/Native warning and stabilize metadata
      freeCompilerArgs += listOf("-Xbinary=bundleId=dev.staticvar.vlr.shared")
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
        implementation(compose.materialIconsExtended)
        implementation(libs.coroutines.core)
        implementation(libs.lifecycle.runtime)
        implementation(libs.lifecycle.viewmodel.compose.cmp)
        implementation(libs.lifecycle.viewmodel.navigation3)
        implementation(libs.kotlinx.serialization)
        implementation(libs.koin.core)
        implementation(libs.koin.compose)
        implementation(libs.koin.compose.viewmodel)
        implementation(libs.koin.compose.navigation3)
        implementation(libs.navigation3.ui.cmp)
        implementation(projects.designsystem)
        implementation(projects.core)
        implementation(projects.localSource)
        implementation(projects.remoteSource)
        implementation(projects.data)
        implementation(projects.domain)
        implementation(projects.sharedUi)
        implementation(projects.featureAbout)
        implementation(projects.featureHome)
        implementation(projects.featureEvents)
        implementation(projects.featureMatches)
        implementation(projects.featureNews)
        implementation(projects.featurePlayer)
        implementation(projects.featureRankings)
        implementation(projects.featureTeam)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation(libs.activity.compose)
        implementation(libs.sentry.android)
      }
    }

    val iosMain by getting

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
  }
}
