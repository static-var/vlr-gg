@file:Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.Properties

 plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.kapt)
  id("dagger.hilt.android.plugin")
  id("kotlin-parcelize")
  id("kotlinx-serialization")
  id("com.google.gms.google-services")
  id("com.google.firebase.firebase-perf")
  id("com.google.firebase.crashlytics")
  id("vlr.spotless")
  id("io.gitlab.arturbosch.detekt")
  alias(libs.plugins.ksp.plugin)
}

android {
  compileSdk = 33
  namespace = "dev.staticvar.vlr"

  defaultConfig {
    applicationId = "dev.staticvar.vlr"
    minSdk = 23
    targetSdk = 33
    versionCode = 47
    versionName = "v0.3.3"

    setProperty("archivesBaseName", "${applicationId}-${versionCode}(${versionName})")

    ksp {
      arg("room.schemaLocation", "$projectDir/schemas")
    }
  }

  signingConfigs {
    create("release") {
      storeFile = file("keystore/vlr-gg.jks")
      storePassword =
        System.getenv("SIGNING_STORE_PASSWORD")
          ?: gradleLocalProperties(rootDir).getProperty("store.password") as String
      keyPassword =
        System.getenv("SIGNING_KEY_PASSWORD")
          ?: gradleLocalProperties(rootDir).getProperty("key.password") as String
      keyAlias =
        System.getenv("SIGNING_KEY_ALIAS")
          ?: gradleLocalProperties(rootDir).getProperty("key.alias") as String
    }
  }

  buildTypes {
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
      localProperties.load(FileInputStream(localPropertiesFile))
    }
    create("benchmark") {
      buildConfigField(
        "String",
        "TOKEN",
        System.getenv("API_TOKEN") ?: localProperties["token"] as String
      )
      isShrinkResources = true
      isMinifyEnabled = true
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += mutableListOf("release")
      isDebuggable = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      configure<CrashlyticsExtension> { mappingFileUploadEnabled = false }
    }
    getByName("debug") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      buildConfigField(
        "String",
        "TOKEN",
        System.getenv("API_TOKEN") ?: localProperties["token"] as String
      )
      configure<CrashlyticsExtension> { mappingFileUploadEnabled = false }
    }
    getByName("release") {
      isShrinkResources = true
      isMinifyEnabled = true
      buildConfigField(
        "String",
        "TOKEN",
        System.getenv("API_TOKEN") ?: localProperties["token"] as String
      )
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      configure<CrashlyticsExtension> { mappingFileUploadEnabled = true }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_17.toString()
    freeCompilerArgs = freeCompilerArgs + listOf(
        "-opt-in=kotlin.RequiresOptIn",
        "-opt-in=kotlin.contracts.ExperimentalContracts",
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        "-opt-in=kotlin.time.ExperimentalTime",
        "-opt-in=androidx.compose.ui.text.ExperimentalTextApi",
        "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
        "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
        "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        "-opt-in=androidx.compose.runtime.InternalComposeApi",
        "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
        "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",
    )
  }
  buildFeatures { compose = true
    buildConfig = true
  }
  composeOptions { kotlinCompilerExtensionVersion = "1.4.4" }
  packagingOptions {
    jniLibs { excludes += listOf("/META-INF/{AL2.0,LGPL2.1}") }
    resources { excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/DEPENDENCIES") }
  }
  bundle { storeArchive { enable = false } }
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.bundles.base)
  implementation(libs.bundles.compose)
  implementation(libs.compose.icons)

  implementation(libs.navigation)
  implementation(libs.bundles.lifecycle)

  implementation(libs.glance.widget)
  implementation(libs.splashscreen)
  implementation(libs.profileinstaller)

  implementation(libs.work.manager)

  implementation(libs.kotlin.monad)

  implementation(libs.bundles.accompanist)

  implementation(libs.browser)
  implementation(libs.webkit)

  implementation(libs.immutable.collection)

  // Coroutines
  implementation(libs.bundles.coroutines)

  // Hilt
  implementation(libs.bundles.hilt)
  kapt(libs.hilt.compiler)
  kapt(libs.hilt.android.compiler)

  // Room
  implementation(libs.bundles.room)
  ksp(libs.room.compiler)

  implementation(libs.kotlinx.serialization)
  implementation(libs.kotlinx.collections.immutable)

  // Firebase
  implementation(platform("com.google.firebase:firebase-bom:31.3.0"))
  implementation("com.google.firebase:firebase-analytics-ktx")
  implementation("com.google.firebase:firebase-messaging-ktx")
  implementation("com.google.firebase:firebase-crashlytics-ktx")
  implementation("com.google.firebase:firebase-perf-ktx")

  implementation(libs.bundles.ktor)
  implementation(libs.okhttp.brotli)
  implementation(libs.logging.interceptor)

  implementation(libs.jsoup)
  implementation(libs.landscapist.glide)
  implementation(libs.landscapist.animation)

//  coreLibraryDesugaring(libs.desugar.jdk.libs)

  testImplementation(libs.bundles.testing)

  detektPlugins(libs.detekt.formatting)
  detektPlugins(libs.detekt)
}
