plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.ksp.plugin)
}

kotlin {
  applyDefaultHierarchyTemplate()

  androidLibrary {
    namespace = "dev.staticvar.vlr.data"
    compileSdk = 35
    minSdk = 24

    withHostTestBuilder {}

    withDeviceTestBuilder {
      sourceSetTreeName = "test"
    }.configure {
      instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.kotlin.stdlib)
        implementation(libs.coroutines.core)
        implementation(libs.koin.core)
        implementation(libs.kotlinx.datetime)
        // Domain layer
        implementation(projects.domain)

        // Data sources
        implementation(projects.remoteSource)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation(projects.localSource)
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

  }
}

ksp {
  arg("konvert.non-constructor-properties-mapping", "explicit")
  arg("konvert.invalid-mapping-strategy", "fail")
}

dependencies {
  add("kspAndroid", libs.konvert.processor)
}

// iOS test disabling - not needed when iOS targets are commented out
// listOf("iosX64", "iosArm64", "iosSimulatorArm64").forEach { targetPrefix ->
//   tasks.matching { it.name.startsWith(targetPrefix) && it.name.endsWith("Test") }.configureEach {
//     enabled = false
//   }
// }
