plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  id("vlr.detekt")
  id("vlr.ktfmt")
}

android {
  namespace = "dev.staticvar.designsystem"
  compileSdk = 34

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
  kotlinOptions { jvmTarget = JavaVersion.VERSION_17.toString() }
  buildFeatures {
    compose = true
  }
  composeOptions { kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get() }
}

dependencies {
  api(platform(libs.compose.bom.alpha))
  implementation(libs.bundles.base)
  implementation(libs.bundles.compose)
  implementation(libs.bundles.m3)
  implementation(libs.compose.icons)

  testImplementation(libs.junit)
  androidTestImplementation(libs.android.junit)
  androidTestImplementation(libs.espresso.core)
}
