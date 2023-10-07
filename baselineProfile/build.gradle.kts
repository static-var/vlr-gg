import com.android.build.api.dsl.ManagedVirtualDevice

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
  alias(libs.plugins.androidTest)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.baselineprofile)
}

android {
  namespace = "dev.staticvar.baselineprofile"
  compileSdk = 34

  defaultConfig {
    minSdk = 28
    targetSdk = 34

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_17.toString()
  }

  targetProjectPath = ":app"

  testOptions.managedDevices.devices {
    create<ManagedVirtualDevice>("pixel6Api33") {
      device = "Pixel 6"
      apiLevel = 33
      systemImageSource = "aosp"
    }
  }
}

// This is the configuration block for the Baseline Profile plugin.
// You can specify to run the generators on a managed devices or connected devices.
baselineProfile {
  managedDevices += "pixel6Api33"
  useConnectedDevices = false
  enableEmulatorDisplay = true
}

dependencies {
  implementation(libs.android.junit)
  implementation(libs.espresso.core)
  implementation(libs.uiautomator)
  implementation(libs.benchmark.macro)
}