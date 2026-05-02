/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins { `kotlin-dsl` }

afterEvaluate {
  tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
  }

  tasks.withType<KotlinCompile>().configureEach {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
  }
}

dependencies {
  compileOnly(libs.build.agp)
  compileOnly(libs.build.kover)
  compileOnly(libs.build.kotlin)
  compileOnly(libs.detekt.gradle)
  compileOnly(libs.spotless.gradle)
}

gradlePlugin {
  plugins {
    register("detekt") {
      id = "vlr.detekt"
      implementationClass = "DetektConventionPlugin"
    }
    register("ktlint") {
      id = "vlr.ktlint"
      implementationClass = "KtlintConventionPlugin"
    }
  }
}
