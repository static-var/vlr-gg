import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
  id("vlr.detekt")
  id("vlr.ktfmt")
}

extensions.configure<LibraryExtension> {
  namespace = "dev.staticvar.designsystem"
  compileSdk = 37

  defaultConfig {
    minSdk = 24

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures { compose = true }
  composeCompiler { reportsDestination = layout.buildDirectory.dir("compose_compiler") }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
  api(platform(libs.compose.bom))
  implementation(libs.bundles.base)
  implementation(libs.bundles.compose)
  implementation(libs.bundles.m3)
  implementation(libs.compose.icons)

  testImplementation(libs.junit)
  androidTestImplementation(libs.android.junit)
  androidTestImplementation(libs.espresso.core)
}
