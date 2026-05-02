/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.FormatterFunc
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import java.io.Serializable

class KtlintConventionPlugin : Plugin<Project> {

  companion object {
    private const val KTLINT_VERSION = "1.8.0"
    private const val VLR_KTLINT_RULES = "dev.staticvar:ktlint-rules:1.0.0"
    private const val ONE_ANNOTATION_PER_LINE_STEP = "oneAnnotationPerLine"
    private const val KOTLIN_LICENSE_DELIMITER = "(@file|package|import)"
    private const val KOTLIN_SCRIPT_LICENSE_DELIMITER =
      "(@file|pluginManagement|plugins|enableFeaturePreview|dependencyResolutionManagement|rootProject|includeBuild|include|import)"
  }

  override fun apply(project: Project) {
    with(project) {
      val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

      pluginManager.apply(libs.findPlugin("spotless-plugin").get().get().pluginId)

      extensions.getByType<SpotlessExtension>().apply {
        kotlinGradle {
          val runsKtlint = shouldRunKtlint()
          if (this@with == rootProject) {
            target("**/*.gradle.kts")
            targetExclude("**/build/**")
          } else {
            target("*.gradle.kts")
          }
          licenseHeaderFile(rootProject.file("license-header.txt"), KOTLIN_SCRIPT_LICENSE_DELIMITER)
          if (runsKtlint) {
            ktlint(KTLINT_VERSION)
              .customRuleSets(listOf(VLR_KTLINT_RULES))
              .editorConfigOverride(KTLINT_EDITOR_CONFIG_OVERRIDE)
            custom(ONE_ANNOTATION_PER_LINE_STEP, OneAnnotationPerLineFormatter)
            bumpThisNumberIfACustomStepChanges(1)
          }
        }
      }

      fun configureKotlinTarget() {
        extensions.getByType<SpotlessExtension>().apply {
          kotlin {
            val runsKtlint = shouldRunKtlint()
            if (this@with == rootProject) {
              target("**/*.kt")
              targetExclude("**/build/**")
            } else {
              target("src/**/*.kt")
            }
            licenseHeaderFile(rootProject.file("license-header.txt"), KOTLIN_LICENSE_DELIMITER)
            if (runsKtlint) {
              ktlint(KTLINT_VERSION)
                .customRuleSets(listOf(VLR_KTLINT_RULES))
                .editorConfigOverride(KTLINT_EDITOR_CONFIG_OVERRIDE)
              custom(ONE_ANNOTATION_PER_LINE_STEP, OneAnnotationPerLineFormatter)
              bumpThisNumberIfACustomStepChanges(1)
            }
          }
        }
      }

      if (this == rootProject) configureKotlinTarget()

      pluginManager.withPlugin("org.jetbrains.kotlin.jvm") { configureKotlinTarget() }
      pluginManager.withPlugin("org.jetbrains.kotlin.android") { configureKotlinTarget() }
      pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") { configureKotlinTarget() }

      tasks.matching { it.name.startsWith("spotless") }.configureEach {
        dependsOn(gradle.includedBuild("build-logic").task(":ktlint-rules:jar"))
      }
    }
  }

  private fun Project.shouldRunKtlint(): Boolean =
    this != rootProject && name != "app"
}

private object OneAnnotationPerLineFormatter : FormatterFunc, Serializable {
  override fun apply(input: String): String = input.formatOneAnnotationPerLine()
}

private val KTLINT_EDITOR_CONFIG_OVERRIDE =
  mapOf(
    "indent_size" to 2,
    "continuation_indent_size" to 4,
    "max_line_length" to 120,
    "ktlint_code_style" to "intellij_idea",
    "ktlint_standard_filename" to "disabled",
    "ktlint_standard_function-naming" to "disabled",
    "ktlint_standard_property-naming" to "disabled",
  )

private fun String.formatOneAnnotationPerLine(): String = split('\n')
  .joinToString(separator = "\n") { line ->
    line.formatAnnotationRun()
  }

private fun String.formatAnnotationRun(): String {
  val indent = takeWhile(Char::isWhitespace)
  var cursor = indent.length
  val annotations = mutableListOf<String>()

  while (true) {
    val match = annotationRegex.find(this, cursor) ?: break
    if (match.range.first != cursor) break

    annotations += match.value.trimEnd()
    cursor = match.range.last + 1
    cursor += substring(cursor).takeWhile(Char::isWhitespace).length
  }

  if (annotations.size < 2) return this

  val remainder = substring(cursor)
  return buildString {
    append(annotations.joinToString(separator = "\n$indent", prefix = indent))
    if (remainder.isNotBlank()) {
      append('\n')
      append(indent)
      append(remainder.trimStart())
    }
  }
}

private val annotationRegex = Regex("""@[A-Za-z_][A-Za-z0-9_:.]*(?:\([^)\n]*\))?\s*""")
