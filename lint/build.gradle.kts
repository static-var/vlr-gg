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
