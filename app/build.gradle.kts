@file:Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")


import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.parcelize)
  id("dagger.hilt.android.plugin")
  alias(libs.plugins.ksp.plugin)
  alias(libs.plugins.secrets.plugin)
  alias(libs.plugins.baselineprofile)
  alias(libs.plugins.firebase.perf)
  alias(libs.plugins.firebase.crashlytics)
  alias(libs.plugins.room)
  alias(libs.plugins.gms.plugin)
  id("vlr.detekt")
  id("vlr.ktfmt")
}

android {
  compileSdk = 35
  namespace = "dev.staticvar.vlr"

  defaultConfig {
    applicationId = "dev.staticvar.vlr"
    minSdk = 23
    targetSdk = 35
    versionCode = 66
    versionName = "v0.6.0"

    setProperty("archivesBaseName", "${applicationId}-${versionCode}(${versionName})")

    room {
      schemaDirectory("$projectDir/schemas/")
    }
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
      manifestPlaceholders["appName"] = "VLR Benchmark"
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      configure<CrashlyticsExtension> { mappingFileUploadEnabled = false }
    }
    getByName("debug") {
      isMinifyEnabled = false
      applicationIdSuffix = ".debug"
      manifestPlaceholders["appName"] = "VLR Debug"
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      configure<CrashlyticsExtension> { mappingFileUploadEnabled = false }
    }
    getByName("release") {
      isShrinkResources = true
      isMinifyEnabled = true
      manifestPlaceholders["appName"] = "VLR.gg (Unofficial)"
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
  composeCompiler {
    featureFlags.set(listOf(ComposeFeatureFlag.StrongSkipping))

    reportsDestination = layout.buildDirectory.dir("compose_compiler")
  }
  packaging {
    jniLibs { excludes += listOf("/META-INF/{AL2.0,LGPL2.1}") }
    resources { excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/DEPENDENCIES") }
  }
  bundle { storeArchive { enable = false } }
  baselineProfile {
    saveInSrc = true
    mergeIntoMain = true
    dexLayoutOptimization = true
    from(projects.baselineprofile.dependencyProject)
  }
  experimentalProperties["android.experimental.art-profile-r8-rewriting"] = true
}

dependencies {
  api(platform(libs.compose.bom.alpha))
  implementation(libs.bundles.base)
  implementation(libs.bundles.compose)
  implementation(libs.bundles.m3)
  implementation(libs.compose.icons)

  implementation(platform(libs.firebase.bom))
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

  implementation(libs.immutable.collection)
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
  implementation(libs.logging.interceptor)

  implementation(libs.jsoup)
  implementation(libs.landscapist.glide)
  implementation(libs.landscapist.animation)
  implementation(libs.haze)
  implementation(libs.haze.materials)

  coreLibraryDesugaring(libs.core.desugar)

  testImplementation(libs.bundles.testing)
}
