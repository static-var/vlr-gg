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
