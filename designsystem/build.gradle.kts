/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.agentpreview)
  id("vlr.detekt")
  id("vlr.ktlint")
}

kotlin {
  explicitApi()
  applyDefaultHierarchyTemplate()

  androidLibrary {
    namespace = "dev.staticvar.designsystem"
    compileSdk = 37
    minSdk = 24
    androidResources.enable = true
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
      baseName = "DesignSystem"
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
        implementation(compose.animation)
        implementation(libs.compose.icons.lineawesome)
        @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
        implementation(compose.components.resources)
        // Keep for future Fleet/Canary support
        implementation(libs.compose.ui.tooling.preview)
      }
    }

    val commonTest by getting { dependencies { implementation(kotlin("test")) } }

    val androidMain by getting {
      dependencies {
        implementation(libs.activity.compose)
        implementation(libs.core)
        implementation(libs.core.viewtree)
        implementation(libs.emoji2)
        implementation(libs.app.compat)
        implementation(libs.customview.poolingcontainer)
        implementation(libs.lifecycle.runtime)
        implementation(libs.lifecycle.viewmodel)
      }
    }
  }
}

agentPreview {
  maxPreviewParameterValues.set(8)
  android {
    viewport("phone", 393, 852)
    viewport("tablet", 840, 1100)
    screenshot {
      cropToContent.set(true)
      cropPaddingDp.set(20)
    }
  }
}

listOf("iosArm64", "iosSimulatorArm64").forEach { targetPrefix ->
  tasks
    .matching { it.name.startsWith(targetPrefix) && it.name.endsWith("Test") }
    .configureEach { enabled = false }
}

dependencies {
  lintChecks(project(":lint"))
  androidRuntimeClasspath(libs.compose.ui.tooling.cmp)
}
