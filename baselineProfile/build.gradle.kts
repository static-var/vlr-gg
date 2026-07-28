import com.android.build.api.dsl.TestExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.androidTest)
  alias(libs.plugins.baselineprofile)
}

extensions.configure<TestExtension> {
  namespace = "dev.staticvar.baselineprofile"
  compileSdk = 36

  defaultConfig {
    minSdk = 28
    targetSdk = 36

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  targetProjectPath = ":app"

  testOptions.managedDevices.localDevices {
    create("pixel6Api33") {
      device = "Pixel 6"
      apiLevel = 33
      systemImageSource = "aosp"
    }
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
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
