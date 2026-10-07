/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.sentry.kmp)
}

val sentryCocoaVersion = "9.30.1"

sentryKmp {
  autoInstall.apple.provider.set(io.sentry.kotlin.multiplatform.gradle.AppleDependencyProvider.NONE)
  autoInstall.apple.sentryCocoaVersion.set(sentryCocoaVersion)
  linker.xcodeprojPath.set(rootProject.file("iosApp/iosApp.xcodeproj").absolutePath)
  System.getenv("CI_DERIVED_DATA_PATH")?.let { derivedDataPath ->
    linker.frameworkPath.set("$derivedDataPath/SourcePackages/artifacts/sentry-cocoa/Sentry/Sentry.xcframework")
  }
}

tasks.withType<KotlinNativeLink>().configureEach {
  val pinnedVersion = sentryCocoaVersion
  onlyIf("Sentry Cocoa framework matches the pinned version") { task ->
    val frameworkPlists = (task as KotlinNativeLink).linkerOpts
      .filter { it.startsWith("-F") }
      .map { File(it.removePrefix("-F"), "Sentry.framework/Info.plist") }
      .filter { it.isFile }
      .distinct()
    check(frameworkPlists.isNotEmpty()) {
      "Sentry Cocoa $pinnedVersion framework is missing. Resolve the iOS package and set CI_DERIVED_DATA_PATH to its DerivedData directory."
    }
    frameworkPlists.forEach { plist ->
      val process = ProcessBuilder("/usr/libexec/PlistBuddy", "-c", "Print :CFBundleShortVersionString", plist.path)
        .redirectErrorStream(true)
        .start()
      val version = process.inputStream.bufferedReader().use { it.readText().trim() }
      check(process.waitFor() == 0 && version == pinnedVersion) {
        "Expected Sentry Cocoa $pinnedVersion, found $version at $plist. Resolve the iOS package and set CI_DERIVED_DATA_PATH to its DerivedData directory."
      }
    }
    true
  }
}

dependencies {
  lintChecks(project(":lint"))
  androidRuntimeClasspath(libs.compose.ui.tooling.cmp)
}

kotlin {
  applyDefaultHierarchyTemplate()

  androidLibrary {
    namespace = "dev.staticvar.vlr.shared"
    androidResources.enable = true
    withHostTestBuilder {}
    compileSdk {
      version = release(37) { minorApiLevel = 1 }
    }
    minSdk = 24
  }

  val fastIos = project.findProperty("fastIos") == "true"
  val iosTargets = if (fastIos) {
    listOf(iosSimulatorArm64())
  } else {
    listOf(iosArm64(), iosSimulatorArm64())
  }
  iosTargets.forEach { target ->
    target.binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable>().configureEach {
      linkerOpts("-lsqlite3")
    }
    target.binaries.framework {
      baseName = "shared"
      isStatic = true
      export(projects.core)
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
        implementation(libs.compose.adaptive)
        implementation(libs.compose.adaptive.navigation.suite)
        implementation(compose.ui)
        implementation(compose.components.resources)
        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.coroutines.core)
        implementation(libs.lifecycle.runtime)
        implementation(libs.lifecycle.viewmodel.compose.cmp)
        implementation(libs.lifecycle.viewmodel.navigation3)
        implementation(libs.kotlinx.serialization)
        implementation(libs.multiplatform.settings)
        implementation(libs.koin.core)
        implementation(libs.koin.compose)
        implementation(libs.koin.compose.viewmodel)
        implementation(libs.koin.compose.navigation3)
        implementation(libs.navigation3.ui.cmp)
        implementation(projects.designsystem)
        api(projects.core)
        implementation(projects.localSource)
        implementation(projects.remoteSource)
        implementation(projects.data)
        implementation(projects.domain)
        implementation(projects.sharedUi)
        implementation(projects.featureAbout)
        implementation(projects.featureHome)
        implementation(projects.featureEvents)
        implementation(projects.featureMatches)
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

    val androidHostTest by getting {
      dependencies {
        implementation(libs.coroutine.test)
      }
    }

    val iosMain by getting

    val iosTest by getting {
      dependencies {
        implementation(libs.multiplatform.settings.test)
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
  }
}
