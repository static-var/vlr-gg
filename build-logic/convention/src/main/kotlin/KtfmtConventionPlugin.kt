
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

class KtfmtConventionPlugin : Plugin<Project> {

  companion object {
    private const val KTFMT_VERSION = "0.49"
  }

  override fun apply(project: Project) {
    with(project) {
      val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

      pluginManager.apply(libs.findPlugin("spotless-plugin").get().get().pluginId)
      extensions.getByType<SpotlessExtension>().apply {
        kotlin { ktfmt(KTFMT_VERSION).googleStyle() }
        kotlinGradle { ktfmt(KTFMT_VERSION).googleStyle() }
      }

    }
  }
}
