/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
  id("vlr.ktlint")
}

kotlin {
  explicitApi()
  applyDefaultHierarchyTemplate()

  androidLibrary {
    namespace = "dev.staticvar.vlr.sharedui"
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
      baseName = "SharedUi"
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
        implementation(libs.lifecycle.runtime)
        implementation(compose.components.resources)
        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor3)
        implementation(libs.kotlinx.datetime)
        implementation(projects.designsystem)
        api(projects.core)
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
        implementation(libs.ktor.okhttp)
      }
    }

    val androidDeviceTest by getting {
      dependencies {
        implementation(libs.android.junit)
        implementation(libs.android.test.runner)
        implementation("org.jetbrains.compose.ui:ui-test-junit4:${libs.versions.compose.multiplatform.get()}")
      }
    }

    val iosMain by getting {
      dependencies {
        implementation(libs.ktor.darwin)
      }
    }
  }
}

if (providers.gradleProperty("enableAgentPreview").orNull != "false") {
  apply(from = rootProject.file("gradle/agent-preview.gradle"))
}

dependencies {
  lintChecks(project(":lint"))
  androidRuntimeClasspath(libs.compose.ui.tooling.cmp)
}
