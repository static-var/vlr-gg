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
}

android {
  compileSdk = 33
  namespace = "dev.staticvar.vlr"

  defaultConfig {
    applicationId = "dev.staticvar.vlr"
    minSdk = 23
    targetSdk = 33
    versionCode = 43
    versionName = "v0.2.17"

    setProperty("archivesBaseName", "${applicationId}-${versionCode}(${versionName})")

    javaCompileOptions {
      annotationProcessorOptions {
        argument("room.schemaLocation", "$projectDir/schemas")
      }
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
      optimization {
        keepRules {
          ignoreExternalDependencies("androidx.glance:glance-appwidget")
        }
      }
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
      optimization {
        keepRules {
          ignoreExternalDependencies("androidx.glance:glance-appwidget")
        }
      }
    }
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_1_8.toString()
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
        "-opt-in=androidx.lifecycle.compose.ExperimentalLifecycleComposeApi",
        "-opt-in=com.google.accompanist.pager.ExperimentalPagerApi",
        "-opt-in=androidx.compose.runtime.InternalComposeApi",
        "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
        "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",
    )
  }
  buildFeatures { compose = true
    buildConfig = true
  }
  composeOptions { kotlinCompilerExtensionVersion = "1.3.2" }
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
  kapt(libs.room.compiler)

  implementation(libs.kotlinx.serialization)
  implementation(libs.kotlinx.collections.immutable)

  // Firebase
  implementation(platform("com.google.firebase:firebase-bom:31.1.0"))
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

  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.0")

  testImplementation(libs.bundles.testing)

  detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.22.0")
  detektPlugins("com.twitter.compose.rules:detekt:0.0.22")
}
