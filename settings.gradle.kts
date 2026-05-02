pluginManagement {
  includeBuild("build-logic")
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven { url = java.net.URI.create("https://dl.bintray.com/kotlin/kotlinx") }
    maven { url = java.net.URI.create("https://androidx.dev/storage/compose-compiler/repository") }
  }
}
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven(url = "https://jitpack.io")
  }
}

rootProject.name = "VLR"

include(":androidApp")

include(":iosApp")

include(":shared")

include(":domain")

include(":data")

include(":remote-source")

include(":local-source")

include(":core")

include(":designsystem")

include(":lint")

include(":feature-news")

include(":feature-matches")

include(":feature-events")

include(":feature-rankings")

include(":feature-team")

include(":feature-player")

include(":feature-about")
