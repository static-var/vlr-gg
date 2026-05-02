/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  lintChecks(project(":lint"))
  androidRuntimeClasspath(libs.compose.ui.tooling.cmp)
}

kotlin {
  applyDefaultHierarchyTemplate()

  androidLibrary {
    namespace = "dev.staticvar.vlr.shared"
    compileSdk = 36
    minSdk = 24
  }

  jvm("desktop")

  val fastIos = project.findProperty("fastIos") == "true"
  val iosTargets = if (fastIos) {
    listOf(iosSimulatorArm64())
  } else {
    listOf(iosX64(), iosArm64(), iosSimulatorArm64())
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
        implementation(libs.kotlinx.serialization)
        implementation(libs.koin.core)
        implementation(libs.navigation3.ui.cmp)
        implementation(projects.designsystem)
        implementation(projects.core)
        implementation(projects.localSource)
        implementation(projects.remoteSource)
        implementation(projects.data)
        implementation(projects.featureAbout)
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
      }
    }

    val iosMain by getting

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }

    val desktopMain by getting {
      dependencies {
        implementation(compose.desktop.currentOs)
      }
    }

    val desktopTest by getting
  }
}
