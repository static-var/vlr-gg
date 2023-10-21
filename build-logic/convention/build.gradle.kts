
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins { `kotlin-dsl` }

afterEvaluate {
  tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
  }

  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions { jvmTarget = JavaVersion.VERSION_17.toString() }
  }
}

dependencies {
  compileOnly(libs.build.agp)
  compileOnly(libs.build.kover)
  compileOnly(libs.build.kotlin)
  compileOnly(libs.detekt.gradle)
  compileOnly(libs.spotless.gradle)
  compileOnly(libs.sentry.gradle)
}

gradlePlugin {
  plugins {
    register("detekt") {
      id = "vlr.detekt"
      implementationClass = "DetektConventionPlugin"
    }
    register("ktfmt") {
      id = "vlr.ktfmt"
      implementationClass = "KtfmtConventionPlugin"
    }
    register("sentry") {
      id = "vlr.sentry"
      implementationClass = "SentryConventionPlugin"
    }
  }
}