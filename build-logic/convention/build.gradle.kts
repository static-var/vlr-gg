import org.gradle.api.JavaVersion
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins { `kotlin-dsl` }

afterEvaluate {
  tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
  }

  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions { jvmTarget = JavaVersion.VERSION_11.toString() }
  }
}

dependencies {
  compileOnly(libs.build.agp)
  compileOnly(libs.build.kover)
  compileOnly(libs.build.kotlin)
}

gradlePlugin {
  plugins {
//    register("spotless") {
//      id = "vlr.spotless"
//      implementationClass = "SpotlessConventionPlugin"
//    }
  }
}