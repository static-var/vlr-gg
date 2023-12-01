import java.net.URI

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url = java.net.URI.create("https://dl.bintray.com/kotlin/kotlinx") }
        maven { url = java.net.URI.create("https://androidx.dev/storage/compose-compiler/repository/") }
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven (url = "https://jitpack.io")
    }
}
rootProject.name = "VLR"
include (":app")
include (":benchmark")
include(":baselineprofile")
