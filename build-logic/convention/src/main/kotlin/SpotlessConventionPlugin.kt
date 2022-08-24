import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class SpotlessConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      pluginManager.apply("com.diffplug.spotless")

      extensions.configure<SpotlessExtension> {
        kotlin {
          ktfmt().googleStyle()
          target("**/*.kt")
          targetExclude("**/build/**/*.kt")
        }
        kotlinGradle {
          ktfmt().googleStyle()
          target("**/*.kts")
          targetExclude("**/build/**/*.kt")
        }
        format("xml") {
          target("**/*.xml")
          targetExclude("**/build/", ".idea/")
          trimTrailingWhitespace()
          indentWithSpaces()
          endWithNewline()
        }
      }
    }
  }
}
