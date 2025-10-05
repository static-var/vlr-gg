plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

kotlin {
  applyDefaultHierarchyTemplate()

  androidLibrary {
    namespace = "dev.staticvar.vlr.shared"
    compileSdk = 35
    minSdk = 24
  }

  val fastIos = project.findProperty("fastIos") == "true"
  val iosTargets = if (fastIos) {
    listOf(iosSimulatorArm64())
  } else {
    listOf(iosX64(), iosArm64(), iosSimulatorArm64())
  }
  iosTargets.forEach { target ->
    target.binaries.framework {
      baseName = "shared"
      isStatic = true
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
        implementation(compose.ui)
        implementation(compose.components.resources)
        implementation(compose.components.uiToolingPreview)
        implementation(libs.coroutines.core)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation(compose.preview)
        implementation(libs.activity.compose)
      }
    }

    val iosMain by getting
  }
}
