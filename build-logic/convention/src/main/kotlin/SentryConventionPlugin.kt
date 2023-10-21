import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.extensions.SentryPluginExtension
import java.io.File
import java.io.FileInputStream
import java.util.EnumSet
import java.util.Properties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

class SentryConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
      pluginManager.apply(libs.findPlugin("sentry-plugin").get().get().pluginId)

      val localProperties = Properties().apply {
        load(FileInputStream(File(rootProject.rootDir, "local.properties")))
      }

      val sentryProperties = Properties().apply {
        load(FileInputStream(File(rootProject.rootDir, "sentry.properties")))
      }

      project.pluginManager.withPlugin("com.android.application") {
        project.extensions.configure<ApplicationAndroidComponentsExtension> {
          onVariants(selector().all()) { variant ->
            val sentryDsn = localProperties.getProperty(SENTRY_DSN_PROPERTY) as String
            variant.manifestPlaceholders.put("sentryDsn", sentryDsn)
            variant.manifestPlaceholders.put("enableSentry", "${variant.name == "release"}")
          }
        }
      }

      pluginManager.apply(libs.findPlugin("sentry-plugin").get().get().pluginId)
      project.plugins.apply(io.sentry.android.gradle.SentryPlugin::class)
      extensions.getByType<SentryPluginExtension>().apply {
        val enableMappings =
          project.providers.gradleProperty(SENTRY_UPLOAD_MAPPINGS_PROPERTY).isPresent
        includeProguardMapping.set(enableMappings)
        autoUploadProguardMapping.set(enableMappings)
        if (enableMappings) {
          authToken.set(
            System.getenv(SENTRY_AUTH_TOKEN) ?: sentryProperties.getProperty("auth.token") as String
          )
        }
        uploadNativeSymbols.set(false)
        autoUploadNativeSymbols.set(false)
        includeNativeSources.set(false)
        ignoredVariants.set(emptySet())
        ignoredBuildTypes.set(setOf("benchmark", "debug"))
        ignoredFlavors.set(emptySet())
        tracingInstrumentation {
          enabled.set(true)
          debug.set(false)
          forceInstrumentDependencies.set(false)
          features.set(EnumSet.allOf(InstrumentationFeature::class.java))
        }
        experimentalGuardsquareSupport.set(false)
        autoInstallation {
          enabled.set(true)
          sentryVersion.set(libs.findVersion("sentry-sdk").get().requiredVersion)
        }
        includeDependenciesReport.set(true)
      }
      with(project.dependencies) {
        addProvider("implementation", platform(libs.findLibrary("sentry-bom").get()))
      }
    }
  }

  private companion object {
    private const val SENTRY_DSN_PROPERTY = "SENTRY_DSN"
    private const val SENTRY_UPLOAD_MAPPINGS_PROPERTY = "sentryUploadMappings"
    private const val SENTRY_AUTH_TOKEN = "SENTRY_AUTH_TOKEN"
  }
}