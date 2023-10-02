import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

class DetektConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
      pluginManager.apply(libs.findPlugin("detekt").get().get().pluginId)

      tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = JavaVersion.VERSION_17.toString()
      }
      tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        jvmTarget = JavaVersion.VERSION_17.toString()
      }

      extensions.getByType<DetektExtension>().apply {
        buildUponDefaultConfig = true // preconfigure defaults
        allRules = false // activate all available (even unstable) rules.
        autoCorrect = true
        parallel = true
        config = files("config/detekt/detekt.yml")
        baseline = file("config/detekt/detekt-baseline.xml")
        autoCorrect = true
      }

      tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        reports {
          // observe findings in your browser with structure and code snippets
          html.required.set(true)
          // similar to the console output, contains issue signature to manually edit baseline files
          txt.required.set(true)
          // simple Markdown format
          md.required.set(true)
        }
      }

      dependencies.apply {
        add("detektPlugins",  libs.findLibrary("detekt-compose").get())
      }
    }
  }
}