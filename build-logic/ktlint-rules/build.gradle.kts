/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
plugins {
  alias(libs.plugins.kotlin.jvm)
}

group = "dev.staticvar"
version = "1.0.0"

kotlin {
  explicitApi()
  jvmToolchain(17)
}

dependencies {
  compileOnly(libs.ktlint.cli.ruleset.core)
  compileOnly(libs.ktlint.rule.engine.core)
}
