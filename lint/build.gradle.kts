/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
plugins {
  kotlin("jvm")
}

kotlin { jvmToolchain(17) }

dependencies {
  compileOnly(libs.jvm.lint.api)
  compileOnly(libs.jvm.lint.checks)
  testImplementation(kotlin("test"))
  testImplementation(libs.jvm.lint.tests)
}
