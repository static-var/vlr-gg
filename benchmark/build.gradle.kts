@file:Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")

import com.android.build.api.dsl.ManagedVirtualDevice
import java.io.FileInputStream
import java.util.Properties

plugins {
  id("com.android.test")
  id("org.jetbrains.kotlin.android")
  id("vlr.spotless")
}

android {
  namespace = "com.example.benchmark"
  compileSdk = 33

  defaultConfig {
    minSdk = 23 // Macrobenchmark doesn't work with SDK lower than 23
    targetSdk = 33

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions { jvmTarget = JavaVersion.VERSION_11.toString() }
  // [END_EXCLUDE]
  // Note that your module name may have different name
  targetProjectPath = ":app"
  // Enable the benchmark to run separately from the app process
  experimentalProperties["android.experimental.self-instrumenting"] = true

  testOptions {
    managedDevices {
      devices {
        create("pixel6Api30", ManagedVirtualDevice::class.java) {
          device = "Pixel 6"
          apiLevel = 30
          systemImageSource = "aosp"
        }
      }
    }
  }

  buildTypes {
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
      localProperties.load(FileInputStream(localPropertiesFile))
    }
    // declare a build type to match the target app's build type
    create("benchmark") {
      buildConfigField(
        "String",
        "TOKEN",
        System.getenv("API_TOKEN") ?: localProperties["token"] as String
      )
      isDebuggable = true
      signingConfig = signingConfigs.getByName("debug")
      // [START_EXCLUDE silent]
      // Selects release buildType if the benchmark buildType not available in other modules.
      matchingFallbacks += mutableListOf("release")
      // [END_EXCLUDE]
    }
  }
}

dependencies { implementation(libs.bundles.benchmark) }

androidComponents { beforeVariants(selector().all()) { it.enable = it.buildType == "benchmark" } }
