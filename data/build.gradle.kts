/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.ksp.plugin)
}

kotlin {
  applyDefaultHierarchyTemplate()

  androidLibrary {
    namespace = "dev.staticvar.vlr.data"
    compileSdk = 36
    minSdk = 24

    withHostTestBuilder {}

    withDeviceTestBuilder {
      sourceSetTreeName = "test"
    }.configure {
      instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
  }

  jvm("desktop")

  val fastIos = project.findProperty("fastIos") == "true"
  val iosTargets = if (fastIos) {
    listOf(iosSimulatorArm64())
  } else {
    listOf(iosX64(), iosArm64(), iosSimulatorArm64())
  }
  iosTargets.forEach { iosTarget ->
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

        implementation(libs.sqldelight.coroutines)

        implementation(projects.core)
        implementation(projects.domain)
        implementation(projects.remoteSource)
        implementation(projects.localSource)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation(libs.konvert.annotations)
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

    val desktopTest by getting {
      dependencies {
        implementation(kotlin("test-junit"))
        implementation(libs.sqldelight.sqlite.driver)
      }
    }
  }
}

ksp {
  arg("konvert.non-constructor-properties-mapping", "explicit")
  arg("konvert.invalid-mapping-strategy", "fail")
}

dependencies {
  add("kspAndroid", libs.konvert.processor)
}

listOf("iosX64", "iosArm64", "iosSimulatorArm64").forEach { targetPrefix ->
  tasks.matching { it.name.startsWith(targetPrefix) && it.name.endsWith("Test") }.configureEach {
    enabled = false
  }
}

tasks.register("test") {
  dependsOn("desktopTest")
}

// iOS test disabling - not needed when iOS targets are commented out
// listOf("iosX64", "iosArm64", "iosSimulatorArm64").forEach { targetPrefix ->
//   tasks.matching { it.name.startsWith(targetPrefix) && it.name.endsWith("Test") }.configureEach {
//     enabled = false
//   }
// }
