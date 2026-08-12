@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.ApplicationExtension
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val vlrApplicationId = "dev.staticvar.vlr"
val vlrVersionCode = 69
val vlrVersionName = "v0.6.3"

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.parcelize)
  alias(libs.plugins.hilt.plugin)
  alias(libs.plugins.ksp.plugin)
  alias(libs.plugins.secrets.plugin)
  alias(libs.plugins.baselineprofile)
  alias(libs.plugins.firebase.perf)
  alias(libs.plugins.gms.plugin)
  alias(libs.plugins.firebase.crashlytics)
  alias(libs.plugins.room)
  id("vlr.detekt")
  id("vlr.ktfmt")
}

extensions.configure<ApplicationExtension> {
  compileSdk = 37
  namespace = "dev.staticvar.vlr"

  defaultConfig {
    applicationId = vlrApplicationId
    minSdk = 24
    targetSdk = 36
    versionCode = vlrVersionCode
    versionName = vlrVersionName

    room { schemaDirectory("$projectDir/schemas/") }
  }

  signingConfigs {
    create("release") {
      val prop =
        Properties().apply { load(FileInputStream(File(rootProject.rootDir, "local.properties"))) }
      storeFile = file("keystore/vlr-gg.jks")
      storePassword =
        System.getenv("SIGNING_STORE_PASSWORD") ?: prop.getProperty("store.password") as String
      keyPassword =
        System.getenv("SIGNING_KEY_PASSWORD") ?: prop.getProperty("key.password") as String
      keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: prop.getProperty("key.alias") as String
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
      configure<CrashlyticsExtension> { mappingFileUploadEnabled = false }
      isDebuggable = false
      manifestPlaceholders["appName"] = "VLR Benchmark"
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    getByName("debug") {
      isMinifyEnabled = false
      applicationIdSuffix = ".debug"
      configure<CrashlyticsExtension> { mappingFileUploadEnabled = false }
      manifestPlaceholders["appName"] = "VLR Debug"
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    getByName("release") {
      isShrinkResources = true
      isMinifyEnabled = true
      configure<CrashlyticsExtension> { mappingFileUploadEnabled = true }
      manifestPlaceholders["appName"] = "VLR.gg (Unofficial)"
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  composeCompiler { reportsDestination = layout.buildDirectory.dir("compose_compiler") }
  packaging {
    jniLibs { excludes += listOf("/META-INF/{AL2.0,LGPL2.1}") }
    resources { excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/DEPENDENCIES") }
  }
  bundle { storeArchive { enable = false } }
  baselineProfile {
    saveInSrc = true
    mergeIntoMain = true
    dexLayoutOptimization = true
  }
  experimentalProperties["android.experimental.art-profile-r8-rewriting"] = true
}

base { archivesName.set("$vlrApplicationId-$vlrVersionCode($vlrVersionName)") }

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
    optIn.addAll(
      "kotlin.RequiresOptIn",
      "kotlin.contracts.ExperimentalContracts",
      "kotlinx.coroutines.ExperimentalCoroutinesApi",
      "kotlinx.serialization.ExperimentalSerializationApi",
      "kotlin.time.ExperimentalTime",
      "androidx.compose.ui.text.ExperimentalTextApi",
      "androidx.compose.ui.ExperimentalComposeUiApi",
      "androidx.compose.animation.ExperimentalAnimationApi",
      "androidx.compose.foundation.ExperimentalFoundationApi",
      "androidx.compose.material3.ExperimentalMaterial3Api",
      "androidx.compose.runtime.InternalComposeApi",
      "androidx.compose.material.ExperimentalMaterialApi",
      "com.google.accompanist.permissions.ExperimentalPermissionsApi",
    )
  }
}

dependencies {
  baselineProfile(project(":baselineprofile"))

  api(platform(libs.compose.bom))
  implementation(libs.bundles.base)
  implementation(libs.bundles.compose)
  implementation(libs.bundles.m3)
  implementation(libs.compose.icons)

  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.installations)
  implementation(libs.firebase.perf)
  implementation(libs.firebase.messaging)
  implementation(libs.firebase.crashlytics)
  implementation(libs.firebase.analytics)

  implementation(libs.bundles.lifecycle)

  implementation(libs.glance.widget)
  implementation(libs.glance.widget.m3)
  implementation(libs.splashscreen)
  implementation(libs.profileinstaller)

  implementation(libs.work.manager)

  implementation(libs.kotlin.monad)

  implementation(libs.bundles.accompanist)

  implementation(libs.browser)
  implementation(libs.webkit)

  implementation(libs.androidx.collection)

  implementation(libs.material.kolor)
  implementation(libs.coil)

  // Coroutines
  implementation(libs.bundles.coroutines)

  // Hilt
  implementation(libs.bundles.hilt)
  ksp(libs.hilt.compiler)
  ksp(libs.hilt.android.compiler)

  // Room
  implementation(libs.bundles.room)
  ksp(libs.room.compiler)

  implementation(libs.kotlinx.serialization)

  implementation(libs.bundles.ktor)

  implementation(libs.jsoup)
  implementation(libs.landscapist.glide)
  implementation(libs.landscapist.animation)
  implementation(libs.haze)
  implementation(libs.haze.materials)

  coreLibraryDesugaring(libs.core.desugar)

  testImplementation(libs.bundles.testing)
  testImplementation(libs.work.testing)
}
