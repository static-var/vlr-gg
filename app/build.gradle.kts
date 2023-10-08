@file:Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")

import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.parcelize)
  id("dagger.hilt.android.plugin")
  id("com.google.gms.google-services")
  id("com.google.firebase.firebase-perf")
  id("com.google.firebase.crashlytics")
  alias(libs.plugins.ksp.plugin)
  alias(libs.plugins.secrets.plugin)
  alias(libs.plugins.baselineprofile)
  id("vlr.detekt")
  id("vlr.ktfmt")
}

android {
  compileSdk = 34
  namespace = "dev.staticvar.vlr"

  defaultConfig {
    applicationId = "dev.staticvar.vlr"
    minSdk = 23
    targetSdk = 34
    versionCode = 50
    versionName = "v0.3.6"

    setProperty("archivesBaseName", "${applicationId}-${versionCode}(${versionName})")

    ksp { arg("room.schemaLocation", "$projectDir/schemas") }
  }

  signingConfigs {
    create("release") {
      val prop = Properties().apply {
        load(FileInputStream(File(rootProject.rootDir, "local.properties")))
      }
      storeFile = file("keystore/vlr-gg.jks")
      storePassword =
        System.getenv("SIGNING_STORE_PASSWORD")
          ?: prop.getProperty("store.password") as String
      keyPassword =
        System.getenv("SIGNING_KEY_PASSWORD")
          ?: prop.getProperty("key.password") as String
      keyAlias =
        System.getenv("SIGNING_KEY_ALIAS")
          ?: prop.getProperty("key.alias") as String
    }
  }

  buildTypes {
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
      localProperties.load(FileInputStream(localPropertiesFile))
    }
    create("benchmark") {
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

      configure<CrashlyticsExtension> { mappingFileUploadEnabled = false }
    }
    getByName("release") {
      isShrinkResources = true
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      configure<CrashlyticsExtension> { mappingFileUploadEnabled = true }
    }
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_17.toString()
    freeCompilerArgs =
      freeCompilerArgs +
        listOf(
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
  buildFeatures {
    compose = true
    buildConfig = true
  }
  composeOptions { kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get() }
  packaging {
    jniLibs { excludes += listOf("/META-INF/{AL2.0,LGPL2.1}") }
    resources { excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/DEPENDENCIES") }
  }
  bundle { storeArchive { enable = false } }
  baselineProfile {
    saveInSrc = true
    mergeIntoMain = true
    from(projects.baselineprofile.dependencyProject)
  }
  experimentalProperties["android.experimental.art-profile-r8-rewriting"] = true
  experimentalProperties["android.experimental.r8.dex-startup-optimization"] = true
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.bundles.base)
  implementation(libs.bundles.compose)
  implementation(libs.compose.icons)

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
  "baselineProfile"(project(":baselineprofile"))
  ksp(libs.hilt.compiler)
  ksp(libs.hilt.android.compiler)

  // Room
  implementation(libs.bundles.room)
  ksp(libs.room.compiler)

  implementation(libs.kotlinx.serialization)
  implementation(libs.kotlinx.collections.immutable)

  // Firebase
  implementation(platform(libs.firebase.bom))
  implementation(libs.bundles.firebase)

  implementation(libs.bundles.ktor)
  implementation(libs.okhttp.brotli)
  implementation(libs.logging.interceptor)

  implementation(libs.jsoup)
  implementation(libs.landscapist.glide)
  implementation(libs.landscapist.animation)

  coreLibraryDesugaring(libs.core.desugar)

  testImplementation(libs.bundles.testing)
}
