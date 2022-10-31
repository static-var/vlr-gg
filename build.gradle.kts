@file:Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.kapt) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.hilt.plugin) apply false
}

buildscript {
  val hilt_version = "2.44"
  val kotlin_version = "1.7.20"
  repositories {
    google()
    mavenCentral()
  }

  dependencies {
    classpath("com.android.tools.build:gradle:7.3.1")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlin_version")
    classpath("com.google.dagger:hilt-android-gradle-plugin:$hilt_version")
    classpath("com.google.gms:google-services:4.3.14")
    classpath("com.google.firebase:perf-plugin:1.4.2")
    classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.2")
  }
}
