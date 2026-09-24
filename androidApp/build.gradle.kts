/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
import io.sentry.android.gradle.extensions.InstrumentationFeature
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.sentry.android)
  alias(libs.plugins.baselineprofile)
  alias(libs.plugins.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
  FileInputStream(localPropertiesFile).use(localProperties::load)
}

val envProperties = Properties()
val envFile = rootProject.file(".env")
if (envFile.exists()) {
  FileInputStream(envFile).use(envProperties::load)
}

val authToken =
  (
    System.getenv("VLR_AUTH_TOKEN")
      ?: localProperties.getProperty("TOKEN")
      ?: localProperties.getProperty("VLR_AUTH_TOKEN")
      ?: envProperties.getProperty("VLR_AUTH_TOKEN")
      ?: ""
    )
    .trim()
    .removeSurrounding("\"")
    .removeSurrounding("'")
val escapedAuthToken = authToken.replace("\\", "\\\\").replace("\"", "\\\"")

fun sentrySetting(name: String, fallback: String = ""): String =
  sequenceOf(System.getenv(name), localProperties.getProperty(name), envProperties.getProperty(name), fallback)
    .filterNotNull().map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
    .firstOrNull { it.isNotBlank() }.orEmpty()

fun String.buildConfigLiteral(): String = "\"" + replace("\\", "\\\\")
  .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

val sentryUploadEnabled = providers.gradleProperty("sentryUpload").map(String::toBoolean).getOrElse(false)

sentry {
  org.set(sentrySetting("SENTRY_ORG"))
  projectName.set(sentrySetting("SENTRY_PROJECT", "vlr-mobile"))
  // The CLI reads SENTRY_AUTH_TOKEN or ignored sentry.properties at upload time.
  autoUploadProguardMapping.set(sentryUploadEnabled)
  uploadNativeSymbols.set(sentryUploadEnabled)
  autoUploadNativeSymbols.set(sentryUploadEnabled)
  includeNativeSources.set(false)
  includeSourceContext.set(false)
  autoInstallation.enabled.set(false)
  tracingInstrumentation.enabled.set(true)
  tracingInstrumentation.features.set(setOf(InstrumentationFeature.FILE_IO))
  ignoredBuildTypes.set(setOf("debug"))
}

val appVersion = Properties().apply {
  val contents = providers.fileContents(rootProject.layout.projectDirectory.file("version.xcconfig")).asText.get()
  load(contents.lineSequence().map { it.substringBefore("//").trim() }.joinToString("\n").reader())
}
val releaseVersionName = requireNotNull(appVersion.getProperty("MARKETING_VERSION"))
val releaseVersionCode = requireNotNull(appVersion.getProperty("CURRENT_PROJECT_VERSION")?.toIntOrNull())
require(Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)").matches(releaseVersionName)) {
  "MARKETING_VERSION in version.xcconfig must use major.minor.patch"
}
require(releaseVersionCode in 1..2_100_000_000) {
  "CURRENT_PROJECT_VERSION in version.xcconfig must be between 1 and 2100000000"
}
require(!providers.gradleProperty("releaseVersionName").isPresent && !providers.gradleProperty("releaseVersionCode").isPresent) {
  "Update version.xcconfig instead of overriding the Android version"
}
val releaseSigningStore = providers.environmentVariable("VLR_SIGNING_STORE_FILE").orNull

android {
  sourceSets.getByName("androidTest").assets.srcDir(rootProject.file("app/schemas"))
  namespace = "dev.staticvar.vlr.android"
  compileSdk = 37

  defaultConfig {
    applicationId = "dev.staticvar.vlr"
    minSdk = 24
    targetSdk = 36
    versionCode = releaseVersionCode
    versionName = releaseVersionName
    buildConfigField("String", "TOKEN", "\"$escapedAuthToken\"")
    buildConfigField("String", "SENTRY_DSN", sentrySetting("SENTRY_DSN_ANDROID", sentrySetting("SENTRY_DSN")).buildConfigLiteral())
    buildConfigField("String", "SENTRY_ENVIRONMENT", sentrySetting("SENTRY_ENVIRONMENT").buildConfigLiteral())
    buildConfigField("boolean", "SENTRY_ENABLED", sentrySetting("SENTRY_ENABLED", "true").toBoolean().toString())


    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    if (releaseSigningStore != null) {
      create("release") {
        storeFile = file(releaseSigningStore)
        keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").get()
        keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").get()
        storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").get()
      }
    }
  }

  buildTypes {
    debug {
      applicationIdSuffix = ".debug"
    }
    release {
      if (releaseSigningStore != null) {
        signingConfig = signingConfigs.getByName("release")
      }
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

baselineProfile {
  saveInSrc = true
  automaticGenerationDuringBuild = false
}

dependencies {
  androidTestImplementation(libs.multiplatform.settings)
  androidTestImplementation(libs.multiplatform.settings.test)
  androidTestImplementation(libs.android.junit)
  androidTestImplementation(libs.android.test.runner)
  androidTestImplementation(projects.localSource)
  androidTestImplementation(libs.sqldelight.android.driver)
  baselineProfile(projects.baselineProfile)
  implementation(libs.profileinstaller)
  // Compose Multiplatform
  implementation(compose.runtime)
  implementation(compose.foundation)
  implementation(compose.material3)
  implementation(compose.ui)
  implementation(compose.components.resources)
  implementation(libs.compose.ui.tooling.preview)

  // Preview support
  debugImplementation(libs.compose.ui.tooling.cmp)
  debugImplementation(libs.leakcanary.android)

  // Activity Compose
  implementation(libs.activity.compose)
  implementation(libs.glance.appwidget)
  implementation(libs.work.runtime)
  implementation(libs.splashscreen)
  implementation(libs.koin.android)
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.messaging)
  implementation(libs.coroutines.core)
  implementation(libs.kotlinx.serialization)

  // Project modules
  implementation(projects.shared)
  implementation(projects.domain)
  implementation(projects.core)
  implementation(projects.sharedUi)
  implementation(projects.designsystem)
  lintChecks(project(":lint"))
}
